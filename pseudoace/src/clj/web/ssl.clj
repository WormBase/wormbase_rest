(ns web.ssl
  (:require [cemerick.friend :as friend]
            [cemerick.friend.workflows :as workflows]
            [cemerick.friend.util :refer (gets)]))

(defn client-cert-workflow [& {:keys [credential-fn] :as config}]
  (fn [{cert :ssl-client-cert :as request}]
    (when cert
      (if-let [user-record ((gets :credential-fn config (::friend/auth-config request))
                            ^{::friend/workflow :ssl-client-cert}
                            {:ssl-client-cert cert})]
        (workflows/make-auth
         user-record
         {::friend/workflow :ssl-client-cert
          ::friend/redirect-on-auth? false
          ::friend/ensure-session false})))))
