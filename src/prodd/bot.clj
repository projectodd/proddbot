(ns prodd.bot
  (:require [clojure.set :as set]
            [irclj.core :as irc]
            [irclj.events :as events]
            [proddbot.issues :as issues]
            [proddbot.jenkins-builds :as builds]))

(defn msg-handler [config irc {:keys [target] :as args}]
  (run! #(irc/message irc target %)
    (issues/issue-handler config (set/rename-keys args {:target :channel}))))

(defn log-and-exit
  "Logs a message and does a hard exit."
  [event-type & [e]]
  (when e
    (.printStackTrace e))
  (println event-type "event received, exiting")
  (System/exit 1))

(defn start [{:keys [host port web-port web-host nick channels] :as config}]
  (let [irc (irc/connect host port nick
              :callbacks {:raw-log events/stdout-callback
                          :privmsg #(#'msg-handler config %1 %2)
                          :on-exception (partial log-and-exit :exception)
                          :on-shutdown (partial log-and-exit :shutdown)})]
    (when (= ::timeout (deref (:ready? @irc) 10000 ::timeout))
      (log-and-exit :timeout (ex-info "Failed to connect to irc" @irc)))
    (apply irc/join irc (keys channels))
    (builds/start
      web-port
      web-host
      (System/getenv "JENKINS_TOKEN")
      (set (keys channels))
      #(irc/message irc %1 %2))
    irc))

(defn stop [irc]
  (irc/kill irc)
  (builds/stop))

(comment
  (def test-config
    {:host "irc.freenode.net"
     :port 6667
     :web-port 8080
     :web-host "localhost"
     :nick "proddbot2"
     :channels {"##tcrawley" {:issue-url "https://issues.jboss.org/browse/IMMUTANT"}}
     :issue-triggers {:directed [#"(?i)^@(jira|issue) (.+)"]
                      :global [#"(?i)(fil(e|ing)|create) an* (jira|issue)[?]"
                           #"(?i)@(jira|issue)"]}
     :base-phrases ["We'd be tickled"
                    "We'd be blah"]})
  )
