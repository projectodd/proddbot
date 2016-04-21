(ns proddbot.jenkins-builds
  (:require [cheshire.core :as json]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.walk :refer [keywordize-keys]]
            [immutant.web :as web]
            [ring.middleware.params :refer [wrap-params]]
            [taoensso.timbre :as log]))

(defn handler [irc-fn req]
  (when-let [payload (::payload req)]
    (let [build (:build payload)
          msg (format "%s build %s %s with %s\n%s"
                 (:name payload)
                 (:number build)
                 (:phase build)
                 (:status build)
                 (:full_url build))]
      (log/info (first (str/split msg #"\n")))
      (irc-fn (::channel req) msg)))
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
    (let [channel (str "#" (get-in req [:params "channel"]))]
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

(defn start [port valid-token channels irc]
  (web/run
    (-> (partial handler irc)
      (wrap-phase-filtering #{"STARTED" "FINALIZED"})
      wrap-payload-decoding
      (wrap-channel-check channels)
      (wrap-token valid-token)
      wrap-params)
    :port port))

(defn stop []
  (web/stop))
