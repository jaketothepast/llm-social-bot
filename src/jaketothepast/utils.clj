(ns jaketothepast.utils)

(defn promise? [v]
  (every? #(instance? % v)
         [clojure.lang.IPending
          clojure.lang.IFn
          clojure.lang.IBlockingDeref
          clojure.lang.IDeref]))
