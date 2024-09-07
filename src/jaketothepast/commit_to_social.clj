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
  ":join? false - REPL driven development"
  {:adapter/jetty {:port 8000 :join? false}})

(def prod-config
  "Set jetty adapter to join, used in Jar startup"
  {:adapter/prod-jetty {:port 8000 :join? true}})

;;;;;;;;;;;;;;;;;;;;;;;; APPLICATION LOGIC
;;;;;;;;;;;;;;;;;;;;;;;;
(defn commit-message-handler
  "Receive commit message as input, transform into tweet and post to social media"
  [request]
  (let [body-str (body-string request)]
    (resp/response {:status "received request"})))

(defroutes routes
  (POST "/commit-msg" [request] commit-message-handler))

(def app
  (json/wrap-json-response routes))

;;;;;;;;;;;;;;;;;;;;;;; System configuration
;;;;;;;;;;;;;;;;;;;;;;;
(defmethod ig/init-key :adapter/jetty [_ opts]
  (jetty/run-jetty app opts))
(defmethod ig/halt-key! :adapter/jetty [_ server]
  (.stop server))

(defmethod ig/init-key :adapter/prod-jetty [_ opts]
  (jetty/run-jetty app opts))

(def system (atom nil))
(defn start-system [] (reset! system (ig/init config)))
(defn stop-system [] (do (ig/halt! @system)
                         (reset! system nil)))
(defn restart-system [] (do (stop-system) (start-system)))

(defn -main
  "Print the last commit as a patch, then shutdown. In the future, this will be a full-blown webserver
  that runs and receives patch files as diffs. Will then take that patch file and pass it to the LLM to prompt and send off
  to various social media platforms.

  That's all"
  [& args]
  (reset! system (ig/init prod-config)))

(comment
  (last-commit-as-patch "/home/jacob/Projects/fastmath")
  (last-commit-as-patch)

  (start-system)
  (stop-system)
  (restart-system)
  (-main))
