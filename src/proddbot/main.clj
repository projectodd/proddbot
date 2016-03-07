(ns proddbot.main
  (:require [prodd.bot :as bot]
            [clojure.java.io :as io])
  (:gen-class))

(defn read-config [path]
  (-> path io/file slurp read-string))

(defn -main [& args]
  (if (not= 1 (count args))
    (println "Path to config file should be the only arg")
    (bot/start (read-config (first args)))))

