# pseudoace



- Reading and writing ACeDB model files.
- Generating Datomic schemas based on ACeDB models.
- Model-driven import of ACeDB data into multiple databases (currently Datomic and MongoDB).
- Emulating an ACeDB server (currently incomplete).

Installation

sudo yum -y install perl-autodie perl-IPC-System-Simple


##Ace to Datomic

This tool is used to import databases from ACeDB to Datomic


   - To get help run the following command

lein run ace-to-datomic --help

###Help Output

Ace to dataomic is tool for importing data from ACeDB into to Datomic database

Usage: ace-to-datomic [options] action

Options:
      --model PATH                             Specify the model file that you would like to use that is found in the models folder e.g. models.wrm.WS250.annot
      --url URL                                Specify the url of the Dataomic transactor you would like to connect. Example: datomic:free://localhost:4334/WS250
      --schema-filename PATH                   Specify the name of the file for the schema view to be written to when selecting Action: generate-schema-view exampls schema250.edn
      --log-dir PATH                           Specifies the path to and empty directory to store the Datomic logs in. Example: /datastore/datomic/tmp/datomic/import-logs-WS250/
      --acedump-dir PATH                       Specifies the path to the directory of the desired acedump. Example /datastore/datomic/tmp/acedata/WS250/
      --backup-file PATH                       Secify the path to the file in which you would like to have the database dumped into
      --datomic-database-report-filename PATH  Specify the relative or full path to the file that you would like the report to be written to
  -v, --verbose
  -f, --force
  -h, --help

Actions: (required options for each action are provided in square brackets)
  create-database                      Select this option if you would like to create a Datomic database from a schema. Required options [model, url]
  generate-datomic-schema-view         Select if you would like the schema to the database to be exported to a file. Required options [schema-filename, url]
  acedump-to-datomic-log               Select if you are importing data from ACeDB to Datomic and would like to create the Datomic log files [url, log-dir, acedump-dir]
  sort-datomic-log                     Select if you would like to sort the log files generated from your ACeDB dump [log-dir]
  import-logs-into-datomic             Select if you would like to import the sorted logs back into datomic [log-dir, url]
  excise-tmp-data                      Select in order to remove all the tmp data that was created in the database to import the data [url]
  test-datomic-data                    Select if you would like to perform tests on the generated database [url acedump-dir]
  all-import-actions                   Select if you would like to perform all actions from acedb to datomic [model url schema-filename log-dir acedump-dir]
  generate-datomic-database-report     Select if you want to generate a summary report of the contents of a particular Datomic database [url datomic-database-report-filename]
  list-databases                       Select if you would like to get a list of the database names [url]
  delete-database                      Select this option if you would like to delete a database in datomic [url]. If the force option you will not be asked if you are certain about your decision
  backup-database                      Select if you would like to backup a datomic database into a file


Example command

lein run ace-to-datomic generate-datomic-database-report --url datomic:free://localhost:4334/WS250 --datomic-database-report-filename /home/ec2-user/git/db/pseudoace/hello.t -v

