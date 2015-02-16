(ns mongoweb.gene
  (:use hiccup.core
        ring.middleware.stacktrace
        ring.middleware.params
        ring.middleware.session)
  (:require [monger.core :as mg]
            [monger.collection :as mc :refer (find-maps)]
            [monger.operators :refer :all]
            [clojure.string :as str]
            [ring.adapter.jetty :refer (run-jetty)]
            [compojure.core :refer (defroutes GET)]
            [compojure.route :as route]
            [compojure.handler :as handler]
            [ring.util.response :refer [file-response]]))

(def mcon (mg/connect))
(def mdb (mg/get-db mcon "wb244-mongo1"))

(def table-script
  "window.addEventListener('load', function(ev) {
        console.log('loaded...')
        WB.getPlugin('dataTables', function(){
        $jq.extend( $jq.fn.dataTableExt.oSort, {
          'scientific-pre': function ( a ) {
            return parseFloat(a);
          },
          'scientific-asc': function ( a, b ) {
            return ((a < b) ? -1 : ((a > b) ? 1 : 0));
          },
          'scientific-desc': function ( a, b ) {
            return ((a < b) ? 1 : ((a > b) ? -1 : 0));
          }
          });
        WB.getPlugin('tabletools', function(){
          setTimeout(function(){
          $jq('#table_phenotype_observed_by').dataTable({
            'bPaginate'        : true,
            'bLengthChange'    : true,
            'sPaginationType'  : 'full_numbers',
            'bFilter'          : true,
            'bInfo'            : true,
            'fnInitComplete'   : function(){ setTimeout(function(){ WB.resize();}, 700); },
            'sDom': '<\"wb_table_wrapper table_paginate\"Tflrtip>',
            'oTableTools': {
              'sSwfPath': '/js/jquery/plugins/tabletools/media/swf/copy_csv_xls_pdf.swf',
              'aButtons': [
                {
                  'sExtends': 'collection',
                  'sButtonText': 'Save table',
                  'aButtons': [ 'copy', 'csv', 'pdf', 'print' ]
                }
              ]
            },  });


           $jq('#table_phenotype_not_observed').dataTable({
            'bPaginate'        : true,
            'bLengthChange'    : true,
            'sPaginationType'  : 'full_numbers',
            'bFilter'          : true,
            'bInfo'            : true,
            'fnInitComplete'   : function(){ setTimeout(function(){ WB.resize();}, 700); },
            'sDom': '<\"wb_table_wrapper table_paginate\"Tflrtip>',
            'oTableTools': {
              'sSwfPath': '/js/jquery/plugins/tabletools/media/swf/copy_csv_xls_pdf.swf',
              'aButtons': [
                {
                  'sExtends': 'collection',
                  'sButtonText': 'Save table',
                  'aButtons': [ 'copy', 'csv', 'pdf', 'print' ]
                }
              ]
            },  });



          }, 1);
        });
	      });
              });")

(defn gene-var-pheno [db id pheno-key]
  (->> (mc/find-maps db "Variation" {:gene.gene id} [:_id pheno-key])
       (mapcat 
        (fn [{vari :_id pheno-holders pheno-key}]
          (for [ph pheno-holders]
            (assoc ph :variation vari))))
       (group-by :phenotype)))

(defn gene-rnai-phenos [db id pheno-key]
  (->> (mc/find-maps db "RNAi" {:gene.gene id} [:_id pheno-key])
       (mapcat
        (fn [{rnai :_id pheno-holders pheno-key}]
          (for [ph pheno-holders]
            (assoc ph :rnai rnai))))
       (group-by :phenotype)))

