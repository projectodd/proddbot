(ns lazybot.plugins.greeting
  (:require [lazybot.registry :as registry]))


(registry/defplugin
  (:hook
   :on-message
   (fn [{:keys [bot nick message] :as com-m}]
     (when-let [{:keys [regex response]} (get-in @bot [:config :greeting])]
       (when (seq (re-find regex message))
         (registry/send-message com-m (str nick ": " response)))))))
