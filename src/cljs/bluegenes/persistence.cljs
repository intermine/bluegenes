(ns bluegenes.persistence
  "
  /*
  * Copyright (C) 2016 Intermine
  *
  * This code may be freely distributed and modified under the
  * terms of the GNU Lesser General Public Licence. This should
  * be distributed with the code. See the LICENSE file for more
  * information or http://www.gnu.org/copyleft/lesser.html.
  *
  */

  This namespace provides functions for saving
  and loading the state of the application to&from
  localstorage

  Also functions for exporting in various formats
  "
  (:require
    [dommy.core :as dommy :refer-macros [sel sel1]]
    [cognitect.transit :as t]))

(defn merge-state [state other-state except-paths]
  (merge state
    (reduce
      (fn [r p]
        (assoc-in r p (get-in state p)))
      other-state
      except-paths)))

(defn to-transit [state]
  (t/write (t/writer :json-verbose) state))

(defn persist! [state]
  (js/localStorage.setItem "bluegenes/state" (to-transit state))
  state)

(defn destroy! []
  (js/localStorage.removeItem "bluegenes/state")
  true)

(defn get-state!
  "Returns the merging of the given state
  with the one in localstorage, except the given paths"
  ([]
   (get-state! {} []))
  ([state]
    (get-state! state []))
  ([state paths]
    (merge-state state
      (t/read (t/reader :json)
        (js/localStorage.getItem "bluegenes/state"))
      paths)))

(defn merge-state-from-file [state file]
  (merge state (t/read (t/reader :json) file)))

(defn download!
  "Contrive to 'download' the given contents
  as a file locally to be saved on the user's
  magnetic disk-drive storage medium"
  ([tipe naym contents]
    (let [
          a (.createElement js/document "a")
          f (js/Blob. (clj->js [contents]) {:type (name tipe)})
          ]
      (set! (.-href a) (.createObjectURL js/URL f))
      (set! (.-download a) (str naym "." (name tipe)))
      (println "<a>" a)
      (.dispatchEvent a (js/MouseEvent. "click")))))

(defn make-filename [s]
  (str (get-in s [:msas (:selected-msa s) :name]) "-" (:selected-msa s) ".dg"))

(defn load! [s]
  (-> (sel1 :#file_button) .click)
   s)

(defn save! [s]
  (download! "JSON"
    (make-filename s)
    (to-transit s)) s)
