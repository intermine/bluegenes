(ns bluegenes.events.webproperties
  (:require [re-frame.core :refer [reg-event-db reg-event-fx subscribe]]
            [bluegenes.db :as db]
            [imcljs.fetch :as fetch]
            [bluegenes.events.registry :as registry]
            [clojure.set :refer [rename-keys]]
            [clojure.string :as string]
            [clojure.spec.alpha :as s]))

(defn capitalize-kw [kw]
  (->> (name kw)
       string/capitalize
       keyword))

(defn parse-citation
  "Returns a URL citation or the URL of the first anchor element if it's HTML."
  [citation]
  (when-let [cit (not-empty citation)]
    (or (re-matches #"\S+" cit) ;; If there are no spaces, it's probably a URL.
        ;; Otherwise, extract it from the href attribute.
        (second (re-find #"href=[\"']([^\"']+)" cit)))))

(defn parse-numbered-properties
  "Converts a web property object with numbered keys and object values to a
  vector of maps in the correct order."
  [object spec web-property-path]
  (if (s/valid? spec object)
    (->> object
         (map (juxt (comp js/parseInt name key) val))
         (into (sorted-map))
         (vals)
         (vec))
    (do (.error js/console
                (str "Invalid web property " web-property-path ": " (s/explain-str spec object)))
        nil)))

(defn web-properties-to-bluegenes
  "Map intermine web properties to bluegenes properties"
  [web-properties]
  {:name                         (get-in web-properties [:project :title])
   :description                  (get-in web-properties [:project :subTitle])
   :release                      (get-in web-properties [:project :releaseVersion])
   :default-organism             (->> (string/split (get-in web-properties [:genomicRegionSearch :defaultOrganisms]) #",")
                                      (map string/trim)
                                      (first))
   :regionsearch-example         (get-in web-properties [:genomicRegionSearch :defaultSpans])
   :news                         (get-in web-properties [:project :news])
   :rss                          (get-in web-properties [:project :rss])
   :citation                     (or (parse-citation (get-in web-properties [:project :citation])) "http://intermine.org/publications/")
   :credits                      (parse-numbered-properties (get-in web-properties [:project :credit])
                                                            :bluegenes.webproperties.project/credit
                                                            "project.credit")
   :oauth2-providers             (set (get-in web-properties [:oauth2_providers]))
   :idresolver-example           (let [ids (get-in web-properties [:bag :example :identifiers])]
                                   ;; ids can be one of the following:
                                   ;;     {:default "foo bar"
                                   ;;      :protein "baz boz"} ; post im 4.1.0?
                                   ;;     "foo bar"            ; pre  im 4.1.0?
                                   ;; When it's a map, we capitalize the keys
                                   ;; and rename :Default to :Gene; otherwise
                                   ;; we make a map with ids assigned to :Gene.
                                   (if (map? ids)
                                     (-> (reduce-kv (fn [m k v]
                                                      (assoc m (capitalize-kw k) v))
                                                    {} ids)
                                         (rename-keys {:Default :Gene}))
                                     {:Gene ids}))
   :url (merge {:aboutUs "http://intermine.org/about-intermine/"
                :privacyPolicy "http://intermine.org/privacy-policy/"}
               (get-in web-properties [:project :url]))

   :support-email (get-in web-properties [:project :supportEmail])

   :customisation (-> (clojure.walk/postwalk #(case % "true" true "false" false %)
                                             (:bluegenes web-properties))
                      (update-in [:homepage :cta] parse-numbered-properties
                                 :bluegenes.webproperties.customisation.homepage/cta
                                 "bluegenes.homepage.cta"))})

;   :default-query-example        {;;we need json queries to use the endpoint properly
                                  ;;https://github.com/intermine/intermine/issues/1770
                                  ;;note that the default query button won't appear
                                  ;;until we fix this issue
                                ;}

; Fetch web properties
(reg-event-db
 :assets/success-fetch-web-properties
 (fn [db [_ mine-kw web-properties]]
   (let [original-properties (get-in db [:mines mine-kw])
         fetched-properties  (web-properties-to-bluegenes web-properties)]
     (assoc-in db [:mines mine-kw] (merge original-properties fetched-properties)))))

(reg-event-fx
 :assets/fetch-web-properties
 (fn [{db :db} [evt]]
   (let [current-mine (:current-mine db)
         service      (get-in db [:mines current-mine :service])]
     {:im-chan {:chan (fetch/web-properties service)
                :on-success [:assets/success-fetch-web-properties current-mine]
                :on-failure [:assets/failure evt]}})))
