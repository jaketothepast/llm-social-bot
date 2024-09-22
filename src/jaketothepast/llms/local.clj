(ns jaketothepast.llms.local
  (:require [jaketothepast.llms.protocols :as protocols]))

;; This namespace encapsulates using local LLMs via llama.clj. To use these models, simply download the models
;; yourself, then load in the model type using the classpath.

(defrecord Local [url local-dir]
  clojure.lang.IFn
  (invoke [_ commit-msg]
    (prn commit-msg))
  protocols/PromptProto
  (make-prompt [_ message]
    ))
