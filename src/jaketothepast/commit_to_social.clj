(ns jaketothepast.commit-to-social
  (:require
   [ring.adapter.jetty :as jetty]
   [ring.util.response :as resp]
   [ring.util.request :refer [body-string]]
   [ring.middleware.json :as json]
   [reitit.ring :as ring]
   [integrant.core :as ig]
   [jaketothepast.llms.openai :as openai]
   [jaketothepast.llms.anthropic :as anthropic]
   [jaketothepast.llms.local :as local]
   [clojure.java.io :as io]
   [clojure.edn :as edn]
   [taoensso.telemere :as t]
   [jaketothepast.socials.twitter :as twitter])
  (:gen-class))

(def config
  (ig/read-string (slurp "config.edn")))

(def system (atom nil))

;; TODO: :llm/handler needs to be read from the system, rather than the config.
;;  - Need system-wide way to manage system, in reload-friendly fashion
(defn commit-message-handler
  "Receive commit message as input, transform into tweet and post to social media"
  [request]
  (let [body-str (body-string request)
        tweet-text ((:llm/handler @system) body-str)
        tweet-response (twitter/tweet {:text tweet-text})]
    (resp/response {:status tweet-text})))

(defn post-commit-script
  [request]
  (let [script (slurp (io/resource "post-commit-script.sh"))]
    ))

(def app
  (ring/ring-handler
   (ring/router
    [["/commit-msg" {:post {:handler #'commit-message-handler}}]
     ["/post-commit" {:get {:handler #'post-commit-script}}]]
    {:data {:middleware [json/wrap-json-response]}})))

(defmethod ig/init-key :adapter/jetty [_ opts]
  (jetty/run-jetty app opts))

(defmethod ig/halt-key! :adapter/jetty [_ server]
  (.stop server))

(defmethod ig/init-key :adapter/prod-jetty [_ opts]
  (jetty/run-jetty app opts))

(defmethod ig/init-key :llm/handler [_ {:keys [type key url local-dir] :as llm}]
  (cond
    (= type :openai) (openai/->ChatGPT key)
    (= type :anthropic) (anthropic/->Claude key)
    (= type :local) (local/config->Local url local-dir)))

(def log-fn (fn [val] (t/log! :info val)))

(defn -main
  "Print the last commit as a patch, then shutdown. In the future, this will be a full-blown webserver
  that runs and receives patch files as diffs. Will then take that patch file and pass it to the LLM to prompt and send off
  to various social media platforms.

  That's all"

  [& args]
  (add-tap log-fn)
  (reset! system (ig/init config))
  (tap> @system))

(comment
  (def system (ig/init config))
  (ig/halt! system)
  (:llm/handler system)

  ((:llm/handler system) "hey")

  (let [patch (slurp "sample-patch.txt")]
    ((:llm/handler system) patch))

  local/local-llm-state

  system
  (add-tap log-fn)
  (remove-tap log-fn)

  (-main)
  )
