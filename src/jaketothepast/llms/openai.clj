(ns jaketothepast.llms.openai
  (:require [wkok.openai-clojure.api :as api]
            [jaketothepast.llms.protocols :as protocols]
            [jaketothepast.llms.prompts :as prompts]))

(def model "gpt-4o")

(defn get-response [{:keys [choices]}]
  (-> (nth choices 0)
      :message
      :content))

(defrecord ChatGPT [key]
  clojure.lang.IFn
  (invoke [this commit-msg]
    (get-response (api/create-chat-completion
                   {:model model
                    :messages (protocols/make-prompt this commit-msg)}
                   {:api-key key})))
  protocols/PromptProto
  (make-prompt [_ message]
    [{:role "system" :content prompts/system-prompt}
     {:role "user" :content message}]))


(comment
  (let [model (->ChatGPT "mykey")]
    (model "hey")))
