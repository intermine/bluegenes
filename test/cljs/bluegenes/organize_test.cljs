(ns bluegenes.organize-test
  (:require [cljs.test :refer-macros [deftest is are testing]]
            [bluegenes.pages.mymine.views.organize :as organize]))

(defn tag
  "Helper function to create a compliant path tag."
  [& [s]]
  (str organize/path-tag-prefix ":" s))

(deftest extract-path-tag
  (is (= (organize/extract-path-tag ["idc" "im:public" (tag "foo.bar") "bgother:ignore"])
         ["bgpath:foo.bar" "foo.bar"]))
  (is (= (organize/extract-path-tag ["idc" "im:public" "bgother:ignore"])
         nil)))

(deftest read-path
  (is (= (organize/read-path ["idc" "im:public" (tag "foo.bar") "bgother:ignore"])
         ["foo" "bar"]))
  (is (= (organize/read-path ["idc" "im:public" "bgother:ignore"])
         nil)))

(deftest tree-path
  (is (= (organize/tree-path nil)
         [:lists]))
  (is (= (organize/tree-path ["less"])
         [:folders "less" :lists]))
  (is (= (organize/tree-path ["less" "more"])
         [:folders "less" :folders "more" :lists])))

(let [list-a {:tags [] :title "list-a" :id 0}
      list-b {:tags [(tag "less")] :title "list-b" :id 1}
      list-c {:tags [(tag "less")] :title "list-c" :id 2}
      list-d {:tags [(tag "less.deeper")] :title "list-d" :id 3}
      list-e {:tags [(tag "more")] :title "list-e" :id 4}
      tree {:lists {"list-a" list-a}
            :folders {"less" {:lists {"list-b" list-b
                                      "list-c" list-c}
                              :folders {"deeper" {:lists {"list-d" list-d}}}}
                      "more" {:lists {"list-e" list-e}}}}
      tree+empty (assoc-in tree [:folders "less" :folders "empty"]
                           {:lists {} :folders {}})]

  (deftest lists->tree
    (is (= (organize/lists->tree [list-a list-b list-c list-d list-e])
           tree)))

  (deftest tree->tags
    (is (= (organize/tree->tags tree)
           {"list-a" nil
            "list-b" (tag "less")
            "list-c" (tag "less")
            "list-d" (tag "less.deeper")
            "list-e" (tag "more")})))

  (deftest has-children?
    (is (true? (organize/has-children? tree+empty [:folders "less" :folders "deeper"])))
    (is (false? (organize/has-children? tree+empty [:folders "less" :folders "empty"]))))

  (deftest folder-exists?
    (is (true? (organize/folder-exists? tree
                                        [:folders "deeper"]
                                        [:folders "less"])))
    (is (false? (organize/folder-exists? tree
                                         [:folders "deeper"]
                                         [:folders "more"]))))

  (deftest valid-drop?
    (is (true? (organize/valid-drop?
                tree
                [:lists "Z7 Special"]
                [:folders "TFG" :folders "more" :lists "TFG Panel Extra"])))
    (is (true? (organize/valid-drop?
                tree
                [:lists "Z7 Special"]
                [:folders "TFG" :folders "more"])))
    (is (true? (organize/valid-drop?
                tree
                [:folders "TFG" :folders "more"]
                [:folders "TFG" :folders "less" :lists "TFG Panel Super"])))
    (is (true? (organize/valid-drop?
                tree
                [:folders "TFG" :folders "less" :folders "more" :lists "Z7 Special"]
                [])))
    (is (true? (organize/valid-drop?
                tree
                [:folders "TFG" :folders "foo"]
                [])))
    (is (true? (organize/valid-drop?
                tree
                [:folders "deeper"]
                [:folders "more"])))
    (is (false? (organize/valid-drop?
                 tree
                 [:folders "deeper"]
                 [:folders "less"])))
    (is (false? (organize/valid-drop?
                 tree
                 [:folders "deeper"]
                 [:folders "less" :lists "list-b"])))
    (is (false? (organize/valid-drop?
                 tree
                 [:folders "TFG" :lists "TFG Panel Multi"]
                 [:folders "TFG" :lists "TFG Panel Blue"])))
    (is (false? (organize/valid-drop?
                 tree
                 [:folders "less" :lists "TFG Panel Super"]
                 [:folders "less"])))
    (is (false? (organize/valid-drop?
                 tree
                 [:folders "TFG"]
                 [])))
    (is (false? (organize/valid-drop?
                 tree
                 [:lists "Z7 Special"]
                 [])))
    (is (false? (organize/valid-drop?
                 tree
                 [:folders "less"]
                 [:lists "Z7 Special"])))
    (is (false? (organize/valid-drop?
                 tree
                 [:folders "less"]
                 [:folders "less" :folders "more"])))
    (is (false? (organize/valid-drop?
                 tree
                 [:folders "less"]
                 [:folders "less" :lists "TFG Panel Super"])))
    (is (false? (organize/valid-drop?
                 tree
                 [:folders "less"]
                 [:folders "less" :folders "more" :lists "TFG Panel Extra"])))))

(deftest list-path?
  (is (false? (organize/list-path? [])))
  (is (false? (organize/list-path? [:folders "foo" :folders "bar"])))
  (is (true?  (organize/list-path? [:folders "foo" :lists "bar"]))))

(deftest direct-parent-of?
  (is (true? (organize/direct-parent-of?
              [:folders "one" :folders "two"]
              [:folders "one" :folders "two" :lists "foo"])))
  (is (true? (organize/direct-parent-of?
              [:folders "one"]
              [:folders "one" :lists "foo"])))
  (is (true? (organize/direct-parent-of?  [] [:lists "foo"])))
  (is (false? (organize/direct-parent-of?
               [:folders "one" :folders "two"]
               [:folders "one" :folders "two"])))
  (is (false? (organize/direct-parent-of?
               [:folders "one"]
               [:folders "one" :folders "two" :lists "foo"]))))

(deftest child-of?
  (is (true? (organize/child-of? [:folders "one" :folders "two" :lists "foo"]
                                 [:folders "one" :folders "two"])))
  (is (true? (organize/child-of? [:folders "one" :folders "two" :lists "foo"]
                                 [:folders "one"])))
  (is (false? (organize/child-of? [:folders "one" :lists "foo"]
                                  [:folders "one" :folders "two"]))))

(deftest top-node?
  (is (true? (organize/top-node? [:lists "foo"])))
  (is (true? (organize/top-node? [:folders "more"])))
  (is (false? (organize/top-node? [:folders "more" :lists "foo"])))
  (is (false? (organize/top-node? []))))

(deftest root-node?
  (is (false? (organize/root-node? [:folders "more"])))
  (is (true? (organize/root-node? []))))

(deftest trash-node?
  (is (false? (organize/trash-node? [:folders "more"])))
  (is (true? (organize/trash-node? [:trash]))))

(deftest being-dragged?
  (is (true? (organize/being-dragged? [:folders "less"] [:folders "less"])))
  (is (false? (organize/being-dragged? [:folders "less"] [:folders "more"]))))

(deftest being-dropped?
  (is (true? (organize/being-dropped? [:folders "less"] [:folders "less"])))
  (is (true? (organize/being-dropped? [:folders "less"]
                                      [:folders "less" :lists "foo"])))
  (is (false? (organize/being-dropped? [:folders "less"]
                                       [:folders "less" :folders "foo"]))))
