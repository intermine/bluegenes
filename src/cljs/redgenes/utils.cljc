(ns redgenes.utils
  ""
  (:require
    #?(:cljs [re-frame.core :as re-frame :refer [reg-event-db reg-event-fx reg-fx]])
    #?(:cljs [day8.re-frame.undo :as undo :refer [undoable]])
    [clojure.string :as string]))

(defn register!
  ([v]
   (register! @v (meta v)))
  ([f {reframe-key  :reframe-key
       reframe-kind :reframe-kind
       naym         :name
       undoable?    :undoable?
       undo-exp     :undo-exp
       :or {undoable? false}}]
      ;(println ">>" (fn? undo-exp))
   #?(:cljs
      (case reframe-kind
        :event
          (if undoable?
            (reg-event-db reframe-key
              (if undo-exp (undoable undo-exp) (undoable)) f)
            (reg-event-db reframe-key f))
        :cofx
          (if undoable?
            (reg-event-fx reframe-key
              (if undo-exp (undoable undo-exp) (undoable)) f)
            (reg-event-fx reframe-key f))
         :fx     (reg-fx reframe-key f))
      :clj
        (println "would register" naym reframe-key reframe-kind))))

(comment
  (register!
   (comp test-reset-query (fn [db _] (println "something first") db))
   {:reframe-key  :query-builder/reset-query
    :reframe-kind :effect
    }))

;(register! #'reset-query)


; A and B or B
; (A and B) or B

; A and B or B or A and A or B
; (A and B) or B or (A and A) or B

; (A and (B or A or (A and B))) or B or (A and A) or B