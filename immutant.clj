(ns proddbot-immutant.init
  (:use [lazybot core irc info])
  (:require [immutant.web :as web]
            [clojure.java.io :as io]))

(alter-var-root #'*lazybot-dir* (constantly (io/file "./config")))

(web/start "/" #'lazybot.core/sroutes)

(let [config (read-config)]
  ;; this should use daemons in the future
  (lazybot.utilities/on-thread
   (initiate-mongo)
   (start-bots (:servers config))))
