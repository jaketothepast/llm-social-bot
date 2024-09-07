(ns jaketothepast.commit-to-social
  (:require [clojure.java.shell :as shell :refer [sh]]
            [ring.adapter.jetty :as jetty]
            [ring.util.response :as resp]
            [ring.util.request :refer [body-string]]
            [ring.middleware.json :as json]
            [compojure.core :refer [defroutes POST]]
            [integrant.core :as ig])
  (:gen-class))

(def config
  {:adapter/jetty {:port 8000}})

(defn last-commit-as-patch
  "Grab the last commit as a patch, as a string

  - dir: optional, directory to run this in

  TODO: Refactor this, I'm sure it can be done"
  ([dir]
   (shell/with-sh-dir dir
     (:out (sh "git" "show" "HEAD"))))
  ([] (:out (sh "git" "show" "HEAD"))))

(defn -main
  "Print the last commit as a patch, then shutdown. In the future, this will be a full-blown webserver
  that runs and receives patch files as diffs. Will then take that patch file and pass it to the LLM to prompt and send off
  to various social media platforms.

  That's all"
  [& args]
  (let [commit (last-commit-as-patch)]
    (println commit)
    (shutdown-agents)))

(defn commit-message-handler
  "Receive commit message as input, transform into tweet and post to social media"
  [request]
  (let [body-str (body-string request)]
    {:status "received request"}))

(defroutes routes
  (POST "/commit-msg" [] {:status "success"}))

(def app
  (json/wrap-json-response routes))

(defmethod ig/init-key :adapter/jetty [_ opts]
  (jetty/run-jetty app (-> opts (assoc :join? false))))

(defmethod ig/halt-key! :adapter/jetty [_ server]
  (.stop server))

(comment
  (last-commit-as-patch "/home/jacob/Projects/fastmath")
  (last-commit-as-patch)

  ;; To start the system
  (def system
    (ig/init config))

  ;; To stop the system
  (ig/halt! system))
