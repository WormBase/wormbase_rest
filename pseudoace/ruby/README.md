# Ruby Datomic adaptor prototype

This is a simple experiment in implementing bits of the WormBase website API on top
of Datomic using Ruby.  We're querying Datomic directly using the peer library,
so this will only work with JRuby (tested with version 1.7.10).

## Prerequisites

      jgem install sinatra
      jgem install sinatra-json
      jgem install diametric

Diametric currently defaults to using a fairly old version of the Datomic peer library.
Try editing $JRUBY/lib//ruby/gems/shared/gems/diametric-0.1.3-java/datomic_version.yml to
point to a recent version (I use 0.9.5130).

## Running the server

      jruby server.rb

...should do the trick.