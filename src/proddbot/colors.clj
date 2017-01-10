(ns proddbot.colors)

(def colors
  (zipmap
    [:white :black :blue :green :light-red :brown :purple :orange
     :yellow :light-green :cyan :light-cyan :light-blue :pink :grey
     :light-grey]
    (range)))

(defn color [c]
  (if-let [color (colors c)]
    color
    (throw (ex-info (format "%s isn't a legal color." c) {:legal-colors (sort (keys colors))}))))

(defn fc [c]
  (format "%c%02d" (char 3) (color c)))

(defn bc [c]
  (format "%c,%02d" (char 3) (color c)))

(defn c [fg bg]
  (format "%c%02d,%02d" (char 3) (color fg) (color bg)))

(def nc (constantly (str (char 15))))

(defn with-color
  ([fg msg]
   (format "%s%s%s" (fc fg) msg (nc)))
  ([fg bg msg]
   (format "%s%s%s" (c fg bg) msg (nc))))
