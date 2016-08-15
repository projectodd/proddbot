(ns proddbot.releases
  (:require [clj-http.lite.client :as http]
            [clojure.string :as str]
            [immutant.scheduling :as s]
            [irclj.core :as irc]
            [taoensso.timbre :as log])
  (:import [java.util Date TimeZone]
           java.text.SimpleDateFormat))

(defonce watches (atom #{}))

(def date-formatter (doto (SimpleDateFormat. "yyyy-MM-dd HH:mm z")
                      (.setTimeZone (TimeZone/getTimeZone "UTC"))))

(defn artifact->str [{:keys [::group ::artifact ::version]}]
  (format "%s:%s:%s" group artifact version))

(defn parse-command [str]
  (let [[cmd & rest] (str/split str #" ")]
    {::cmd (when (and cmd (not (empty? cmd))) (keyword cmd))
     ::args (map str/trim rest)}))

(defn filter-by-channel [channel watches]
  (filter #(= (::channel %) channel) watches))

(defn sort-by-time [watches]
  (sort-by ::time watches))

(defn watch-list-for-channel [channel]
  (->> @watches (filter-by-channel channel) sort-by-time))

(defn watch->str [{:keys [::nick ::time] :as watch}]
  (format "%s by: %s at: %s" (artifact->str watch) (or nick "<CI>") (.format date-formatter time)))

(defn usage [signal]
  [(format "usage: '%s add group:artifact:version' to add watch" signal)
   (format "       '%s list' to list current watches" signal)
   (format "       '%s cancel n' to cancel watch # n" signal)])

(defmulti command (fn [data _] (::cmd data)))

(defmethod command :add [{:keys [::args]} {:keys [nick channel ::signal]}]
  (let [add-re #"([^:]+):([^:]+):([^:]+)"]
    (if (and (= 1 (count args))
          (re-find add-re (first args)))
      (let [[_ group artifact version] (re-find add-re (first args))
            watch {::group group
                   ::artifact artifact
                   ::version version
                   ::nick nick
                   ::channel channel
                   ::time (Date.)}]
        (swap! watches conj watch)
        [(format "watch for %s created" (artifact->str watch))])
      (usage signal))))

(defmethod command :cancel [{:keys [::args]} {:keys [channel ::signal]}]
  (if (and (= 1 (count args))
        (re-find #"^\d+$" (first args)))
    (if-let [w (nth (watch-list-for-channel channel) (read-string (first args)) nil)]
      (do
        (swap! watches disj w)
        [(format "watch for %s canceled" (artifact->str w))])
      ["no matching watch found"])
    (usage signal)))

(defmethod command :list [_ {:keys [channel]}]
  (let [watches (watch-list-for-channel channel)]
    (if (seq watches)
      (concat ["Current watches:"]
        (map-indexed
          (fn [n w]
            (format "%s) %s" n (watch->str w)))
          watches))
      ["No active watches"])))

(defmethod command :default [_ {:keys [::signal]}]
  (usage signal))

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
      (send-fn channel (format "%s%s %s is now available in central"
                         (if nick (str nick ": ") "")
                         (happy-message config)
                         (artifact->str watch))))))

(defonce timer-id (atom nil))

(defn start [config send-fn]
  (reset! timer-id
    (s/schedule (partial check-watches config send-fn)
      (s/every (-> config :release-watch :interval) :minutes))))

(defn stop []
  (when-let [id @timer-id]
    (s/stop id)))


