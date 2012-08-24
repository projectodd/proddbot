{:servers ["irc.freenode.net"]        ; A list of servers.
 :prepends #{"@"}   ; The character you want for a prepend. Currently set to @
 :max-operations 3 ; The maximum number of operations that can be running at any given time.
 :pending-ops 0    ; The number of operations running right now
 :prefix-arrow "\u21D2 "
 :clojure {:eval-prefixes {:defaults ["->" ; prefixes in any channel
                                      #"&\|(.*?)(?=\|&|\|&|$)" ; stuff like &|this|&
                                      ]}}
 "irc.freenode.net" {:channels ["##tcrawley" "#immutant" "#torquebox"
                                "#boxgrinder" "#dynjs" "#awestruct" "#aerogear"
                                "#escalante"]
                     :bot-name "proddbot"
                     :plugins #{"clojure" "javadoc" "jruby" "jira" ;"annoy-jim" "greeting"
                                }}
 :jira {:regex [#"(?i)(fil(e|ing)|create) an* (jira|issue)[?]"
                #"(?i).+@(jira|issue)"]
        "##tcrawley" "https://issues.jboss.org/browse/IMMUTANT"
        "#boxgrinder" "https://issues.jboss.org/browse/BGBUILD"
        "#immutant" "https://issues.jboss.org/browse/IMMUTANT"
        "#torquebox" "https://issues.jboss.org/browse/TORQUE"
        "#dynjs" "https://jira.codehaus.org/browse/DYNJS"
        "#awestruct" "https://github.com/awestruct/awestruct/issues"
        "#aerogear" "https://issues.jboss.org/browse/AEROGEAR"
        "#escalante" "https://issues.jboss.org/browse/ESC"}}
