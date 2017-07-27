(ns bluegenes.db.users
  (:require [clojure.java.jdbc :as jdbc]
            [bluegenes.mounts :refer [db]]))

;(defn users []
;  (jdbc/with-db-connection
;    [conn db]
;    (let [rows (jdbc/query conn "SELECT * FROM users")]
;      (println rows))))

(defn first-user-by-name [username]
  (jdbc/with-db-connection
    [conn db]
    (let [rows (jdbc/query conn ["SELECT * FROM users WHERE username = ?" username])]
      (first rows))))

(defn store-user! [user]
  (jdbc/with-db-connection
    [conn db]
    (first (jdbc/insert! conn :users user))))