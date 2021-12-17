(ns bluegenes.events-test
  (:require [cljs.test :refer-macros [deftest is testing use-fixtures]]
            [bluegenes.test-utils :as utils]
            [re-frame.core :as rf]
            [day8.re-frame.test :refer-macros [run-test-sync run-test-async wait-for]]
            [bluegenes.events]
            [bluegenes.subs]
            [imcljs.fetch :as fetch]
            [imcljs.auth :as auth]
            [bluegenes.config :as config]))

(use-fixtures :each utils/fixtures)

(deftest new-anonymous-token
  (run-test-async
   (utils/stub-local-storage)
   (with-redefs [fetch/session  (utils/stub-fetch-fn "stubbed-token")
                 auth/who-am-i? (utils/stub-fetch-fn {})
                 config/init-vars (delay nil)]
     (rf/dispatch-sync [:authentication/init])
     (wait-for [:authentication/store-token]
       (testing "fetch token when none present"
         (let [token @(rf/subscribe [:active-token])]
           (is (= "stubbed-token" token))))))))

(deftest reuse-anonymous-token
  (run-test-sync
   (utils/stub-local-storage)
   (rf/dispatch [:authentication/store-token "existing-token"])
   (with-redefs [auth/who-am-i? (utils/stub-fetch-fn {})
                 config/init-vars (delay nil)]
     (rf/dispatch [:authentication/init])
     (testing "reuse token when present"
       (let [token @(rf/subscribe [:active-token])]
         (is (= "existing-token" token)))))))

(deftest reuse-login-token
  (run-test-sync
   (utils/stub-local-storage)
   ;; Starting the router causes a crash here, so change it to noop.
   (rf/reg-event-fx :bluegenes.events.boot/start-router (fn [] {}))
   (with-redefs [fetch/lists     (utils/stub-fetch-fn [])
                 fetch/templates (utils/stub-fetch-fn {})
                 auth/who-am-i?  (utils/stub-fetch-fn {})
                 config/init-vars (delay nil)]
     (rf/dispatch [:bluegenes.events.auth/login-success
                   {:identity {:token "login-token"}}])
     (rf/dispatch [:authentication/init])
     (testing "prioritise login token"
       (let [token @(rf/subscribe [:active-token])]
         (is (= "login-token" token)))))))

(deftest reuse-persisted-login-token
  (run-test-async
   (utils/stub-local-storage)
   (rf/dispatch-sync [:save-login nil {:token "persisted-token"}])
   (with-redefs [auth/who-am-i? (utils/stub-fetch-fn {})
                 config/init-vars (delay nil)]
     (rf/dispatch-sync [:authentication/init])
     (wait-for [:authentication/store-token]
       (testing "use persisted login token"
         (let [token @(rf/subscribe [:active-token])]
           (is (= "persisted-token" token))))))))

(deftest use-oauth2-token
  (run-test-async
   (utils/stub-local-storage)
   (with-redefs [auth/who-am-i? (utils/stub-fetch-fn {})
                 config/init-vars (delay {:identity {:token "oauth2-token"}})]
     (rf/dispatch-sync [:authentication/init])
     (wait-for [:authentication/store-token]
       (testing "use oauth2 token"
         (let [token @(rf/subscribe [:active-token])]
           (is (= "oauth2-token" token))))))))

(deftest replace-invalid-token
  (run-test-async
   (utils/stub-local-storage)
   (let [orig-fn fetch/session]
     (set! fetch/session (utils/stub-fetch-fn "valid-token"))
     (swap! utils/stubbed-variables conj #(set! fetch/session orig-fn)))
   (rf/dispatch-sync [:authentication/store-token "invalid-token"])
   (with-redefs [auth/who-am-i? (utils/stub-fetch-fn {:statusCode 401})
                 config/init-vars (delay nil)]
     (rf/dispatch-sync [:authentication/init])
     (wait-for [:authentication/store-token]
       (testing "replace token when invalid"
         (let [token @(rf/subscribe [:active-token])]
           (is (= "valid-token" token))))))))

(deftest clear-invalid-login-token
  (run-test-async
   (utils/stub-local-storage)
   (let [orig-fn fetch/session]
     (set! fetch/session (utils/stub-fetch-fn "anon-token"))
     (swap! utils/stubbed-variables conj #(set! fetch/session orig-fn)))
   (rf/dispatch-sync [:save-login nil {:token "login-token"}])
   (with-redefs [auth/who-am-i? (utils/stub-fetch-fn {:statusCode 401})
                 config/init-vars (delay nil)]
     (rf/dispatch-sync [:authentication/init])
     (wait-for [:authentication/store-token]
       (testing "clear login when invalid"
         (let [authenticated? @(rf/subscribe [:bluegenes.subs.auth/authenticated?])]
           (is (= false authenticated?))))
       (testing "replace token with new anonymous token"
         (let [token @(rf/subscribe [:active-token])]
           (is (= "anon-token" token))))
       (testing "remove invalid login from localstorage"
         (let [login (get @utils/stubbed-storage ":bluegenes/login")]
           (is (empty? login))))))))
