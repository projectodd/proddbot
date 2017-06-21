(ns prodd.bot
  (:require [clojure.set :as set]
            [irclj.core :as irc]
            [irclj.events :as events]
            [proddbot.issues :as issues]
            [proddbot.releases :as releases]
            [proddbot.jenkins-builds :as builds]))

(defn call-handlers [config irc msg handlers]
  (when-let [h (first handlers)]
    (let [result (h config irc msg)]
      (if (not= :next result)
        result
        (recur config irc msg (rest handlers))))))

(defn msg-handler [config irc {:keys [target] :as args}]
  (when-let [result (call-handlers config irc (set/rename-keys args {:target :channel})
                      [releases/release-handler
                       issues/issue-handler])]
    (run! #(irc/message irc target %) result)))

(defn log-and-exit
  "Logs a message and does a hard exit."
  ([event-type]
   (log-and-exit event-type nil nil))
  ([event-type _]
   (log-and-exit event-type nil nil))
  ([event-type _ e]
   (when e
     (.printStackTrace e))
   (println event-type "event received, exiting")
   (System/exit 1)))

(defn start [{:keys [host port web-port web-host nick channels jenkins-token] :as config}]
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
      jenkins-token
      (set (keys channels))
      #(irc/message irc %1 %2))
    (releases/start config (fn [chan msg] (irc/message irc chan msg)))
    irc))

(defn stop [irc]
  (builds/stop)
  (releases/stop)
  (irc/kill irc))

(comment
  (def test-config
    {:host "irc.freenode.net"
     :port 6667
     :web-port 8080
     :web-host "localhost"
     :nick "proddbot2"
     :jenkins-token "foo"
     :channels {"##tcrawley" {:issue-url "https://issues.jboss.org/browse/IMMUTANT"}}
     :issue-triggers {:directed [#"(?i)^@(jira|issue) ([^\s]+)"]
                      :global [#"(?i)(fil(e|ing)|create) an* (jira|issue)[?]"
                               #"(?i)@(jira|issue)"]}
     :release-watch {:signal "@watch-release"
                     :channels #{"##tcrawley"}
                     :interval 1
                     :messages ["msg1" "msg2"]}
     :base-phrases ["We'd be tickled"
                    "We'd be blah"]})
  )
