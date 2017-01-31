(ns datomic-rest-api.formatters.date
  (:import
   (java.text SimpleDateFormat)))

(defn format-date
  [date-str]
   (.format
     (java.text.SimpleDateFormat. "yyyy-M-dd")
        date-str))

(defn format-date2
  [date-str]
   (.format
     (java.text.SimpleDateFormat. "yyyy-M-dd")
      (.parse
      (java.text.SimpleDateFormat. "yyyy-MM-dd'T'HH:mm:ssZ")
        (str date-str))))

(defn format-date3
  [date-str]
   (if (= date-str "")
     date-str
     (.format
       (java.text.SimpleDateFormat. "yyyy-M-dd")
        (.parse
        (java.text.SimpleDateFormat. "EEE MMM dd HH:mm:ss Z yyyy")
          date-str))))
