(ns bluegenes.test-utils
  (:require [cljs.core.async :refer [chan put!]]
            [re-frame.core :as rf]))

;; Instructions for writing tests can be found in `docs/developing.md`.

;; Remember to activate the fixtures in your testing namespace if you're
;; going to use exports from this namespace:
;;
;;     (use-fixtures :each utils/fixtures)

(def stubbed-storage
  "Represents the contents of the local storage, as mutated using the effects
  below. A fixture will reset this atom to nil before every test, so mutate and
  read to your heart's content when writing tests for local storage changes."
  (atom nil))

(defn stub-local-storage
  "Stub the local-storage effect and coeffect to use an atom instead of the
  localStorage API."
  []
  (rf/reg-cofx
   :local-store
   (fn [coeffects key]
     (let [key (str key)
           value (get @stubbed-storage key)]
       (assoc coeffects :local-store value))))
  (rf/reg-fx
   :persist
   (fn [[key value]]
     (let [key (str key)]
       (if (some? value)
         (swap! stubbed-storage assoc key value)
         (swap! stubbed-storage dissoc key)))))
  nil)

(defn stub-fetch-fn
  "We often want to stub imcljs.fetch functions using with-redefs. Instead of
  having to define a function to create, put and return a channel, call this
  function with the value you wish returned and it will do it for you."
  [v]
  (fn [& _]
    (let [c (chan 1)]
      (put! c v)
      c)))

(def stubbed-variables
  "Add functions that reset any globally stubbed variables to this atom.
      (swap! stubbed-variables conj #(set! fetch/session orig-fn))
  A fixture will run all these functions and empty the atom before tests."
  (atom '()))

;; This will be used to clear app-db between test runs.
(rf/reg-event-db
 :clear-db
 (fn [_db] {}))

(def fixtures
  "Necessary fixtures to use the exports from this namespace.
  Use by calling use-fixtures from your testing namespace:
      (use-fixtures :each utils/fixtures)"
  {:before (fn []
             (when (some? @stubbed-storage)
               (reset! stubbed-storage nil))
             (when-let [vars (seq @stubbed-variables)]
               (doseq [restore-fn vars]
                 (restore-fn))
               (reset! stubbed-variables '()))
             (rf/dispatch-sync [:clear-db])
             ;; Stub for gtag tracking function.
             (set! js/gtag (fn [])))})
