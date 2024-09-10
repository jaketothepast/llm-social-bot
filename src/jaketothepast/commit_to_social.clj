(ns jaketothepast.commit-to-social
  (:require [clojure.java.shell :as shell :refer [sh]]
            [ring.adapter.jetty :as jetty]
            [ring.util.response :as resp]
            [ring.util.request :refer [body-string]]
            [ring.middleware.json :as json]
            [compojure.core :refer [defroutes POST]]
            [reitit.ring :as ring]
            [integrant.core :as ig]
            [user :as u]
            [jaketothepast.llms.openai :as openai])
  (:gen-class))

(def config
  (ig/read-string (slurp "config.edn")))

(defn commit-message-handler
  "Receive commit message as input, transform into tweet and post to social media"
  [request]
  (let [body-str (body-string request)]
    (resp/response {:status ((:llm/handler system) body-str)})))

(defroutes routes
  (POST "/commit-msg" [request] commit-message-handler))

(def app
  (ring/ring-handler
   (ring/router
    ["/commit-msg" {:post {:handler #'commit-message-handler}}]
    {:data {:middleware [json/wrap-json-response]}})))

(defmethod ig/init-key :adapter/jetty [_ opts]
  (jetty/run-jetty app opts))

(defmethod ig/halt-key! :adapter/jetty [_ server]
  (.stop server))

(defmethod ig/init-key :adapter/prod-jetty [_ opts]
  (jetty/run-jetty app opts))

(defn make-anthropic-handler [k]
  (fn [k] k))

(defmethod ig/init-key :llm/handler [_ {:keys [type key]}]
  (cond
    (= type :openai) (openai/->ChatGPT key)
    (= type :anthropic) (make-anthropic-handler key)))

(defn -main
  "Print the last commit as a patch, then shutdown. In the future, this will be a full-blown webserver
  that runs and receives patch files as diffs. Will then take that patch file and pass it to the LLM to prompt and send off
  to various social media platforms.

  That's all"
  [& args])

(comment
  (def system (ig/init config))
  (ig/halt! system)

  ((:llm/handler system) "hey")

  (let [patch (slurp "sample-patch.txt")]
    ((:llm/handler system) patch))


  (-main))
