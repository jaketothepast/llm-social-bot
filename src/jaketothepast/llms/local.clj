(ns jaketothepast.llms.local
  (:require [jaketothepast.llms.protocols :as protocols]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clj-http.client :as client]
            [com.phronemophobic.llama :as llama]
            [clojure.string :as str]
            [jaketothepast.llms.prompts :as prompts]
            [jaketothepast.utils :as utils]))

;; This namespace encapsulates using local LLMs via llama.clj. To use these models, simply download the models
;; yourself, then load in the model type using the classpath.
;;
(def local-llm-state (atom {:model-context nil
                            :model-location nil}))

(defn- model-name [url]
  (-> (io/as-url url)
      .getFile
      (str/split #"/")
      last))

(defn- download-model
  "Either download the model, or return the path."
  [url local-dir]
  (let [filename (model-name url)
        model-file (io/file local-dir filename)
        file-write-promise (promise)]
    (if (.exists model-file)
      (deliver file-write-promise (.getCanonicalPath model-file)) ;; It exists, deliver the path
      (future (do ;; It doesn't exist, download it on the background thread.
                (when (not (.exists model-file))
                  (with-open [in (io/input-stream (io/as-url url))
                              out (io/output-stream model-file)]
                    (io/copy in out)))
                (deliver file-write-promise (.getCanonicalPath model-file)))))
    file-write-promise))

(defn- retrieve-model
  "Get the model weights, optionally download the model weights from the internet
  first. If we download the model weights from the internet, then write the weight
  location to the cache."
  [url local-dir]
  (let [dir (io/file local-dir)]
    ;; Handle creating our model dir and our cache file location
    (or (.exists dir) (.mkdirs dir)) ; Create the local directory, this didn't expand tilde
    (let [model-location (download-model url local-dir)] ;; Gets a promise
      (swap! local-llm-state assoc :model-location model-location))))

(defn- model-context
  "When invoking the model, don't try to deref the promise until now. This gives it time to load in the background"
  [n-ctx]
  (let [model-location (:model-location @local-llm-state)
        location (cond-> model-location
                   (utils/promise? model-location) deref) ;; Deref the promise from retriev
        context (:model-context @local-llm-state)]
    (tap> {:location location
           :context context})   
    (if (nil? context)
      (swap! local-llm-state merge {:model-location location
                                    :model-context (llama/create-context location {:n-ctx n-ctx})})) ;; TODO: n-ctx should be a local configuration
    (:model-context @local-llm-state)))

(defrecord Local [n-ctx]
  clojure.lang.IFn
  (invoke [this commit-msg]
    (tap> {:commit-msg commit-msg
           :llm-state @local-llm-state})
    (llama/generate-string
     (model-context n-ctx)
     (protocols/make-prompt this commit-msg)))
  protocols/PromptProto
  (make-prompt [_ message]
    (llama/chat-apply-template
     (:model-context @local-llm-state)
     [{:role "system"
       :content prompts/system-prompt}
      {:role "user"
       :content message}])))

(defn config->Local [url local-dir]
  (retrieve-model url local-dir)
  (->Local 8096))

(comment
  (-> (io/file "~" "models") (.getCanonicalFile))
  (-> (io/file "../models") (.getAbsolutePath))
  (retrieve-model  "." "https://example.com/clunker.txt")
  (def llama-model "https://huggingface.co/QuantFactory/Meta-Llama-3-8B-Instruct-GGUF/resolve/main/Meta-Llama-3-8B-Instruct.Q2_K.gguf")
  (model-name llama-model)

  (download-model llama-model "./src/models")
  (retrieve-model llama-model "./src/models")
  (model-context)

  @local-llm-state

  (def location (:model-location @local-llm-state))
  (deref location)
  (if (future? location)
    (deref location)
    location)
  (cond-> location
      (utils/promise? location) deref)

  (:model-location @local-llm-state)
  (get (llama/metadata (:model-context @local-llm-state)) "tokenizer.chat_template")

  (llama/chat-apply-template
   (:model-context @local-llm-state)
   [{:role "system"
     :content "You are great"}
    {:role "user"
     :content "You are not great"}])

  (def model (->Local))

  (add-tap clojure.pprint/pprint)
  (model "Hello world")
  ())
