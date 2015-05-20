(let [issue-channels {"##tcrawley"  "https://issues.jboss.org/browse/IMMUTANT"
                      "#clojars"    "https://github.com/ato/clojars-web/issues"
                      "#boxgrinder" "https://issues.jboss.org/browse/BGBUILD"
                      "#immutant"   "https://issues.jboss.org/browse/IMMUTANT"
                      "#torquebox"  "https://issues.jboss.org/browse/TORQUE"
                      "#dynjs"      "https://github.com/dynjs/dynjs/issues"
                      "#nodyn"      "https://github.com/nodyn/nodyn/issues"
                      "#awestruct"  "https://github.com/awestruct/awestruct/issues"
                      "#aerogear"   "https://issues.jboss.org/browse/AEROGEAR"
                      "#escalante"  "https://issues.jboss.org/browse/ESC"
                      "#liveoak"    "https://issues.jboss.org/browse/LIVEOAK"}]
  {:servers ["irc.freenode.net"]        ; A list of servers.
   :prepends #{"@"}   ; The character you want for a prepend. Currently set to @
   :max-operations 3 ; The maximum number of operations that can be running at any given time.
   :pending-ops 0    ; The number of operations running right now
   :prefix-arrow "\u21D2 "
   :clojure {:eval-prefixes {:defaults ["->" ; prefixes in any channel
                                        #"&\|(.*?)(?=\|&|\|&|$)" ; stuff like &|this|&
                                        ]}}
   "irc.freenode.net" {:channels (keys issue-channels)
                       :bot-name "proddbot"
                       :plugins #{"javadoc"  "jira" "github" "mail" ;"clojure"
                                  }
                       :user-blacklist #{}}
   :jira (assoc issue-channels
           :regex [#"(?i)(fil(e|ing)|create) an* (jira|issue)[?]"
                   #"(?i).+@(jira|issue)"])})
