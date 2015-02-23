(ns web.widgets
  (:use hiccup.core)
  (:require [datomic.api :as d :refer (db history q touch entity)]
            [clojure.string :as str]))

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

(def q-gene-rnai-pheno
  '[:find ?pheno (distinct ?ph)
    :in $ ?gid
    :where [?g :gene/id ?gid]
           [?gh :rnai.gene/gene ?g]
           [?rnai :rnai/gene ?gh]
           [?rnai :rnai/phenotype ?ph]
           [?ph :rnai.phenotype/phenotype ?pheno]])

(def q-gene-rnai-not-pheno
  '[:find ?pheno (distinct ?ph)
    :in $ ?gid
    :where [?g :gene/id ?gid]
           [?gh :rnai.gene/gene ?g]
           [?rnai :rnai/gene ?gh]
           [?rnai :rnai/phenotype-not-observed ?ph]
    [?ph :rnai.phenotype-not-observed/phenotype ?pheno]])

(def q-gene-var-pheno
   '[:find ?pheno (distinct ?ph)
     :in $ ?gid
     :where [?g :gene/id ?gid]
            [?gh :variation.gene/gene ?g]
            [?var :variation/gene ?gh]
            [?var :variation/phenotype ?ph]
            [?ph :variation.phenotype/phenotype ?pheno]])
 
(def q-gene-var-not-pheno
   '[:find ?pheno (distinct ?ph)
     :in $ ?gid
     :where [?g :gene/id ?gid]
            [?gh :variation.gene/gene ?g]
            [?var :variation/gene ?gh]
            [?var :variation/phenotype-not-observed ?ph]
            [?ph :variation.phenotype-not-observed/phenotype ?pheno]])

(defn gene-phenotypes-table [db id not?]
  (let [var-phenos (into {} (q (if not?
                                 q-gene-var-not-pheno
                                 q-gene-var-pheno)
                               db id))
        rnai-phenos (into {} (q (if not?
                                  q-gene-rnai-not-pheno
                                  q-gene-rnai-pheno)
                                db id))
        phenos (set (concat (keys var-phenos)
                            (keys rnai-phenos)))]
    (html
     [:table.display {:id (if not?
                            "table_phenotype_not_observed"
                            "table_phenotype_observed_by")}
      [:thead
       [:tr
        [:th "Phenotype"]
        [:th "Supporting evidence"]]]
      [:tbody
       (for [pid phenos :let [pheno (entity db pid)]]
         [:tr
          [:td
           [:a {:href (str "/view/phenotype/" (:phenotype/id pheno))}
            (:phenotype.primary-name/text (:phenotype/primary-name pheno))]]
          [:td
           (if-let [vp (seq (var-phenos pid))]
             (html
              "Allele :"
              (for [v vp
                    :let [holder (entity db v)
                          var ((if not?
                                 :variation/_phenotype-not-observed
                                 :variation/_phenotype)
                               holder)]]
                [:div.evidence.result
                 [:span {:style (if (= (:variation/seqstatus var) :variation.seqstatus/sequenced) "font-weight:bold")}
                  [:a.variation-link {:href (str "/view/variation/" (:variation/id var))}
                   [:span.var (:variation/public-name var)]]]
                 [:div#evidence_table_phenotype_observed_by.ev.ui-helper-hidden
                  (for [person (:phenotype-info/person-evidence holder)]
                    (html
                     [:b "Person evidence"]
                     ": "
                     [:a.person-link {:href (str "/view/person/" (:person/id person))}
                      (:person/standard-name person)]
                     [:br]))
                  (for [curator (:phenotype-info/curator-confirmed holder)]
                    (html
                     [:b "Curator"]
                     ": "
                     [:a.person-link {:href (str "/view/person/" (:person/id curator))}
                      (:person/standard-name curator)]
                     [:br]))
                  (for [remark (:phenotype-info/remark holder)]
                    (html
                     [:b "Remark"]
                     ": "
                     (:phenotype-info.remark/text remark)))]
                 [:div.ev-more
                  [:div.v.ev-more-line]
                  [:div.ev-more-text [:span "details"]]
                  [:div.v.ui-icon.ui-icon-triangle-1-s]]])))
           (if-let [rp (seq (rnai-phenos pid))]
             (html
              "RNAi :"
              (for [r rp
                    :let [holder (entity db r)
                          rnai ((if not?
                                  :rnai/_phenotype-not-observed
                                  :rnai/_phenotype)
                                holder)]]
                [:div.evidence.result
                 [:span
                  [:a.rnai-link {:href (str "/view/rnai/" (:rnai/id rnai))}
                   (:rnai/id rnai)]]
                 [:div#evidence_table_phenotype_observed_by.ev.ui-helper-hidden
                  (if-let [paper (:rnai/reference rnai)]    ; Reference is unique on RNAi.  How odd..
                    (html
                     [:b "Paper"]
                     ": "
                     [:a.paper-link {:href (str "/view/paper/" (:paper/id paper))}
                      (:paper/brief-citation paper)]
                     [:br]))

                  (if-let [strain (:rnai/strain rnai)]
                    (html
                     [:b "Strain"]
                     ": "
                     [:a.strain-link {:href (str "/view/strain/" (:strain/id strain))}
                      (:strain/id strain)]
                     [:br]))

                  (if-let [genotype (:rnai/genotype rnai)]
                    (html
                     [:b "Genotype"]
                     ": "
                     genotype
                     [:br]))
                     
                  (for [remark (:phenotype-info/remark holder)]
                    (html
                     [:b "Remark"]
                     ": "
                     (:phenotype-info.remark/text remark)))]

                 [:div.ev-more
                  [:div.v.ev-more-line]
                  [:div.ev-more-text [:span "details"]]
                  [:div.v.ui-icon.ui-icon-triangle-1-s]]])))

           ]])]])))
        

