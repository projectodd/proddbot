(ns lazybot.plugins.jira
  (:require [lazybot.registry :as registry]
            [clojure.tools.logging :as log]))

(def we-phrasing ["We'd be happy as a big sunflower"
                  "We'd be happy as a boy at a baseball game"
                  "We'd be happy as a June bug"
                  "We'd be happy as a prince"
                  "We'd be happy as a queen"
                  "We'd be happy as a turtle dove"
                  "We'd be happy as a wave that dances on the sea"
                  "We'd be happy as the sunlight"
                  "We'd be happy as a reprieved thief"
                  "We'd be happy as a clam at high water"
                  "We'd be happy as a dinner-bell"
                  "We'd be happy as a may-pole"
                  "We'd be happy as birds in the spring"
                  "We'd be happy as a lark"
                  "We'd be happy as the kine in the fields"
                  "We'd be happy as birds that sing on a tree"
                  "We'd be happy as a fish in water"
                  "We'd be happy as spirits cleansed"
                  "We'd be happy as a king"
                  "We'd be happy as a Sunday in Paris, full of song, and dance, and laughter"
                  "We'd be happy as a lord"
                  "We'd be happy as the bird whose nest is heaven'd in the heart of purple hills"
                  "We'd be happy as a miner when he has discovered a vein of precious metal"
                  "We'd be happy as a schoolgirl going home for the holidays"
                  "We'd be happy as a priest at a wedding"
                  "We'd be happy as an enfranchised bird"
                  "We'd be happy as a poor man with a bag of gold"
                  "We'd be happy as a pig in muck"
                  "We'd be happy as a young lamb"
                  "We'd be happy as heroes after battles won"
                  "We'd be happy as the day is lang"
                  "We'd be happy as the fairest of all"
                  "We'd be happy as a serf who leaves the king ennobled"
                  "We'd be happy as a rose-tree in sunshine"
                  "We'd be happy as a child"
                  "We'd be happy as a lover"
                  "We'd be happy as birds in their bowers"
                  "We'd be happy as a wave"
                  "We'd be pleased as punch"
                  "We'd be tickled pink"
                  "We'd be happy as ') DROP TABLE Issues;--"
                  "We'd be happy as a drunk in a handbell choir"
                  "We'd be happy as a dog with two tails"
                  "We'd be happy as a moth in a sweater factory"
                  "We'd likely dance a jig"
                  "The normally docile populace would dance in the streets"
                  "We'd be happy as kids around a maypole on May Day"
                  "We would finally be Turing Complete"
                  "We'd be happy as a bear at a picnic"
                  "We'd be happy as a bear in a koi pond"
                  "We'd be happy as Burt Reynolds in the 70's"
                  "We'd be happy as a cow in Chic-fil-A"
                  "We'd be happy as a little girl with a puppy"
                  "Fireflies would tenderly alight on the hand of a child"])

(def nick-phrasing
  (into (map (fn [s] (.replace s "We'd" "%nick would")) we-phrasing)
        ["It would please %nick to no end"
         "It would make %nick's day"
         "You would have %nick's eternal gratitude"
         "All the worry and doubt would rise from %nick's shoulders, and he would stand straighter, embiggened"
         "%nick's faith in mankind would assuredly be restored"]))

(defn jira-url [bot channel]
  (get-in @bot [:config :jira channel]))

(defn phrase [nick]
  (str
   (let [phrases (if nick nick-phrasing we-phrasing)
         phrase (nth phrases (rand-int (count phrases)))]
     (if nick (.replace phrase "%nick" nick) phrase))
   (when (< 7 (rand-int 10)) ", my friend,")))

(defn jira-message [com-m bot channel & [nick prefix]]
  (registry/send-message com-m (str prefix
                                    (phrase nick)
                                    " if you would file a jira at "
                                    (jira-url bot channel))))

(registry/defplugin
  (:hook
   :on-message
   (fn [{:keys [bot message channel] :as com-m}]
     (when-let [regex (get-in @bot [:config :jira :regex])]
       (when (seq (remove nil? (map #(re-find % message) regex)))
         (jira-message com-m bot channel)))))

  (:cmd
   "Respond with the jira url"
   #{"jira"}
   (fn [{:keys [bot nick channel args] :as com-m}]
     (if-let [url (jira-url bot channel)]
       (jira-message com-m bot channel nick (and (seq args) (str (first args) ": ")))
       (registry/send-message com-m
                              (str nick ": I don't have a jira url for this channel"))))))
