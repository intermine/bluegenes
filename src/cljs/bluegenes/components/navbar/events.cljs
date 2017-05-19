(ns bluegenes.components.navbar.events
  (:require [re-frame.core :as re-frame :refer [reg-event-db reg-event-fx reg-fx dispatch subscribe]]
    [oops.core :refer [ocall oapply oget oset!]]
))

(reg-fx
  :visual-navbar-minechange
  (fn []
    ;;makes sure that the user notices the mine has changed.
    (let [navbar (.querySelector js/document ".navbar-brand")
          navbar-class (.-className navbar)]
      (oset! navbar ["className"] (str navbar-class " recently-changed"))
      (.setTimeout js/window #(oset! navbar ["className"] navbar-class) 3000)
  )))

