#db

*The database migration project at WormBase.*

**THIS IS A PRIVATE REPOSITORY**

## FULL-SCALE DATOMIC PROTOTYPE

This can be found in the `pseudoace` directory.  See the [wiki](https://github.com/wormbase/db/wiki)
for more information.

## LAYOUT

   README.md
   data/       -- sample data files, organized by date
   ec2/        -- scripts and credentials for developing
                  and benchmarking on the AWS cloud
   platforms/  -- one directory for each system under evaluation
   
## DATA

smallace datasets, versioned by date, prepared by @khowe. Please
reference which dataset you use in your documentation.

## CONVENTIONS

I suggest we use some standard bioinformatics conventions. For each platform:

* scripts in bin/ (or scripts/)
* lib/ if required, etc.
* If your code requires third party modules, please document them and (if possible) include them as submodules.
* There is a stub README in each directory. This would be a good place to collect documentation for now.

## DOCUMENTATION STANDARDS

Minimally, documentation should include the shell commands necessary to load your resource, expected
input files and any output created.

Better yet, assume a naive user and vanilla system. Include all commands necessary to get that
system up and running with your platform and loaded with the test data.  This will enable any of us
to test, verify or extend any of the platforms with minimal work by launching a new instance on AWS. 
We can (and should) automate the installation, configuration and loading of each platform so that we
can easily evaluate more complicated scenarios.

