(ns jaketothepast.llms.local
  (:require [jaketothepast.llms.protocols :as protocols]
            [clojure.edn :as edn]
            [clojure.java.io :as io]))

;; This namespace encapsulates using local LLMs via llama.clj. To use these models, simply download the models
;; yourself, then load in the model type using the classpath.

(def cache-file "models.edn")

(defrecord Local [url local-dir model]
  clojure.lang.IFn
  (invoke [_ commit-msg]
    (prn commit-msg))
  protocols/PromptProto
  (make-prompt [_ message]))

;; TODO: Retrieve the model if the model hasn't already been downloaded
(defn- retrieve-model [local-dir url]
  (let [cache (edn/read-string (slurp (io/file local-dir)))]
    ))

(defn config->Local [url local-dir]
  ;; Download the model first, and then use it in the API call
  (let [model-weights (retrieve-model local-dir url)]))

;; TODO
;; 1. Handle local model directory
;; 2. EDN file in model directory that shows what has been downloaded, a cache of requests

(comment
  (-> (io/file "~" "models") (.getCanonicalFile))
  (-> (io/file "../models") (.getAbsolutePath)))
