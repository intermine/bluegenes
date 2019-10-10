(ns bluegenes.pages.reportpage.components.fasta
  (:require [re-frame.core :refer [subscribe]]
            [bluegenes.pages.reportpage.subs :as subs]))

(defn main
  []
  (let [fasta @(subscribe [::subs/fasta])]
    (when fasta
      [:div.fasta
       [:h3 "FASTA Sequence"]
       [:div [:pre fasta]]])))
