(ns jaketothepast.llms.protocols)

(defprotocol PromptProto
  (make-prompt [obj message]))
