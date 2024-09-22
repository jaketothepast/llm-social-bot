(ns jaketothepast.llms.local
  (:require [jaketothepast.llms.protocols :as protocols]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]))

;; This namespace encapsulates using local LLMs via llama.clj. To use these models, simply download the models
;; yourself, then load in the model type using the classpath.

(def cache-file "sample-models.edn")

(defrecord Local [url local-dir model]
  clojure.lang.IFn
  (invoke [_ commit-msg]
    (prn commit-msg))
  protocols/PromptProto
  (make-prompt [_ message]))

(defn- model-name [url]
  (-> (io/as-url url)
      .getFile
      (str/split #"/")
      last))

(defn- retrieve-model
  "Get the model weights, optionally download the model weights from the internet
  first. If we download the model weights from the internet, then write the weight
  location to the cache."
  [local-dir url]
  (let [cache-file-location (io/file local-dir cache-file)
        dir (io/file local-dir)]
    ;; Handle creating our model dir and our cache file location
    (or (.exists dir) (.mkdirs dir))
    (or (.exists cache-file-location) (.createNewFile cache-file-location))
    (let [data (edn/read-string (slurp cache-file-location))
          model-exists? (and (nil? data) (nil? (get data url)))
          update {url (model-name url)}]
      ;; If data is nil, retrieve model, write update to edn file
      ;; If data is not nil, try to retrieve model. If present, return the model weights
      ;; If data is not nil, can't retrieve model, download model
      update)))

;; TODO
;; 1. Handle local model directory
;; 2. EDN file in model directory that shows what has been downloaded, a cache of requests

(comment
  (-> (io/file "~" "models") (.getCanonicalFile))
  (-> (io/file "../models") (.getAbsolutePath))
  (retrieve-model  "." "https://example.com/clunker.txt")
  (def llama-model "https://huggingface.co/QuantFactory/Meta-Llama-3-8B-Instruct-GGUF/resolve/main/Meta-Llama-3-8B-Instruct.Q2_K.gguf")
  (model-name llama-model)
  )