(defn- gene-phenotypes-table [db id not?]
  (let [var-phenos  (gene-var-pheno db id 
                                    (if not?
                                      :phenotype_not_observed
                                      :phenotype))
        var-ids     (seq (mapcat #(map :variation %) (vals var-phenos)))

        vars        (if var-ids
                      (->> (mc/find-maps db "Variation" {:_id {$in var-ids}})
                           (map (juxt :_id identity))
                           (into {})))
        rnai-phenos (gene-rnai-phenos db id
                                      (if not?
                                        :phenotype_not_observed
                                        :phenotype))
        rnai-ids    (seq (mapcat #(map :rnai %) (vals rnai-phenos)))
        rnais       (if rnai-ids
                      (->> (mc/find-maps db "RNAi" {:_id {$in rnai-ids}})
                           (map (juxt :_id identity))
                           (into {})))
        pheno-ids   (seq (set (concat (keys var-phenos) (keys rnai-phenos))))
        phenos      (if pheno-ids
                      (mc/find-maps db "Phenotype" {:_id {$in pheno-ids}} [:primary_name]))]
    (html
     [:table.display {:id (if not?
                            "table_phenotype_not_observed"
                            "table_phenotype_observed_by")}
      [:thead
       [:tr
        [:th "Phenotype"]
        [:th "Supporting evidence"]]]
      [:tbody
       (for [pheno phenos]
         [:tr
          [:td
           [:a {:href (str "/view/phenotype/" (:_id pheno))}
            (:text (:primary_name pheno))]]
          [:td
           (if-let [vp (seq (var-phenos (:_id pheno)))]
             (list
              "Allele :"
              (for [v    vp
                    :let [var (vars (:variation v))]]
                [:div.evidence.result
                 [:span {:style (if (= (:seqstatus var) "sequenced")
                                  "font-weight: bold")}
                  [:a.variation-link {:href (str "/view/variation/" (:_id var))}
                   [:span.var (:public_name var)]]]
                 [:div#evidence_table_phenotype_observed_by.ev.ui-helper-hidden
                  (for [person-id (:person_evidence v)
                        :let [person (mc/find-map-by-id db "Person" person-id)]]
                    (list
                     [:b "Person evidence"]
                     ": "
                     [:a.person-link {:href (str "/view/person/" person-id)}
                      (:standard_name person)]
                     [:br]))
                  (for [person-id (:curator_confirmed v)
                        :let [person (mc/find-map-by-id db "Person" person-id)]]
                    (list
                     [:b "Curator"]
                     ": "
                     [:a.person-link {:href (str "/view/person/" person-id)}
                      (:standard_name person)]
                     [:br]))
                  (for [remark (:remark v)]
                    (list
                     [:b "Remark"]
                     ": "
                     (:text remark)))]
                 [:div.ev-more
                  [:div.v.ev-more-line]
                  [:div.ev-more-text [:span "details"]]
                  [:div.v.ui-icon.ui-icon-triangle-1-s]]])))

           (if-let [rp (seq (rnai-phenos (:_id pheno)))]
             (list
              "RNAi :"
              (for [r    rp
                    :let [rnai (rnais (:rnai r))]]
                [:div.evidence.result
                 [:span
                  [:a.rnai-link {:href (str "/view/rnai/" (:_id rnai))}
                   (:_id rnai)]]
                 [:div#evidence_table_phenotype_observed_by.ev.ui-helper-hidden
                  (if-let [paper-id (:reference rnai)]
                    (let [paper (mc/find-map-by-id db "Paper" paper-id)]
                      (list
                       [:b "Paper"]
                       ": "
                       [:a.paper-link {:href (str "/view/paper/" (:_id paper))}
                        (:brief_citation paper)]
                       [:br])))

                  (if-let [strain-id (:strain rnai)]
                    (list
                     [:b "Strain"]
                     ": "
                     [:a.strain-link {:href (str "/view/strain/" strain-id)}
                      strain-id]
                     [:br]))

                  (if-let [genotype (:genotype rnai)]
                    (list
                     [:b "Genotype"]
                     ": "
                     genotype
                     [:br]))

                  (for [remark (:remark r)]
                    (list
                     [:b "Remark"]
                     ": "
                     (:text remark)))]

                 [:div.ev-more
                  [:div.v.ev-more-line]
                  [:div.ev-more-text [:span "details"]]
                  [:div.v.ui-icon.ui-icon-triangle-1-s]]])))

             ]])]])))
                    
                
    
    


(defn mgene-phenotypes-widget [db id]
  (let [gene (mc/find-map-by-id db "Gene" id [:public_name])]
    (html
     [:html
      [:head
      [:html
       [:head
        [:link {:type "text/css"
                :href "http://www.wormbase.org/css/jquery-ui.min.css"
                :rel "stylesheet"}]
        [:link {:type "text/css"
                :href "http://www.wormbase.org/css/main.min.css"
                :rel "stylesheet"}]

        [:script {:src "/js/jquery-1.9.1.min.js"}]
        [:script {:src "/js/jquery-ui-1.10.1.custom.min.js"}]
        [:script {:src "/js/jquery/plugins/dataTables/media/js/jquery.dataTables.min.js"}]
        [:script {:src "/js/jquery/plugins/jquery.placeholder.min.js"}]
        [:script {:src "/js/wormbase.js"}]]
       [:body
        [:div#header {:data-page "{'history': '0'}"}]
        (if gene
          [:div#content
           [:div.field-title "Phenotypes:"]
           [:div.field-content
            [:p
             [:i "The following phenotypes have been observed in " (:public_name gene)]
             (gene-phenotypes-table db id false)]
            [:br]
            [:div.toggle
             [:i "The following phenotypes have been reported as NOT observed in " (:public_name gene)]
             (gene-phenotypes-table db id true)]
            [:script table-script]]]
          [:div#content
           [:p "Gene " id " not found"]])]]]])))
           

(defroutes routes
  (GET "/gene-phenotypes/:id" {params :params}
       (mgene-phenotypes-widget mdb (:id params)))
  (route/files "/" {:root "resources/public"}))

(def app
  (-> routes
      wrap-params
      wrap-stacktrace
      wrap-session))

(defonce server (run-jetty #'app {:port 8125
                                  :join? false}))
                                  

