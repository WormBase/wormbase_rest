(ns user
  (:require [clojure.string :as str]
            [clojure.pprint :refer (pprint)]
            [clojure.repl :refer :all]
            [clojure.test :as test]
            [clojure.tools.namespace.repl :refer (refresh refresh-all)]
            [datomic.api :as d]
            [mount.core :as mount]))



(defn resolve-xref-info
  "Resolve object references to an attribute `ident` given a `db` handle.

  Returned map may contain the following keys:
     `:ref-schema` - symbol of the referenced schema
     `:component-schema` - symbol of the component schema if `ident`
                           is a component attribute.
  "
  [db ident]
  (if-let [attr (d/entity db ident)]
    (if (:db/isComponent attr)
      {:component-schema (-> (:pace/use-ns attr)
                             (first)
                             (symbol))
       :ref-schema (symbol (str (namespace ident)
                                "."
                                (name ident)))}
      (if-let [ref (:pace/obj-ref attr)]
        {:ref-schema (-> ref
                         (namespace)
                         (symbol))}
        ))
    (throw (Exception. (str ident
                            " is not a valid schema attribute")))))
