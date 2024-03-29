(ns bluegenes.pages.regions.utils
  (:require [clojure.string :as str]
            [bluegenes.components.icons :refer [icon]]))

;; Base pair input and notation

(defn linear->log [x]
  (if (< x 1)
    0
    (js/Math.trunc (js/Math.pow 10 (/ x 10)))))

(defn log->linear [x]
  (if (< x 1)
    0
    (js/Math.trunc (* 10 (js/Math.log10 x)))))

(defn parse-bp [s]
  (let [[matched number unit :as match] (re-matches #"(\d*\.?\d*)([kKmM]?)" s)]
    (when match
      [matched
       (* (if (not-empty number)
            (js/parseFloat number 10)
            0)
          (case (str/lower-case unit)
            "k" 1000
            "m" 1e6
            1))])))

(defn bp->int [bp]
  (if (string? bp)
    (js/Math.trunc (-> (parse-bp bp) (second) (or 0)))
    0))

(defn one-decimal [number]
  (-> number (* 10) (js/Math.trunc) (/ 10)))

(defn int->bp [number]
  (cond
    (>= number 1e6) (str (one-decimal (/ number 1e6)) "M")
    (>= number 1000) (str (one-decimal (/ number 1000)) "k")
    :else (str number)))

;; Strand matching

(let [plus #{1 "1" "+1" "+" "plus" "positive" "forward"}
      minus #{-1 "-1" "-" "minus" "negative" "reverse"}
      unknown #{0 "0" "" nil}]

  (defn strands-match? [s1 s2]
    (or (every? #(contains? plus %) [s1 s2])
        (every? #(contains? minus %) [s1 s2])
        (= s1 s2)))

  (defn strand-indicator [strand]
    (condp contains? strand
      plus    [icon "arrow-right"]
      minus   [icon "arrow-left"]
      unknown nil
      (str " " strand " ")))

  (defn strand-cell [strand]
    (condp contains? strand
      plus    "+"
      minus   "−"
      unknown "∙"
      strand)))
