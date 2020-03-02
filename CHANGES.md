# Change Log
All notable changes to this project will be documented in this file.
This project adheres to [Semantic Versioning](http://semver.org/).

## [1.2.2] -2020-03-02
- Fix for bug for public-name found during caching

## [1.2.1] - 2020-02-26
- Removing Protein overview and motif details widget

## [1.2.0] - 2020-02-24
- release for WS275
- contains protein overview and motif details widget
- contains GOTerm page associations widget
- contains gene Homology widet!  

## [1.1.0] - 2019-12-19
- release for WS274
- retrieve protein homology data
- retrieve data for interaction venn
- fix the display of premature stop codon
- fix life-stage description/definiton WormBase/website#7463
- fix typo in all_labs representative column

## [1.0.2] - 2019-10-24
- release for WS273
- stop merging annotations for interactions

## [1.0.1] - 2019-09-29
- release for WS272

## [1.0.0] - 2019-06-19
- created several of the entity level widgets
- first release for WS271

## [0.9.9] - 2019-06-19
- Fix MTGE transcript bug found by user

## [0.9.8] - 2019-06-13
- releasing PCR-oligo sequences widget
- fixing a couple bugs with variation molecular details widget

## [0.9.7] - 2019-05-25
- Fixing bugs

## [0.9.6] - 2019-05-20
- fixing handling of multiple 5 prime and/or 3 prime utr on transcripts and cds sequence pages

## [0.9.5] - 2019-05-18
- fix for #6490 (off by one error with variation molecular details widget)

## [0.9.4] - 2019-05-16
- fixed minor issues with #6437 and #7112

## [0.9.3] - 2019-05-15
- Adding logic for when there is not enough room for 2000 bp padding on either side

## [0.9.2] - 2019-05-13
- Fix missing 5 prime UTR in spliced transcript sequence caused by typo

## [0.9.1] - 2019-05-10
- Adding in pseudogene, transcript and CSD widgets

## [0.9.0] - 2019-04-22
- First release for WS270

## [0.8.4] - 2019-04-08
- Hotfix for ASSEMBLIES.json

## [0.8.3] - 2019-03-20
- Fixing when the clond is MTCE for a variations clone

## [0.8.2] - 2019-03-20
- Fixing reverse complement for C

## [0.8.1] - 2019-02-25
- Fixing a couple things with the variation molecular details widget

## [0.8.0] - 2019-02-22
- New disease wigets
- First WS269 release

## [0.7.4] - 2019-01-16
- Hot fix for when there is a variation with and insertion and a deletion

## [0.7.3] - 2019-01-15
- Fix for when there are no primer pairs in gene reagents widget

## [0.7.2] - 2019-01-10
- Fix for variations that are not in sequence database for molecular details widget

## [0.7.1] - 2019-01-10
- Adding Sibyls hotfix for gene descriptions

## [0.7.0] - 2019-01-03
- Adding variation mulecular-details widget

## [0.6.4] - 2018-12-21
- Adding fix for Interactions schema change

## [0.6.3] - 2018-12-17
- Preping for WS268 release

## [0.6.2] - 2018-11-01
- Preping for WS267 release

## [0.6.1] - 2018-08-23
- Small changes
- first release for WS266

## [0.6.0] - 2018-07-05
- Adding endpoints for interaction widgets
- Adding detailed disease model table

## [0.5.2] - 2018-06-08
- Fixing issue with variation

## [0.5.1] - 2018-05-16
- Fixing issue with missing sequence for Fosmids

## [0.5.0] - 2018-05-10
- Adding endpoints for WS264 release

## [0.4.1] - 2018-03-07 - Hotfix
- Fixing 500 errors for gene page expression widget

## [0.4.0] - 2018-02-24
- first release with autoscaling
- several new urls added

## [0.3.9] - 2018-01-26 - Hotfix
- Commenting out lineage widget - causing d2c to crash
- Fixing issue with rnai overview widget from laboratory pack-obj bug

## [0.3.8] - 2018-01-26
- fixed 500 errors from pcr_oligo page

## [0.3.7] - 2018-01-26
- fixed 500 was till persisting final fix
- adding merged into data for variation overview widget

## [0.3.6] - 2018-01-26
- fixed 500 error on variation/locations widgets where variations were merged

## [0.3.5] - 2018-01-26
- fixed 500 error on gene-class/overview widget

## [0.3.4] - 2018-01-09
- make idempotent

## [0.3.3] - 2018-01-09
- Tested and fixed nginx configuration file

## [0.3.2] - 2018-01-09
- Fixing location of nginx configuration file

## [0.3.1] - 2018-01-09
- Adding in extened-proxy-timout.config for EB

## [0.3.0] - 2018-01-09
- Added a significant number of enpoints for WS262 release

## [0.2.4] - 2017-11-03
- changing JVM and datomic memory settings

## [0.2.3] - 2017-10-25
- uncommenting phenotype variation widget

## [0.2.2] - 2017-10-23
- Adding phenotype widgets
- Adding association widgets
- Adding expresion cluster widgets
- Adding some other ones as well

## [0.2.1] - 2017-09-13
- Fix for rearrangement overview widget 500 error

## [0.2.0] - 2017-08-16
- 11 more overview widgets
- all location widgets

## [0.1.8] - 2017-07-25
- Fix bug in utility functions that affects links to papers
- Fix bug in expression widget image display

## [0.1.7] - 2017-07-05
- Minor Bug fixes

## [0.1.6] - 2017-06-19
- Includes fixes by Matt for the gene interaction widget
- commenting out the person widget

## [0.1.5] - 2017-06-11
- Half of the Overview widgets
- Gene pages Disease widget
- Ontology widgets

## [0.1.4] - 2017-05-31
- Gene interactions widget.
- Phenotype widgets

## [0.1.3] - 2017-03-31
- Gene page widgets added: sequences, expression, phenotype-graph,
  reagents and location.
- Person page widgets added: laboratory, overview, publications, tracking.
- All exteranla links.

## [0.1.2] - 2017-02-07
- swagger UI will now display validator badge.

## [0.1.1] - 2017-02-01
- Split up code into new structure.
- Refactored (compojure-api) routing code.
- Sort swagger API endpoint listings.
- Removed unused code.
- Code QA with `lein eastwood`.

## [0.1.0] - 2017-01-29
- New routing using compojure-api
- Automated API listing using swagger (compojure-api)

## [0.0.15] - 2017-01-25
- Added env variable to eb config.

## [0.0.14] - 2017-01-25
- upgrade-to-datomic-0.9.5554
- Fix-require-widgets-for-side-effects
- Added sequence feature widget
- Added gene ontology widget

## [0.0.1] - 2016-07-26
- Externals links URL for the gene page
