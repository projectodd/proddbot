(ns proddbot.issues
  (:require [clojure.string :as str]))

(def nick-phrases
  (memoize
    (fn [base-phrases]
      (into (map (fn [s] (str/replace s #"^We'd" "%nick would")) base-phrases)
        ["It would please %nick to no end"
         "It would make %nick's day"
         "You would have %nick's eternal gratitude"
         "All the worry and doubt would rise from %nick's shoulders, and he would stand straighter, embiggened"
         "%nick's faith in mankind would assuredly be restored"]))))

(defn issue-url [config channel]
  (get-in config [:channels channel :issue-url]))

(defn phrase [{:keys [base-phrases]} nick]
  (str
   (let [phrases (if nick (nick-phrases base-phrases) base-phrases)
         phrase (nth phrases (rand-int (count phrases)))]
     (if nick (str/replace phrase "%nick" nick) phrase))
   (when (< 7 (rand-int 10)) ", my friend,")))

(defn issue-message [config channel & [nick target-nick]]
  (str
    (when target-nick (str target-nick ": "))
    (phrase config nick)
    " if you would file an issue at "
    (issue-url config channel)))

(defn issue-handler [config irc {:keys [nick text channel]}]
  (let [{:keys [directed global]} (:issue-triggers config)]
    (if-let [target (last (some #(re-find % text) directed))]
      (if (some #{target} (-> @irc (get-in [:channels channel :users]) keys))
        [(issue-message config channel nick target)]
        [(issue-message config channel)])
      (if (some #(re-find % text) global)
        [(issue-message config channel)]
        :next))))

