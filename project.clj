(defproject proddbot "2.0.0-SNAPSHOT"
  :description "Yet another IRC bot"
  :url "https://github.com/tobias/proddbot"
  :license {:name "Apache Software License - v 2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0"
            :distribution :repo}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [irclj "0.5.0-alpha4"]]
  :main proddbot.main
  :profiles {:dev {:source-paths ["dev"]}})
