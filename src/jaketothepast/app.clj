(ns jaketothepast.app
  (:require [jaketothepast.system :as sys]
            [reitit.ring :as ring]
            [ring.util.response :as resp]
            [ring.middleware.json :as json]
            [ring.util.request :refer [body-string]]
            [jaketothepast.socials.twitter :as twitter]))

(defn commit-message-handler
  "Receive commit message as input, transform into tweet and post to social media"
  [request]
  (let [body-str (body-string request)
        tweet-text ((:llm/handler sys/system) body-str)
        tweet (twitter/tweet {:text tweet-text})]
    (resp/response {:status tweet-text})))

(def app
  (ring/ring-handler
   (ring/router
    ["/commit-msg" {:post {:handler #'commit-message-handler}}]
    {:data {:middleware [json/wrap-json-response]}})))
