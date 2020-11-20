(ns rest-api.classes.phenotype.widgets.associated-anatomy
  (:require
    [rest-api.classes.generic-fields :as generic]
    [rest-api.formatters.object :as obj :refer  [pack-obj]]
    [pseudoace.utils :as pace-utils]))

(defn anatomy-function [p]
  {:data (let [tags ["anatomy-function"]]
          (not-empty
	   (remove nil?
	    (flatten
	     (for [tag tags]
	      (case tag
	       "anatomy-function"
	       (when-let [afhs (:anatomy-function.phenotype/_phenotype p)]
		(for [afh afhs
		 :let [af (:anatomy-function/_phenotype afh)]]
		 {:reference
		 (when-let [reference (:anatomy-function/reference af)]
		  (pack-obj reference))

		 :af_data (:anatomy-function/id af)

		 :gene
		 (when-let [gene (:anatomy-function.gene/gene
				  (:anatomy-function/gene af))]
		  (pack-obj gene))

		 :assay
		 (when-let [ahs (:anatomy-function/assay af)]
		  (for [ah ahs]
		   (pace-utils/vmap
		    :text (:ao-code/id (:anatomy-function.assay/ao-code ah))
		    :evidence (obj/get-evidence ah))))

                 :phenotype
		 (when-let [ph (:anatomy-function/phenotype af)]
                  (if-let [ev (let [ev-obj (obj/get-evidence ph)]
		              (if-let [remark (:anatomy-function-info/remark ph)]
		  	       (first (conj (dissoc ev-obj :remark) {"Remark" remark}))
			       ev-obj))]
		  {:text (pack-obj p)
		   :evidence ev}
                  (pack-obj p)))

                 :bp_not_inv
 	         (some->> (:anatomy-function/not-involved af)
	  	          (map (fn [afh]
		 	        {:evidence (let [ev (obj/get-evidence-anatomy-function afh)]
                                            (if-let [remark (:remark ev)]
                                             (conj (dissoc ev :remark) {:Remark remark})
                                             ev))
				 :text (when-let [at (:anatomy-function.not-involved/anatomy-term afh)]
		                        (pack-obj at))})))

                 :bp_inv
		 (some->> (:anatomy-function/involved af)
			  (map (fn [afh]
			        {:evidence (let [ev (obj/get-evidence-anatomy-function afh)]
                                            (if-let [remark (:remark ev)]
                                             (conj (dissoc ev :remark) {:Remark remark})
                                             ev))
  			         :text (when-let [at (:anatomy-function.involved/anatomy-term afh)]
		                        (pack-obj at))})))}))))))))
   :description "anatomy_functions associatated with this anatomy_term"})

(def widget
  {:associated_anatomy anatomy-function
   :name generic/name-field})
