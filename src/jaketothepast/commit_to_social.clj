(ns jaketothepast.commit-to-social
  (:require [clojure.java.shell :as shell :refer [sh]]
            [ring.adapter.jetty :as jetty]
            [ring.util.response :as resp]
            [ring.util.request :refer [body-string]]
            [ring.middleware.json :as json]
            [compojure.core :refer [defroutes POST]]
            [integrant.core :as ig]
            [jaketothepast.llms.openai :as openai])
  (:gen-class))

(def config
  ":join? false - REPL driven development"
  {:adapter/jetty {:port 8000 :join? false}
   :llm/handler {:type :openai :key "test"}})

(def llm-handler (atom nil))

(def prod-config
  "Set jetty adapter to join, used in Jar startup"
  {:adapter/prod-jetty {:port 8000 :join? true}})

;;;;;;;;;;;;;;;;;;;;;;;; APPLICATION LOGIC
;;;;;;;;;;;;;;;;;;;;;;;;
(defn commit-message-handler
  "Receive commit message as input, transform into tweet and post to social media"
  [request]
  (let [body-str (body-string request)]
    (resp/response {:status ((:llm/handler system) "hey")})))

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

(defmethod ig/init-key :llm/handler [_ {:keys [type key]}]
  (prn "Initializing the key")
  (let [handler (cond
                  (= type :openai) (openai/->ChatGPT key)
                  (= type :anthropic) (make-anthropic-handler key))]
    (reset! llm-handler handler
    handler))

(defn -main
  "Print the last commit as a patch, then shutdown. In the future, this will be a full-blown webserver
  that runs and receives patch files as diffs. Will then take that patch file and pass it to the LLM to prompt and send off
  to various social media platforms.

  That's all"
  [& args])

(comment
  (last-commit-as-patch "/home/jacob/Projects/fastmath")
  (last-commit-as-patch)

  (def system (ig/init config))
  (ig/halt! system)

  ((:llm/handler system) "hey")

  (@llm-handler)

  (-main))
