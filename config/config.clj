{:servers ["irc.freenode.net"]        ; A list of servers.
 :prepends #{"$"}   ; The character you want for a prepend. Currently set to @
 :dictionary {:wordnik-key "99c266291da87b231f40a0c8902040da0b568588c25526cff"} ; Wordnik API key.
 :sed {:automatic? true}
 :max-operations 3 ; The maximum number of operations that can be running at any given time.
 :pending-ops 0    ; The number of operations running right now
 :prefix-arrow "\u21D2 "
 :clojure {:eval-prefixes {:defaults ["->" "." "," ; prefixes in any channel
                                      #"&\|(.*?)(?=\|&|\|&|$)" ; stuff like &|this|&
                                      #"##(([^#]|#(?!#))+)\s*((##)?(?=.*##)|$)"]}}
 "irc.freenode.net" {:channels ["#immutant"]
                     :bot-name "proddbot"
                     :plugins #{"dictionary" "google" "clojure" "javadoc" "jruby" "seen" "sed"}}}
