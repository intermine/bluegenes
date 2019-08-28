(ns bluegenes.events-test
  (:require [cljs.test :refer-macros [deftest is testing]]
            [cljs.core.async :refer [chan put!]]
            [re-frame.core :as rf]
            [day8.re-frame.test :refer-macros [run-test-sync run-test-async wait-for]]
            [bluegenes.events]
            [bluegenes.subs]
            [imcljs.fetch :as fetch]))

;; ## Tips on writing re-frame unit tests for the novice
;;
;; When testing re-frame event handlers, write your `is` assertions inside:
;; - `run-test-sync` and use `dispatch` when there are no AJAX to wait for.
;; - `run-test-async` and use `dispatch-sync` when you have AJAX to wait on.
;;   - Use `wait-for` to block until specific event handlers are run.
;; Make sure to call any stubbing function/fixtures inside the body of the
;; above two macros, as they will also make sure to roll back the changes.
;;
;; The `(with-redefs <bindings> ...)` form is invaluable for stubbing arbitrary
;; functions. Most of the time you'll be using this for `imcljs.fetch`.
;;
;; To improve documentation, put your `is` assertions inside the body of
;; `(testing "message" ...)` forms. Calls to `testing` may be nested, and all
;; of the strings will be joined together with spaces in the final report.
;;
;; For more information, see the resources:
;; https://github.com/Day8/re-frame-test
;; https://juxt.pro/blog/posts/cljs-apps.html
;; https://clojurescript.org/tools/testing
;; https://clojure.github.io/clojure/clojure.test-api.html

(def ^:private stubbed-storage (atom nil))

(defn- stub-local-storage
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

(defn- stub-fetch-fn
  "We often want to stub imcljs.fetch functions using with-redefs. Instead of
  having to define a function to create, put and return a channel, call this
  function with the value you wish returned and it will do it for you."
  [v]
  (fn [& _]
    (let [c (chan 1)]
      (put! c v)
      c)))

(deftest new-anonymous-token
  (run-test-async
    (stub-local-storage)
    (with-redefs [fetch/session (stub-fetch-fn "stubbed-token")]
      (rf/dispatch-sync [:authentication/init])
      (wait-for [:authentication/store-token]
        (testing "fetch token when none present"
          (let [token @(rf/subscribe [:active-token])]
            (is (= "stubbed-token" token))))))))

(deftest reuse-anonymous-token
  (run-test-sync
    (stub-local-storage)
    (rf/dispatch [:authentication/store-token "existing-token"])
    (rf/dispatch [:authentication/init])
    (testing "reuse token when present"
      (let [token @(rf/subscribe [:active-token])]
        (is (= "existing-token" token))))))

(deftest reuse-login-token
  (run-test-sync
    (stub-local-storage)
    (with-redefs [fetch/lists (stub-fetch-fn [])]
      (rf/dispatch [:bluegenes.events.auth/login-success
                    {:token "login-token"}])
      (rf/dispatch [:authentication/init])
      (testing "prioritise login token"
        (let [token @(rf/subscribe [:active-token])]
          (is (= "login-token" token)))))))
