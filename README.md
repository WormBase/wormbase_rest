# datomic-to-catalyst



- Reading and writing ACeDB model files.
- Generating Datomic schemas based on ACeDB models.
- Model-driven import of ACeDB data into multiple databases (currently Datomic and MongoDB).
- Emulating an ACeDB server (currently incomplete).

Installation

sudo yum -y install perl-autodie perl-IPC-System-Simple

#Ace to Datomic

This tool is used to import databases from ACeDB to Datomic


   - To get help run the following command

lein run dynamodb-to-catalyst --help


##Setting environment variables
    export TRACE_PORT=8130
    export TRACE_DB="datomic:ddb://us-east-1/wormbase/WS252"

## starting server

    lein repl
    >  (use 'web.core)
