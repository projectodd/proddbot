(ns proddbot.jenkins-builds
  (:require [cheshire.core :as json]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.walk :refer [keywordize-keys]]
            [immutant.web :as web]
            [ring.middleware.params :refer [wrap-params]]
            [taoensso.timbre :as log]
            [proddbot.releases :as releases]))

(defn extract-version [req]
  (when-let [key-name (get-in req [:params "version"])]
    (get-in req [::payload :build :parameters (keyword key-name)])))

(defn extract-channel [req]
  (str "#" (get-in req [:params "channel"])))

;; "origin/pr/191/merge"
(defn pr-url [git-url branch]
  (when branch
    (let [[_ type num] (str/split branch #"/")]
      (when (= "pr" type)
        (format "%s/pull/%s" (subs git-url 0 (- (count git-url) 4)) num)))))

(defn build-message [payload]
  (let [build (:build payload)
        msg (format "%s build %s %s with %s"
              (:name payload)
              (:number build)
              (:phase build)
              (:status build))
        msg (if-let [pr-url (pr-url (get-in build [:scm :url]) (get-in build [:scm :branch]))]
              (format "%s for PR %s" msg pr-url)
              msg)]
    (format "%s (%s)" msg (:full_url build))))

(defn handler [irc-fn req]
  (when-let [payload (::payload req)]
    (let [build (:build payload)
          msg (build-message payload)]
      (log/info msg)
      (irc-fn (::channel req) msg)
      (when (= "SUCCESS" (:status build))
        (when-let [ga (get-in req [:params "ga"])]
          (let [[group artifact] (str/split ga #":")]
            (run! (partial irc-fn (::channel req))
              (releases/command
                #:proddbot.releases{:cmd :add
                                    :args [(format "%s:%s:%s" group artifact (extract-version req))]}
                {:channel (extract-channel req)})))))))
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

(defn start [port host valid-token channels irc]
  (web/run
    (-> (partial handler irc)
      (wrap-phase-filtering #{"STARTED" "FINALIZED"})
      wrap-payload-decoding
      (wrap-channel-check channels)
      (wrap-token valid-token)
      wrap-params)
    :port port
    :host host))

(defn stop []
  (web/stop))
