(ns jaketothepast.system
  (:require [jaketothepast.commit-to-social :as cts]
            [jaketothepast.llms.openai :as openai]
            [integrant.core :as ig]
            [ring.adapter.jetty :as jetty]
            [jaketothepast.llms.anthropic :as anthropic]
            [jaketothepast.llms.local :as local]
            [ring.util.response :as resp]
            [ring.middleware.json :as json]
            [jaketothepast.app :as app]))

(def config
  (ig/read-string (slurp "config.edn")))

(def system nil)


(defmethod ig/init-key :adapter/jetty [_ opts]
  (jetty/run-jetty app/app opts))

(defmethod ig/halt-key! :adapter/jetty [_ server]
  (.stop server))

(defmethod ig/init-key :adapter/prod-jetty [_ opts]
  (jetty/run-jetty app/app opts))

(defmethod ig/init-key :llm/handler [_ {:keys [type key url local-dir]}]
  (cond
    (= type :openai) (openai/->ChatGPT key)
    (= type :anthropic) (anthropic/->Claude key)
    (= type :local) (local/config->Local url local-dir)))

(defmethod ig/init-key :socials/twitter [_ {:keys [client-id client-secret]}]
  {:client-id client-id :client-secret client-secret})

(comment
  (def system (ig/init config))
  (ig/halt! system))
