(ns proddbot.main
  (:require [prodd.bot :as bot]
            [clojure.java.io :as io]))

(defn config []
  (-> "config.edn" io/resource slurp read-string))

(defn -main [& args]
  (bot/start (config)))

