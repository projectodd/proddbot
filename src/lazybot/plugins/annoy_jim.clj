(ns lazybot.plugins.annoy-jim
  (:require [lazybot.registry :as registry]))


(registry/defplugin
  (:hook
   :on-message
   (fn [{:keys [nick] :as com-m}]
     (when (seq (re-find #"jcrossley" nick))
       (when (< 7 (rand-int 10))
         (registry/send-message com-m (str nick ": no")))))))


