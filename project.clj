(defproject proddbot "2.0.0-SNAPSHOT"
  :description "Yet another IRC bot"
  :url "https://github.com/tobias/proddbot"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [irclj "0.5.0-alpha4"]]
  :main proddbot.main
  :profiles {:dev {:source-paths ["dev"]}
             :uberjar {:aot :all}})
