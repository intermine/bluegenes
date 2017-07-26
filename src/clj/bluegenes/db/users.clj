(ns bluegenes.db.users
  (:require [clojure.java.jdbc :as jdbc]
            [bluegenes.mounts :refer [db]]))

;(defn users []
;  (jdbc/with-db-connection
;    [conn db]
;    (let [rows (jdbc/query conn "SELECT * FROM users")]
;      (println rows))))

(defn first-user-by-email [email]
  (jdbc/with-db-connection
    [conn db]
    (let [rows (jdbc/query conn ["SELECT * FROM users WHERE email = ?" email])]
      (first rows))))

(defn store-user! [user]
  (jdbc/with-db-connection
    [conn db]
    (jdbc/insert! conn :users user)))