(ns jaketothepast.llms.openai
  (:require [wkok.openai-clojure.api :as api]
            [jaketothepast.llms.protocols :as protocols]))

(def model "gpt-4o")

(def system-prompt "You write social media updates for developers. You receive git patches as input and turn them into tweets.
Keep the tweets concise, and optimize for engagement. Do not use emojis or hashtags.")

(defrecord ChatGPT [key]
  clojure.lang.IFn
  (invoke [this commit-msg]
    (get-response (api/create-chat-completion
                   {:model model
                    :messages (protocols/make-prompt this commit-msg)}
                   {:api-key key})))
  protocols/PromptProto
  (make-prompt [_ message]
    [{:role "system" :content system-prompt}
     {:role "user" :content message}]))

(defn get-response [{:keys [choices]}]
  (-> (nth choices 0)
      :message
      :content))

(comment
  (let [model (->ChatGPT "mykey")]
    (model "hey")))
