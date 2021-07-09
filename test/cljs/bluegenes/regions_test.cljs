(ns bluegenes.regions-test
  (:require [cljs.test :refer-macros [deftest is are testing]]
            [bluegenes.pages.regions.events :refer [parse-region]]))

(deftest parsing-genome-regions
  (testing "Accepted formats in help text"
    (are [in out] (= (parse-region nil in) out)
      "2L:11334..12296"
      {:chromosome "2L" :from 11334 :to 12296}
      "2R:5866746-5868284"
      {:chromosome "2R" :from 5866746 :to 5868284}
      "chrII:14646344-14667746"
      {:chromosome "chrII" :from 14646344 :to 14667746}
      "3R:2578486:2580016:-1"
      {:chromosome "3R" :from 2578486 :to 2580016 :strand "-1"}
      "2L:14615455:14619002:1"
      {:chromosome "2L" :from 14615455 :to 14619002 :strand "1"}
      "2L\t11334\t12296"
      {:chromosome "2L" :from 11334 :to 12296}))
  (is (= (parse-region nil "2L:12296..11334")
         {:chromosome "2L" :from 11334 :to 12296})
      "Should handle descending coordinates")
  (testing "Leading and trailing whitespace"
    (is (= (parse-region nil "  2L:12296..11334  ")
           {:chromosome "2L" :from 11334 :to 12296})
        "Should be removed")
    (is (= (parse-region nil "  2L:12296:11334:-1  ")
           {:chromosome "2L" :from 11334 :to 12296 :strand "-1"})
        "Should be removed also after strand")
    (testing "Nil when instead of chromosome and/or strand"
      (are [in] (nil? (parse-region nil in))
        "  :12296:11334:  "
        "  :12296..11334  "
        "  12296..11334  ")))
  (testing "Interbase coordinate system"
    (is (= (parse-region {:coords :interbase} "2L:11334..12296")
           {:chromosome "2L" :from 11335 :to 12296})
        "Should translate interbase coordinates")
    (is (= (parse-region {:coords :interbase} "2L:12296..11334")
           {:chromosome "2L" :from 11335 :to 12296})
        "Should translate interbase coordinates also for descending coordinates"))
  (testing "Strand-specific search"
    (is (= (parse-region {:strand-specific true} "2L:11334..12296")
           {:chromosome "2L" :from 11334 :to 12296 :strand "1"})
        "Ascending coordinates should use plus strand")
    (is (= (parse-region {:strand-specific true} "2L:12296..11334")
           {:chromosome "2L" :from 11334 :to 12296 :strand "-1"})
        "Descending coordinates should use minus strand")
    (is (= (parse-region {:coords :interbase :strand-specific true} "2L:12296..11334")
           {:chromosome "2L" :from 11335 :to 12296 :strand "-1"})
        "Should still translate interbase coordinates")
    (is (= (parse-region {:strand-specific true} "2L:12296:11334:1")
           {:chromosome "2L" :from 11334 :to 12296 :strand "1"})
        "Should be overridden when explicitly specifying strand")
    (is (= (parse-region {:coords :interbase :strand-specific true} "2L:12296:11334:1")
           {:chromosome "2L" :from 11335 :to 12296 :strand "1"})
        "Should be overridden when explicitly specifying strand and still translate interbase coordinates")))
