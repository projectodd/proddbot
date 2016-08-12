(ns proddbot.releases
  (:require [clj-http.lite.client :as http]
            [clojure.string :as str]
            [immutant.scheduling :as s]
            [irclj.core :as irc]
            [taoensso.timbre :as log])
  (:import [java.util Date TimeZone]
           java.text.SimpleDateFormat))

(defonce watches (atom #{}))

(def add-re #"([^:]+):([^:]+):([^:]+)")

(def cancel-re #"^cancel (\d+)")

(def date-formatter (doto (SimpleDateFormat. "yyyy-MM-dd HH:mm z")
                      (.setTimeZone (TimeZone/getTimeZone "UTC"))))

(defn artifact->str [{:keys [::group ::artifact ::version]}]
  (format "%s:%s:%s" group artifact version))

(defn parse-cancel [str]
  {::cmd ::cancel
   ::num (->> str (re-find cancel-re) last read-string)})

(defn parse-add [str]
  (let [[_ group artifact version] (re-find add-re str)]
    {::cmd ::add
     ::group group
     ::artifact artifact
     ::version version}))

(defn parse-command [str]
  (cond
    (empty? str)             {::cmd ::list}
    (re-find #"^usage$" str) {::cmd ::usage}
    (re-find cancel-re str)  (parse-cancel str)
    (re-find add-re str)     (parse-add str)))

(defn filter-by-channel [channel watches]
  (filter #(= (::channel %) channel) watches))

(defn sort-by-time [watches]
  (sort-by ::time watches))

(defn watch-list-for-channel [channel]
  (->> @watches (filter-by-channel channel) sort-by-time))

(defn watch->str [{:keys [::nick ::time] :as watch}]
  (format "%s by: %s at: %s" (artifact->str watch) nick (.format date-formatter time)))

(defmulti command (fn [data _] (::cmd data)))

(defmethod command ::add [cmd {:keys [nick channel]}]
  (let [watch (-> cmd
                (dissoc ::cmd)
                (assoc ::nick nick ::channel channel ::time (Date.)))]
    (swap! watches conj watch)
    [(format "watch for %s created" (artifact->str watch))]))

(defmethod command ::cancel [{:keys [::num]} {:keys [channel]}]
  (if-let [w (nth (watch-list-for-channel channel) num nil)]
    (do
      (swap! watches disj w)
      [(format "watch for %s canceled" (artifact->str w))])
    ["no matching watch found!"]))

(defmethod command ::list [_ {:keys [channel]}]
  (let [watches (watch-list-for-channel channel)]
    (if (seq watches)
      (concat ["Current watches:"]
        (map-indexed
          (fn [n w]
            (format "%s) %s" n (watch->str w)))
          watches))
      ["No active watches"])))

(defmethod command ::usage [_ {:keys [::signal]}]
  [(format "usage: '%s group:artifact:version' to add watch" signal)
   (format "       '%s' to list current watches" signal)
   (format "       '%s cancel n' to cancel watch # n" signal)])


(defn handle-release [signal {:keys [nick] :as msg} text]
  (if-let [cmd (parse-command text)]
    (command cmd (assoc msg ::signal signal))
    [(format "%s: I don't understand. Try `%s usage`" nick signal)]))

(defn release-handler [config _ {:keys [channel text] :as msg}]
  (let [{:keys [channels signal]} (:release-watch config)]
    (if (and (some #{channel} channels)
          (.startsWith text signal))
      (handle-release signal msg (-> text (subs (count signal)) str/trim))
      :next)))

(defn artifact->url [{:keys [::group ::artifact ::version]}]
  (format "http://central.maven.org/maven2/%s/%s/%s/%s-%s.pom"
    (str/replace group #"\." "/")
    artifact version artifact version))

(defn artifact-available? [artifact]
  (let [url (artifact->url artifact)]
    (log/info (format "checking watch url %s" url))
    (try
      (-> (http/head url {:throw-exceptions false})
        :status
        (= 200))
      (catch Throwable e
        (log/warn e)
        false))))

(defn happy-message [config]
  (rand-nth (-> config :release-watch :messages)))

(defn check-watches [config send-fn]
  (doseq [{:keys [::nick ::channel] :as watch} @watches]
    (when (artifact-available? watch)
      (swap! watches disj watch)
      (send-fn channel (format "%s: %s %s is now available in central"
                         nick (happy-message config) (artifact->str watch))))))

(defonce timer-id (atom nil))

(defn start [config send-fn]
  (reset! timer-id
    (s/schedule (partial check-watches config send-fn)
      (s/every (-> config :release-watch :interval) :minutes))))

(defn stop []
  (when-let [id @timer-id]
    (s/stop id)))


