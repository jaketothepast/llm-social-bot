(ns jaketothepast.llms.openai
  (:require [wkok.openai-clojure.api :as api]
            [jaketothepast.llms.protocols :as protocols]))

(def model "gpt-4o")

(def system-prompt "You are the world's best social media strategist, and your expertise is turning commit messages into high engagement tweets. You work for a developer that hates going on social media.
Structure your tweets accordingly.")


(defrecord ChatGPT [key]
  clojure.lang.IFn
  (invoke [this commit-msg]
    (api/create-chat-completion
     {:model model
      :messages (protocols/make-prompt this commit-msg)}
     {:api-key key}))
  protocols/PromptProto
  (make-prompt [_ message]
    [{:role "system" :content system-prompt}
     {:role "user" :content message}]))

(comment
  (let [model (->ChatGPT "mykey")]
    (model "hey")
    )
  )