(defn gene-phenotypes-widget [db id]
  (let [gids (q '[:find ?g :in $ ?gid :where [?g :gene/id ?gid]] db id)]
    (if-let [[gid] (first gids)]
     (let [g (entity db gid)
           gene-name (:gene/public-name g)]
      (html
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
        [:div#content
         [:div.field-title "Phenotypes:"]
         [:div.field-content
          [:p
           [:i "The following phenotypes have been observed in " gene-name]]
          (gene-phenotypes-table db id false)
          [:br]
          [:div.toggle
           [:i "The following phenotypes have been reported as NOT observed in " gene-name]
           (gene-phenotypes-table db id true)]
          [:script table-script]]]]])))))

(defn nonsense [change]
  (or (seq (:molecular-change/amber-uag change))
      (seq (:molecular-change/ochre-uaa change))
      (seq (:molecular-change/opal-uga change))
      (seq (:molecular-change/ochre-uaa-or-opal-uga change))
      (seq (:molecular-change/amber-uag-or-ochre-uaa change))
      (seq (:molecular-change/amber-uag-or-opal-uga change))))

                                         

(defn gene-genetics-alleles-table [db id alleles]
  (let [alleles (filter :variation/allele alleles)]
    (list
     [:table.display {:id "table_alleles_by"}
      [:thead
       [:tr
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
           [:td [:a.variation-link {:href (str "/view/variation/" (:variation/id allele))}
                 (:variation/public-name allele)]]

           [:td
            (cond
             (:variation/substitution allele)
             "Substitution"

             (:variation/insertion allele)
             "Insertion"

             (:variation/deletion allele)
             "Deletion"

             (:variation/inversion allele)
             "Inversion"

             (:variation/tandem-duplication allele)
             "Tandem_duplication"

             :default
             "Not curated")]

           [:td
            (let [changes (set (mapcat keys (concat (:variation/predicted-cds allele)
                                                    (:variation/transcript allele))))]
              (str/join ", " (filter
                              identity
                              (map {:molecular-change/intron "Intron"
                                    :molecular-change/coding-exon "Coding exon"
                                    :molecular-change/utr-5 "5' UTR"
                                    :molecular-change/utr-3 "3' UTR"}
                                   changes))))]
            

           [:td
            (let [changes (set (mapcat keys (:variation/predicted-cds allele)))]
              (str/join ", " (set (filter
                                   identity
                                   (map {:molecular-change/missense "Missense"
                                         :molecular-change/amber-uag "Nonsense"
                                         :molecular-change/ochre-uaa "Nonsense"
                                         :molecular-change/opal-uga "Nonsense"
                                         :molecular-change/ochre-uaa-or-opal-uga "Nonsense"
                                         :molecular-change/amber-uag-or-ochre-uaa "Nonsense"
                                         :molecular-change/amber-uag-or-opal-uga "Nonsense"
                                         :molecular-change/frameshift "Frameshift"
                                         :molecular-change/silent "Silent"}
                                        changes)))))]

           [:td
            (for [cc (:variation/predicted-cds allele)]
              (list
               (if-let [n (first (:molecular-change/missense cc))]
                 [:span (:molecular-change.missense/text n)])
               (if-let [n (first (nonsense cc))]
                 [:span ((first (filter #(= (name %) "text") (keys n))) n)])))]

           [:td
            (for [cc (:variation/predicted-cds allele)]
              (list
               (if-let [n (first (:molecular-change/missense cc))]
                 [:span (:molecular-change.missense/int n)])))]
            

           [:td
            (for [cc (:variation/predicted-cds allele)
                  :when (or (:molecular-change/missense cc)
                            (nonsense cc))]
              (let [cds (:cds/id (:variation.predicted-cds/cds cc))]
                [:span
                 [:a.cds-link {:href (str "/view/cds/" cds)} cds]]))]

           [:td
            (count (:variation/phenotype allele))]

           [:td
            (:method/id (:variation/method allele))]

           [:td
            (let [strain-list
                  (interpose [:br]
                             (for [vs (:variation/strain allele)
                                   :let [strain (:variation.strain/strain vs)
                                         sid (:strain/id strain)]]
                               [:a.strain-link {:href (str "/view/strain/" sid)}
                                sid]))]
              (if (> (count (:variation/strain allele)) 5)
                (list
                 [:div.toggle
                  [:span.ui-icon.ui-icon-triangle-1-e {:style "float: left"}]
                  " " (count (:strain allele)) " results "]
                 [:div.returned {:style "display: none"}
                  strain-list])))]

           ])]]])))
        
(defn gene-genetics-poly-table [db id alleles]
  (let [alleles (filter (complement :variation/allele) alleles)]
    (list
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
           [:td [:a.variation-link {:href (str "/view/variation/" (:variation/id allele))}
                 (:variation/public-name allele)]]

           [:td
            (let [types (or (seq (filter identity
                                         [(if (:variation/snp allele) "SNP")
                                          (if (:variation/predicted-snp allele) "Predicted SNP")]))
                            ["unknown"])]
              (str/join ", " types))]
           
           [:td
            (cond
             (:variation/substitution allele)
             "Substitution"

             (:variation/insertion allele)
             "Insertion"

             (:variation/deletion allele)
             "Deletion"

             (:variation/inversion allele)
             "Inversion"

             (:variation/tandem-duplication allele)
             "Tandem_duplication"

             :default
             "Not curated")]

           [:td
            (let [changes (set (mapcat keys (concat (:variation/predicted-cds allele)
                                                    (:variation/transcript allele))))]
              (str/join ", " (filter
                              identity
                              (map {:molecular-change/intron "Intron"
                                    :molecular-change/coding-exon "Coding exon"
                                    :molecular-change/utr-5 "5' UTR"
                                    :molecular-change/utr-3 "3' UTR"}
                                   changes))))]
            

           [:td
            (let [changes (set (mapcat keys (:variation/predicted-cds allele)))]
              (str/join ", " (set (filter
                                   identity
                                   (map {:molecular-change/missense "Missense"
                                         :molecular-change/amber-uag "Nonsense"
                                         :molecular-change/ochre-uaa "Nonsense"
                                         :molecular-change/opal-uga "Nonsense"
                                         :molecular-change/ochre-uaa-or-opal-uga "Nonsense"
                                         :molecular-change/amber-uag-or-ochre-uaa "Nonsense"
                                         :molecular-change/amber-uag-or-opal-uga "Nonsense"
                                         :molecular-change/frameshift "Frameshift"
                                         :molecular-change/silent "Silent"}
                                        changes)))))]

           [:td
            (for [cc (:variation/predicted-cds allele)]
              (list
               (if-let [n (first (:molecular-change/missense cc))]
                 [:span (:molecular-change.missense/text n)])
               (if-let [n (first (nonsense cc))]
                 [:span ((first (filter #(= (name %) "text") (keys n))) n)])))]

           [:td
            (for [cc (:variation/predicted-cds allele)]
              (list
               (if-let [n (first (:molecular-change/missense cc))]
                 [:span (:molecular-change.missense/int n)])))]

           [:td
            (count (:variation/phenotype allele))]

           [:td
            (interpose [:br]
              (for [vs (:variation/strain allele)
                    :let [strain (:variation.strain/strain vs)
                          sid (:strain/id strain)]]
                [:a.strain-link {:href (str "/view/strain/" sid)}
                 sid]))]

           ])]]])))

(defn- is-cgc? [strain]
  (some #(= (->> (:strain.location/laboratory %)
                 (:laboratory/id))
            "CGC")
        (:strain/location strain)))

(defn- strain-list [strains]
  (interpose
   ", "
   (for [strain strains]
      [:a.strain-link {:href (str "/view/strain/" (:strain/id strain))}
       (:strain/id strain)])))
            

(defn gene-genetics-strain-table [db id]
  (let [gene    (entity db [:gene/id id])
        strains (->> (q '[:find (pull ?strain [:strain/id
                                               :gene/_strain
                                               :transgene/_strain
                                               :strain/genotype
                                               {:strain/location [{:strain.location/laboratory [:laboratory/id]}]}])
                          :in $ ?gid
                          :where [?gene :gene/id ?gid]
                                 [?gene :gene/strain ?strain]]
                        db id)
                     (map first))]
    (list
     [:table.venn {:cellspacing "0" :cellpadding "5"}
      [:tbody
       [:tr.venn-a
        [:th {:colspan "2"}
         "Carrying "
         [:a.gene-link {:href (str "/view/gene/" id)}
          [:span.locus (:gene/public-name (entity db [:gene/id id]))]]
         " alone"]]
       [:tr
        [:th.venn-a]
        [:th.venn-ab]
        [:th.venn-b "Available from the CGC"]
        [:th "Other strains"]]
       [:tr.venn-data
        [:td.venn-a  (strain-list (filter #(and (not (seq (:transgene/_strain %)))
                                                (= (count (:gene/_strain %)) 1)
                                                (not (is-cgc? %)))
                                          strains))]
        [:td.venn-ab (strain-list (filter #(and (not (seq (:transgene/_strain %)))
                                                (= (count (:gene/_strain %)) 1)
                                                (is-cgc? %))
                                          strains))]
        [:td.venn-b  (strain-list (filter #(and (or (seq (:transgene/_strain %))
                                                    (not= (count (:gene/_strain %)) 1))
                                                (is-cgc? %))
                                          strains))]
        [:td         (strain-list (filter #(and (or (seq (:transgene/_strain %))
                                                    (not= (count (:gene/_strain %)) 1))
                                                (not (is-cgc? %)))
                                          strains))]]
       [:tr
        [:td]
        [:td.venn-b {:colspan "2"}]]]]

     [:br]

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
           [:a.strain-link {:href (str "/view/strain/" (:strain/id strain))}
            (:strain/id strain)]]
          [:td (first (:strain/genotype strain))]
          [:td
           (if (is-cgc? strain)
             [:a {:href (str "https://cgcdb.msi.umn.edu/search.php?st=" (:strain/id strain))} "yes"]
             "no")]])]]
                                        

   )))

