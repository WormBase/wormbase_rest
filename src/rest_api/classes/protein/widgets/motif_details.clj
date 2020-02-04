(ns rest-api.classes.protein.widgets.motif-details
  (:require
   [datomic.api :as d]
   [clojure.string :as str]
   [rest-api.classes.generic-fields :as generic]
   [rest-api.db.main :refer [datomic-homology-conn datomic-conn]]
   [rest-api.formatters.object :as obj :refer [pack-obj]]))

(defn motif-details [p]
 {:data (let [hdb (d/db datomic-homology-conn)
	      db (d/db datomic-conn)
	      plength (->> p :protein/peptide :protein.peptide/length)]
	  (some->> (d/q '[:find ?m ?l
		          :in $ $hdb ?pid
		          :where
  		           [$hdb ?hp :protein/id ?pid]
			   [$hdb ?l :locatable/parent ?hp]
			   [$hdb ?l :homology/motif ?hm]
			   [$hdb ?hm :motif/id ?mid]
			   [$ ?m :motif/id ?mid]]
			  db hdb (:protein/id p))
	   (map (fn [ids]
		 (let [motif (d/entity db (first ids))
                       locatable (d/entity hdb (second ids))]
		  {:source {:id (let [mid (:motif/id motif)]
                                  (if (str/includes? mid ":")
                                    (second (str/split mid #":"))
                                    mid))
                            :db (some->> (:motif/database motif)
                                         (map :motif.database/database)
                                         (map :database/id)
                                         (first))}
		   :start (+ 1 (:locatable/min locatable))
		   :stop (:locatable/max locatable)
		   :feat (pack-obj motif)
		   :desc (or (first (:motif/title motif))
                             (:motif/id motif))
		   :score (:locatable/score locatable)})))))
   :description "The motif details of the protein"})

(def widget
  {:name generic/name-field
   :motif_details motif-details})
