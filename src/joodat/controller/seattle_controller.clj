(ns joodat.controller.seattle-controller
  (:use
    [compojure.core]
    [clojure.pprint]
    [ring.util.response :only (redirect)]
    [joodo.views :only (render-template render-html)]
    [datomic.api :only [q db] :as d]
    [joodat.model.seattle]   
  )   
)

(def uri "datomic:mem://seattle")


(defn create-db
  "Creates an empty database and loads schema and data"
  [uri]

    (d/delete-database uri)
    (d/create-database uri)
    (let [conn (d/connect uri) 
       schema-tx (read-string (slurp "public/resources/seattle-schema.dtm"))
       data-tx   (read-string (slurp "public/resources/seattle-data0.dtm"))
       ]
      (d/transact conn schema-tx)
      (d/transact conn data-tx)
 
 ))

(defn initdb
 "calls create-db and renders the results"
 [uri]

 (if (create-db uri)
   (let
     [conn (d/connect uri)]
     (render-template "seattle/initdb" :regions-count (count (regions-query conn))
                                       :districts-count (count (districts-query conn))
                                       :neighborhoods-count (count (neighborhoods-query conn))
                                       :communities-count (count (communities-query conn)))
   )
   (render-template "seattle/fail")))

(defn community-names
 "returns an array with community names, urls"
 [conn db]
 (let [results (communities-query conn)]
  (apply str (map #(str "<p><a href=  " (second %)  ">" (first %) "</a> " "</p>") (sort results)))
 )
)

(defn district-names
 "returns a html string with district names and link to neighborhoods"
 [conn db]
 (let [results (districts-query conn)]
  (apply str (map #(str "<p>" "<a href=/seattle/district-neighborhoods/"   (clojure.string/replace (first %) " " "%20") ">" (first %) "</p>") (sort results)))
 )
)

(defn neighborhood-names
 "returns aneighborhood names"
 [conn db]
 (let [results (neighborhoods-query conn)]
  (apply str (map #(str "<p>" "<a href=/seattle/neighborhood-communities/"   (clojure.string/replace (first %) " " "%20") ">" (first %)  "</a>"  "</p>") (sort results)))
 )
)

(defn district-neighborhoods-names
 "returns html with neighborhood names for given district"
 [conn db district]
 (let [results (district-neighborhoods-query conn district)]
  (apply str (map #(str "<p>" "<a href=/seattle/neighborhood-communities/"   (clojure.string/replace (first %) " " "%20") ">" (first %)  "</a>"  "</p>") (sort results)))
 )
)

(defn neighborhood-community-names
 "returns html with community links for given neighborhood"
 [conn db neighborhood]
 (let [results (neighborhood-communities-query conn neighborhood)]
  (apply str (map #(str "<p>" "<a href=" (second %) ">" (first %) "</a>" "</p>") (sort results)))
 )
)


(defn render-communities
 "fetches all communities and renders the links"
 [uri]

 (try
  (let [conn (d/connect uri) db (db conn)]
   (render-template "seattle/communities" :community (community-names conn db))
  )
  (catch Exception e
      nil)
 )
)
  
(defn render-districts
 "fetches all districts and renders the names"
 [uri]

 (try
  (let [conn (d/connect uri) db (db conn)]
   (render-template "seattle/districts" :districts (district-names conn db))
  )
  (catch Exception e
      nil)

 )
)


(defn render-neighborhoods
 "renders neighborhood names"
 [uri]
 
 (try
  (let [conn (d/connect uri) db (db conn)]
  (render-template "seattle/neighborhoods" :neighborhoods (neighborhood-names conn db))
  )
  (catch Exception e
      nil)
 )
)

(defn render-district-neighborhoods
 "renders neighborhood links for given district"
 [uri district]

 (let [conn (d/connect uri) db (db conn)]
  (render-template "seattle/district-neighborhoods" :neighborhoods (district-neighborhoods-names conn db district) :district district)
 )
)

(defn render-neighborhood-communities
 "renders community links for given neighborhood"
 [uri neighborhood]

 (let [conn (d/connect uri) db (db conn)]
  (render-template "seattle/neighborhood-communities" :communities (neighborhood-community-names conn db neighborhood) :neighborhood neighborhood)
 )
)

(defroutes seattle-controller
  (GET "/seattle" [] (redirect "seattle/index"))
  (GET "/seattle/index" [] (render-template "seattle/index"))
  (GET "/seattle/initdb" [] (initdb uri))
  (GET "/seattle/districts" [] (render-districts uri))
  (GET "/seattle/neighborhoods" [] (render-neighborhoods uri))
  (GET "/seattle/communities" [] (render-communities uri))
  (GET "/seattle/district-neighborhoods/:id" [id] (render-district-neighborhoods uri id))
  (GET "/seattle/neighborhood-communities/:id" [id] (render-neighborhood-communities uri id))

  (context "/seattle" []
    (GET "/test" [] {:status 200 :body "PASS"})))
