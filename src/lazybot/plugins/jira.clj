(ns lazybot.plugins.jira
  (:require [lazybot.registry :as registry]
            [clojure.tools.logging :as log]))

(defn jira-url [bot channel]
  (get-in @bot [:config :jira channel :url]))

(defn jira-message [com-m bot channel & [prefix]]
  (registry/send-message com-m (str prefix "You can file a jira at " (jira-url bot channel))))

(registry/defplugin
  (:hook
   :on-message
   (fn [{:keys [bot message channel] :as com-m}]
     (when-let [regex (get-in @bot [:config :jira channel :regex])]
       (when (seq (re-find regex message))
         (jira-message com-m bot channel)))))
  
  (:cmd
   "Respond with the jira url"
   #{"jira"}
   (fn [{:keys [bot nick channel args] :as com-m}]
     (log/info "called" args)
     
     (if-let [url (jira-url bot channel)]
       (jira-message com-m bot channel (and (seq args) (str (first args) ": ")))
       (registry/send-message com-m
                              (str nick ": I don't have a jira url for this channel"))))))
