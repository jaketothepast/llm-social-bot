(ns jaketothepast.llms.anthropic
  (:require
    [jaketothepast.llms.protocols :as protocols]))

(defrecord Claude [key]
  clojure.lang.IFn
  (invoke [_ commit-msg]
    (prn commit-msg) ;; Don't do anything yet
    )
  protocols/PromptProto
  (make-prompt [_ message]
    [{:role "system" :content "whatever"}]))
