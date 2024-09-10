(ns user)

;; Debugging atom.
(def state (atom nil))
(defn x [x] (reset! state x))
