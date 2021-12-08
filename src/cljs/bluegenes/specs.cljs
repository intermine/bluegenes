(ns bluegenes.specs
  "Provides shapes of InterMine and BlueGenes data"
  (:require [clojure.spec.alpha :as s]))

(s/def ::empty-non-string (s/and empty? (complement string?)))
(s/def ::integer-keyword (s/and keyword? #(re-matches #"^[0-9]+$" (name %))))

(s/def :bluegenes.webproperties.project.credit.item/text string?)
(s/def :bluegenes.webproperties.project.credit.item/image string?)
(s/def :bluegenes.webproperties.project.credit.item/url string?)

(s/def :bluegenes.webproperties.project.credit/section (s/keys :req-un [:bluegenes.webproperties.project.credit.item/text
                                                                        :bluegenes.webproperties.project.credit.item/image
                                                                        :bluegenes.webproperties.project.credit.item/url]))
(s/def :bluegenes.webproperties.project.credit/logo (s/keys :req-un [:bluegenes.webproperties.project.credit.item/image
                                                                     :bluegenes.webproperties.project.credit.item/url]))

(s/def :bluegenes.webproperties.project.credit/item (s/or :logo :bluegenes.webproperties.project.credit/logo
                                                          :section :bluegenes.webproperties.project.credit/section))
(s/def :bluegenes.webproperties.project/credit (s/or :empty ::empty-non-string
                                                     :map (s/map-of ::integer-keyword :bluegenes.webproperties.project.credit/item)))

(s/def :bluegenes.webproperties.customisation.homepage.cta.item/label string?)
(s/def :bluegenes.webproperties.customisation.homepage.cta.item/text string?)

(s/def :bluegenes.webproperties.customisation.homepage.cta.item/route string?)
(s/def :bluegenes.webproperties.customisation.homepage.cta.item/dispatch string?)
(s/def :bluegenes.webproperties.customisation.homepage.cta.item/url string?)

(s/def :bluegenes.webproperties.customisation.homepage.cta/route (s/keys :req-un [:bluegenes.webproperties.customisation.homepage.cta.item/label
                                                                                  :bluegenes.webproperties.customisation.homepage.cta.item/route
                                                                                  :bluegenes.webproperties.customisation.homepage.cta.item/text]))

(s/def :bluegenes.webproperties.customisation.homepage.cta/dispatch (s/keys :req-un [:bluegenes.webproperties.customisation.homepage.cta.item/label
                                                                                     :bluegenes.webproperties.customisation.homepage.cta.item/dispatch
                                                                                     :bluegenes.webproperties.customisation.homepage.cta.item/text]))

(s/def :bluegenes.webproperties.customisation.homepage.cta/url (s/keys :req-un [:bluegenes.webproperties.customisation.homepage.cta.item/label
                                                                                :bluegenes.webproperties.customisation.homepage.cta.item/url
                                                                                :bluegenes.webproperties.customisation.homepage.cta.item/text]))

(s/def :bluegenes.webproperties.customisation.homepage.cta/item (s/or :route :bluegenes.webproperties.customisation.homepage.cta/route
                                                                      :dispatch :bluegenes.webproperties.customisation.homepage.cta/dispatch
                                                                      :url :bluegenes.webproperties.customisation.homepage.cta/url))

(s/def :bluegenes.webproperties.customisation.homepage/cta (s/or :empty ::empty-non-string
                                                                 :map (s/map-of ::integer-keyword :bluegenes.webproperties.customisation.homepage.cta/item)))

;;;; Below is old!

; Note: It's really worth revisiting this in the future. Creating specs for InterMine
; related things like queries, data models, report page widgets, etc can be REALLY powerful
; because we can programmatically capture why something doesn't conform *before* it throws an error.
;
; Creating a spec for data that moves in and out of report page widgets is a really good place to start.

; (defn one-of? [haystack needle] (some? (some #{needle} haystack)))

; (s/def :package/type (s/and keyword? (partial one-of? [:query])))
; (s/def :package/value some?)
; (s/def :package/source keyword?)

; (comment
;   "The smallest amount of data needed to distinguish InterMine objects
;    from different sources. For example:"
;   {:source :flymine-beta
;    :type :query
;    :value {:from "Gene" :select ["Gene.id"] :where [{:path "Gene.id" :op "=" :value 123}]}})

; (def im-package (s/keys :req-un [:package/source
;                                  :package/type
;                                  :package/value]))
