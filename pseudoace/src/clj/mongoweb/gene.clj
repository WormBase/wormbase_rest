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

(def allele-table-script
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
          $jq('#table_alleles_by').dataTable({
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

          $jq('#table_polymorphisms_by').dataTable({
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

          $jq('#table_genetics_by').dataTable({
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
           

(defn nonsense [change]
  (or (seq (:amber-uag change))
      (seq (:ochre-uaa change))
      (seq (:opal-uga change))
      (seq (:ochre-uaa-or-opal-uga change))
      (seq (:amber-uag-or-ochre-uaa change))
      (seq (:amber-uag-or-opal-uga change))))

(defn mgene-genetics-allele-table [db id]
  (let [alleles (mc/find-maps db "Variation" {:gene.gene id :allele true})]
    [:table.display {:id "table_alleles_by"}
     [:thead
      [:th "Allele"]
      [:th "Molecular change"]
      [:th "Locations"]
      [:th "Protein effects"]
      [:th "Protein change"]
      [:th "Amino acid position"]
      [:th "Isoform"]
      [:th "# of Phenotypes"]
      [:th "Method"]
      [:th "Strain"]]
     [:tbody
      (for [allele alleles]
        [:tr
         [:td [:a.variation-link {:href (str "/view/variation/" (:_id allele))}
               (:public_name allele)]]

         [:td
          (cond
           (:substitution allele)
           "Substitution"

           (:insertion allele)
           "Insertion"

           (:deletion allele)
           "Deletion"

           (:inversion allele)
           "Inversion"

           (:tandem_duplication allele)
           "Tandem_duplication"

           :default
           "Not curated")]
         
         [:td
          (let [changes (set (mapcat keys (concat (:predicted_cds allele)
                                                  (:transcript allele))))]
            (str/join ", " (filter
                            identity
                            (map {:intron       "Intron"
                                  :coding_exon  "Coding exon"
                                  :utr_5        "5' UTR"
                                  :utr_3        "3' UTR"}
                                 changes))))]
         
         [:td
          (let [changes (set (mapcat keys (:predicted_cds allele)))]
            (str/join ", " (set (filter
                                 identity
                                 (map {:missense "Missense"
                                       :amber-uag "Nonsense"
                                       :ochre-uaa "Nonsense"
                                       :opal-uga "Nonsense"
                                       :ochre-uaa-or-opal-uga "Nonsense"
                                       :amber-uag-or-ochre-uaa "Nonsense"
                                       :amber-uag-or-opal-uga "Nonsense"
                                       :frameshift "Frameshift"
                                       :silent "Silent"}
                                      changes)))))]

         
         [:td
          (->> (mapcat
                (fn [cc]
                  (list
                   (if-let [n (first (:missense cc))]
                     [:span (:text n)])
                   (if-let [n (first (nonsense cc))]
                     [:span ((first (filter #(= (name %) "text") (keys n))) n)])))
                (:predicted_cds allele))
               (filter identity)
               (interpose [:br]))]

         [:td
          (interpose [:br]
           (for [cc (:predicted_cds allele)
                 :when (:missense cc)]
             [:span (str (:int (first (:missense cc))))]))]
              
         
         [:td
          (interpose [:br]
            (for [cc (:predicted_cds allele)
                  :when (or (:missense cc)
                            (nonsense cc))
                  :let [cds (:cds cc)]]
              [:span
               [:a.cds-link {:href (str "/view/cds/" cds)} cds]]))]
                       
         [:td
          (count (:phenotype allele))]
         
         [:td
          (:method allele)]
         
         [:td
          (interpose [:br]
            (for [vs (:strain allele)]
              [:a.strain-link {:href (str "/view/strain/" (:strain vs))}
               (:strain vs)]))]
                     
         ])]]))
      

(defn mgene-genetics-poly-table [db id]
  (let [alleles (mc/find-maps db "Variation" {:gene.gene id
                                              :allele {$exists false}})]
    [:table.display {:id "table_polymorphisms_by"}
      [:thead
       [:tr
        [:th "Polymorphism"]
        [:th "Type"]
        [:th "Molecular change"]
        [:th "Locations"]
        [:th "Protein effects"]
        [:th "Protein change"]
        [:th "Amino acid position"]
        [:th "# of Phenotypes"]
        [:th "Strain"]]
       [:tbody
        (for [allele alleles]
          [:tr
           [:td
            [:a.variation-link {:href (str "/view/variation/" (:_id allele))}
             (:public_name allele)]]

           [:td
            (let [types (or (seq (filter identity
                                         [(if (:snp allele) "SNP")
                                          (if (:predicted-snp allele) "Predicted SNP")]))
                            ["unknown"])]
              (str/join ", " types))]

           [:td
            (cond
             (:substitution allele)
             "Substitution"

             (:insertion allele)
             "Insertion"
             
             (:deletion allele)
             "Deletion"
             
             (:inversion allele)
             "Inversion"

             (:tandem_duplication allele)
             "Tandem_duplication"

             :default
             "Not curated")]
           
           [:td
            (let [changes (set (mapcat keys (concat (:predicted_cds allele)
                                                    (:transcript allele))))]
              (str/join ", " (filter
                              identity
                              (map {:intron       "Intron"
                                    :coding_exon  "Coding exon"
                                    :utr_5        "5' UTR"
                                    :utr_3        "3' UTR"}
                                   changes))))]
           
           [:td
            (let [changes (set (mapcat keys (:predicted_cds allele)))]
              (str/join ", " (set (filter
                                   identity
                                   (map {:missense "Missense"
                                         :amber-uag "Nonsense"
                                         :ochre-uaa "Nonsense"
                                         :opal-uga "Nonsense"
                                         :ochre-uaa-or-opal-uga "Nonsense"
                                         :amber-uag-or-ochre-uaa "Nonsense"
                                         :amber-uag-or-opal-uga "Nonsense"
                                         :frameshift "Frameshift"
                                         :silent "Silent"}
                                        changes)))))]

           [:td
            (->> (mapcat
                  (fn [cc]
                    (list
                     (if-let [n (first (:missense cc))]
                       [:span (:text n)])
                     (if-let [n (first (nonsense cc))]
                       [:span ((first (filter #(= (name %) "text") (keys n))) n)])))
                  (:predicted_cds allele))
                 (filter identity)
                 (interpose [:br]))]

           [:td
            (interpose [:br]
              (for [cc (:predicted_cds allele)
                    :when (:missense cc)]
                [:span (str (:int (first (:missense cc))))]))]

           [:td
            (count (:phenotype allele))]
           
         
           [:td
            (let [strain-list
                    (interpose [:br]
                      (for [vs (:strain allele)]
                        [:a.strain-link {:href (str "/view/strain/" (:strain vs))}
                         (:strain vs)]))]
              (if (> (count (:strain allele)) 5)
                (list
                 [:div.toggle
                  [:span.ui-icon.ui-icon-triangle-1-e {:style "float: left"}]
                  " " (count (:strain allele)) " results "]
                 [:div.returned {:style "display: none"}
                  strain-list])
                strain-list))]
              
           
           ])]]]))

(defn- is-cgc? [strain]
  (some #(= (:laboratory %) "CGC")
        (:location strain)))

(defn- strain-list [strains]
  (interpose ", "
    (for [strain strains]          
      [:a.strain-link {:href (str "/view/strain/" (:_id strain))}
       (:_id strain)])))

(defn mgene-genetics-strain-table [db id]
  (let [gene    (mc/find-map-by-id db "Gene" id [:strain :public_name])
        strains (if (:strain gene)
                  (mc/find-maps db "Strain" {:_id {$in (:strain gene)}}))
        strain-in-transgene?
           (comp
            (->> (if (:strain gene)
                   (mc/find-maps db "Transgene" {:strain {$in (:strain gene)}}))
                 (mapcat :strain)
                 (set))
            :_id)]
    (list
     [:table.venn {:cellspacing "0" :cellpadding "5"}
      [:tbody
       [:tr.venn-a
        [:th {:colspan "2"}
         "Carrying "
         [:a.gene-link {:href (str "/view/gene/" id)}
          [:span.locus (:public_name gene)]]
         " alone"]]
       [:tr
        [:th.venn-a]
        [:th.venn-ab]
        [:th.venn-b "Available from the CGC"]
        [:th "Other strains"]]
       [:tr.venn-data
        [:td.venn-a  (strain-list (filter #(and (not (strain-in-transgene? %))
                                                (= (mc/count db "Gene" {:strain (:_id %)}) 1)
                                                (not (is-cgc? %)))
                                          strains))]
        [:td.venn-ab (strain-list (filter #(and (not (strain-in-transgene? %))
                                                (= (mc/count db "Gene" {:strain (:_id %)}) 1)
                                                (is-cgc? %))
                                          strains))]
        [:td.venn-b  (strain-list (filter #(and (or (strain-in-transgene? %)
                                                    (not= (mc/count db "Gene" {:strain (:_id %)}) 1))
                                                (is-cgc? %))
                                          strains))]
        [:td         (strain-list (filter #(and (or (strain-in-transgene? %)
                                                    (not= (mc/count db "Gene" {:strain (:_id %)}) 1))
                                                (not (is-cgc? %)))
                                          strains))]]
       
       [:tr
        [:td]
        [:td.venn-b {:colspan "2"}]]]]
     
     [:table#table_genetics_by.display {:cellpadding "0" :cellspacing "0" :border "0"}
      [:thead
       [:tr
        [:th "Strain"]
        [:th "Genotype"]
        [:th "Available" [:br] " from CGC?"]]]
      [:tbody
       (for [strain strains]
         [:tr
          [:td
           [:a.strain-link {:href (str "/view/strain/" (:_id strain))}
            (:_id strain)]]
          
          [:td
           (first (:genotype strain))]
          
          [:td
           (if (is-cgc? strain)
             [:a {:href (str "https://cgcdb.msi.umn.edu/search.php?st=" (:_id strain))} "yes"]
             "no")]
          

          ])]])))



        

(defn mgene-genetics-widget [db id]
  (let [gene (mc/find-map-by-id db "Gene" id [:public_name :reference_allele])]
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
           (if-let [ref-id (->> (:reference_allele gene)
                                (first)
                                (:variation))]
             (let [ref (mc/find-map-by-id db "Variation" ref-id [:public_name])]
               [:div.field
                [:div.field-title "Reference allele"]
                [:div.field-content
                 [:a.variation-link {:href (str "/view/variation/" ref-id)}
                  [:span.var (:public_name ref)]]]]))
           
           [:div.field
            [:div.field-title "Alleles:"]
            [:div.field-content
             (mgene-genetics-allele-table db id)
             [:script allele-table-script]]]

           [:div.field
            [:div.field-title "Polymorphisms & Natural variants:"]
            [:div.field-content 
             (mgene-genetics-poly-table db id)]]

           [:div.field
            [:div.field-title "Strains:"]
            [:div.field-content
             (mgene-genetics-strain-table db id)]]])]]]])))

(defroutes routes
  (GET "/gene-phenotypes/:id" {params :params}
       (mgene-phenotypes-widget mdb (:id params)))
  (GET "/gene-genetics/:id" {params :params}
       (mgene-genetics-widget mdb (:id params)))
  (route/files "/" {:root "resources/public"}))

(def app
  (-> routes
      wrap-params
      wrap-stacktrace
      wrap-session))

(defonce server (run-jetty #'app {:port 8125
                                  :join? false}))
                                  

