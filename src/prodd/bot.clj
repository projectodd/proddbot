(ns prodd.bot
  (:require [clojure.set :as set]
            [irclj.core :as irc]
            [irclj.events :as events]
            [proddbot.issues :as issues]))

(defn msg-handler [config irc {:keys [target] :as args}]
  (run! #(irc/message irc target %)
    (issues/issue-handler config (set/rename-keys args {:target :channel}))))

(defn start [{:keys [host port nick channels] :as config}]
  (let [irc (irc/connect host port nick
              :callbacks {:raw-log events/stdout-callback
                          :privmsg #(#'msg-handler config %1 %2)})]
    (when (= ::timeout (deref (:ready? @irc) 10000 ::timeout))
      (throw (ex-info "Failed to connect to irc" @irc)))
    (apply irc/join irc (keys channels))
    irc))

(defn stop [irc]
  (irc/kill irc))

(comment
  (def test-config
    {:host "irc.freenode.net"
     :port 6667
     :nick "proddbot2"
     :channels {"##tcrawley" {:issue-url "https://issues.jboss.org/browse/IMMUTANT"}}
     :issue-triggers {:directed [#"(?i)^@(jira|issue) (.+)"]
                      :global [#"(?i)(fil(e|ing)|create) an* (jira|issue)[?]"
                           #"(?i)@(jira|issue)"]}
     :base-phrases ["We'd be tickled"
                    "We'd be blah"]})
  )