(defn gene-genetics-widget [db id]
  (let [gids (q '[:find ?g :in $ ?gid :where [?g :gene/id ?gid]] db id)]
    (if-let [[gid] (first gids)]
     (let [g (entity db gid)
           gene-name (:gene/public-name g)
           alleles (->> (q '[:find (pull ?var [{(limit :variation/predicted-cds 20)
                                                    [{:variation.predicted-cds/cds [:cds/id]
                                                      :molecular-change/missense [:molecular-change.missense/int
                                                                                  :molecular-change.missense/text]
                                                      :molecular-change/intron   [:db/id]
                                                      :molecular-change/coding-exon [:db/id]
                                                      :molecular-change/utr-5 [:db/id]
                                                      :molecular-change/utr-3 [:db/id]
                                                      :molecular-change/silent [:db/id]
                                                      :molecular-change/amber-uag [:db/id :molecular-change.amber-uag/text]
                                                      :molecular-change/ochre-uaa [:db/id :molecular-change.ochre-uaa/text]
                                                      :molecular-change/opal-uga [:db/id :molecular-change.opal-uga/text]
                                                      :molecular-change/ochre-uaa-or-opal-uga [:db/id :molecular-change.ochre-uaa-or-opal-uga/text]
                                                      :molecular-change/amber-uag-or-ochre-uaa [:db/id :molecular-change.amber-uag-or-ochre-uaa/text]
                                                      :molecular-change/amber-uag-or-opal-uga [:db/id :molecular-change.amber-uag-or-opal-uga/text]
                                                      :molecular-change/frameshift [:db/id]}]
                                                    (limit :variation/transcript 20)
                                                    [{:molecular-change/missense [:molecular-change.missense/int
                                                                                  :molecular-change.missense/text]
                                                      :molecular-change/intron   [:db/id]
                                                      :molecular-change/coding-exon [:db/id]
                                                      :molecular-change/utr-5 [:db/id]
                                                      :molecular-change/utr-3 [:db/id]
                                                      :molecular-change/silent [:db/id]
                                                      :molecular-change/frameshift [:db/id]}]
                                                    :variation/method [:method/id]
                                                    :variation/strain [{:variation.strain/strain [:strain/id]}]
                                                    :variation/insertion [:db/id]
                                                    :variation/substition [:db/id]
                                                    :variation/deletion [:db/id]
                                                    :variation/inversion [:db/id]
                                                    :variation/tandem-duplication [:db/id]
                                                    :variation/phenotype [:db/id]}
                                               :variation/allele
                                               :variation/predicted-snp
                                               :variation/public-name
                                               :variation/id])
                             :in $ ?gid
                             :where [?gene :gene/id ?gid]
                                    [?vh :variation.gene/gene ?gene]
                                    [?var :variation/gene ?vh]]
                           db id)
                        (map first))]
       (html
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
          [:div#content
           (if-let [refa (->> (:gene/reference-allele g)
                              (first)
                              (:gene.reference-allele/variation))]
             (html
              [:div.field
               [:div.field-title "Reference allele"]
               [:div.field-content
                [:a.variation-link {:href (str "/view/variation/" (:variation/id refa))}
                 [:span.var (:variation/public-name refa)]]]]))
           [:div.field
            [:div.field-title "Alleles:"]
            [:div.field-content
             (gene-genetics-alleles-table db id alleles)
             [:script allele-table-script]]]

           [:div.field
            [:div.field-title "Polymorphisms & Natural variants:"]
            [:div.field-content 
             (gene-genetics-poly-table db id alleles)]]

           [:div.field
            [:div.field-title "Strains:"]
            [:div.field-content
             (gene-genetics-strain-table db id)]]

           ]]])))))
