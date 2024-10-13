(ns jaketothepast.commit-to-social
  (:require
   [ring.util.response :as resp]
   [ring.middleware.json :as json]
   [reitit.ring :as ring]
   [integrant.core :as ig]
   [clojure.java.io :as io]
   [clojure.edn :as edn])
  (:gen-class))

;; TODO: :llm/handler needs to be read from the system, rather than the config.
;;  - Need system-wide way to manage system, in reload-friendly fashion

(defn -main
  "Print the last commit as a patch, then shutdown. In the future, this will be a full-blown webserver
  that runs and receives patch files as diffs. Will then take that patch file and pass it to the LLM to prompt and send off
  to various social media platforms.

  That's all"
  [& args])
