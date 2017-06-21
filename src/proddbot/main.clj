(ns proddbot.main
  (:require [prodd.bot :as bot]
            [clojure.java.io :as io])
  (:gen-class))

(defn read-config [path]
  (-> path io/file slurp read-string))

(defn override-config-from-sysprops [cfg]
  (->> (for [[name value] (System/getProperties)
             :when (.startsWith name "proddbot.")
             :let [name (keyword (subs name (count "proddbot.")))]]
         [name value])
    (into {})
    (merge cfg)))

(defn -main [& args]
  (if (not= 1 (count args))
    (println "Path to config file should be the only arg")
    (bot/start (-> (first args)
                 read-config
                 override-config-from-sysprops))))

