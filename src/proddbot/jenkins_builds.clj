(ns proddbot.jenkins-builds
  (:require [cheshire.core :as json]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.walk :refer [keywordize-keys]]
            [immutant.web :as web]
            [ring.middleware.params :refer [wrap-params]]
            [taoensso.timbre :as log]
            [proddbot.releases :as releases]
            [proddbot.colors :as c]))

(defn extract-version [req]
  (when-let [key-name (get-in req [:params "version"])]
    (get-in req [::payload :build :parameters (keyword key-name)])))

(defn extract-channel [req]
  (str "#" (get-in req [:params "channel"])))

(defn git-url [build]
  (when-let [url (get-in build [:scm :url])]
    (if (.endsWith url ".git")
      (subs url 0 (- (count url) 4))
      url)))

;; "origin/pr/191/merge"
(defn pr-id [build]
  ;; some jenkins versions give it to us this way?
  (if-let [num (get-in build [:parameters :ghprbPullId])]
    num
    (when-let [branch (get-in build [:scm :branch])]
      (let [[_ type num] (str/split branch #"/")]
        (when (= "pr" type)
          num)))))

(defn pr-url [git-url num]
  (when num
    (format "%s/pull/%s" git-url num)))

(defn format-duration [ms]
  (let [hours   (int (mod (/ ms (* 1000 60 60)) 24))
        minutes (int (mod (/ ms (* 1000 60)) 60))
        seconds (int (mod (/ ms 1000) 60))]
    (format "%02d:%02d:%02d" hours minutes seconds)))

(def status-colors {"ABORTED" :orange
                    "FAILURE" :orange
                    "SUCCESS" :green})

(defn build-message [payload]
  (let [build (:build payload)
        status (:status build)
        msg (format "%s build %s %s with %s"
              (c/with-color :light-blue (:name payload))
              (:number build)
              (.toLowerCase (:phase build))
              (c/with-color (status-colors status :orange) status))
        msg (if-let [duration (::duration payload)]
              (format "%s in %s" msg (c/with-color :orange (format-duration duration)))
              msg)
        msg (if-let [pr-url (pr-url (git-url build) (pr-id build))]
              (format "%s for PR %s" msg (c/with-color :cyan pr-url))
              msg)]
    (format "%s %s" msg (c/with-color :cyan (:full_url build)))))

(defn handler [irc-fn req]
  (when-let [payload (::payload req)]
    (log/info payload)
    (let [build (:build payload)
          msg (build-message payload)]
      (log/info msg)
      (irc-fn (::channel req) msg)
      (when (= "SUCCESS" (:status build))
        (when-let [ga (get-in req [:params "ga"])]
          (let [[group artifact] (str/split ga #":")
                create-watch (get-in req [:params "create_watch"])]
            (when (or (nil? create-watch) (= "true" create-watch))
              (run! (partial irc-fn (::channel req))
                (releases/command
                  #:proddbot.releases{:cmd :add
                                      :args [(format "%s:%s:%s" group artifact (extract-version req))]}
                  {:channel (extract-channel req)}))))))))
  {:status 200})

(defn reject [reason]
  (log/info "reject:" reason)
  {:status 400})

(defn wrap-token [handler valid-token]
  (fn [req]
    (let [token (get-in req [:params "token"])]
      (if (= valid-token token)
        (handler req)
        (reject (str "invalid token: " token))))))

(defn wrap-channel-check [handler channels]
  (fn [req]
    (let [channel (extract-channel req)]
      (if (contains? channels channel)
        (handler (assoc req ::channel channel))
        (reject (str "invalid channel: " channel))))))

(defn wrap-payload-decoding [handler]
  (fn [req]
    (if-let [payload (try
                       (json/parse-stream (io/reader (:body req)) true)
                       (catch Exception e
                         (log/info e)))]
      (handler (assoc req ::payload payload))
      (reject "no payload"))))

(defn wrap-phase-filtering [handler remove-phases]
  (fn [req]
    (let [build (get-in req [::payload :build])
          phase (:phase build)]
      (handler (if (contains? remove-phases phase)
                 (do
                   (log/info (format "ignoring phase %s for %s" phase (:url build)))
                   (dissoc req ::payload))
                 req)))))

(let [start-times (atom {})]
  (defn wrap-duration [handler print-phases]
    (fn [req]
      (let [build (get-in req [::payload :build])
            phase (:phase build)
            url (:url build)]
        (handler (if (= "STARTED" phase)
                   (do
                     (swap! start-times assoc url (System/currentTimeMillis))
                     req)
                   (let [start (@start-times url)]
                     (if (and start (contains? print-phases phase))
                       (do
                         (swap! start-times dissoc url)
                         (assoc-in req [::payload ::duration] (- (System/currentTimeMillis) start)))
                       req))))))))

(defn start [port host valid-token channels irc]
  (web/run
    (-> (partial handler irc)
      (wrap-phase-filtering #{"STARTED" "FINALIZED"})
      (wrap-duration #{"COMPLETED"}) 
      wrap-payload-decoding
      (wrap-channel-check channels)
      (wrap-token valid-token)
      wrap-params)
    :port port
    :host host))

(defn stop []
  (web/stop))
