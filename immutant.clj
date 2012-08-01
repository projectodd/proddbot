(ns proddbot-immutant.init
  (:use [lazybot core irc info]
        [immutant.utilities :only [app-root]])
  (:require [lazybot.info :as info]
            [immutant.web :as web]
            [immutant.daemons :as daemons]
            [clojure.java.io :as io]))

(alter-var-root #'*lazybot-dir* (constantly (io/file (app-root) "config")))
(println "root:" *lazybot-dir*)
(web/start "/" #'lazybot.core/sroutes)

(let [config (info/read-config)]
  ;;(println config)
  (daemons/start "bot"
                 (fn []
                          (println "STARTING")
                          (initiate-mongo)
                          (start-bots (:servers config)))
                 :no-stop-function))
