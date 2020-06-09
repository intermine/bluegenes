(ns bluegenes.effects-test
  (:require [cljs.test :refer-macros [deftest is testing use-fixtures]]
            [bluegenes.test-utils :as utils]
            [re-frame.core :as rf]
            [day8.re-frame.test :refer-macros [run-test-sync run-test-async wait-for]]
            [bluegenes.effects]))

(use-fixtures :each utils/fixtures)

(deftest retry
  (run-test-async
   (rf/reg-event-fx :test-retry (fn [_ _] {:retry {:event [:test-retry-success "melon"]}}))
   (rf/reg-event-fx :test-retry-success (fn [_ [_ secret]]
                                          {:db {:done secret}
                                           :retry {:event [:test-retry-success]
                                                   :success? true}}))
   (rf/reg-sub :done (fn [db] (:done db)))
   (rf/dispatch-sync [:test-retry])
   (wait-for [:test-retry-success]
     (let [code @(rf/subscribe [:done])]
       (is (= code "melon") "Retry effect should dispatch event")))))
