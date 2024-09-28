(ns jaketothepast.llms.local-test
  (:require [jaketothepast.llms.local :as sut]
            [clojure.test :as t]))

(t/deftest the-truth
  (t/is (= 1 1)))

(t/deftest config->Local-tests
  (with-redefs [sut/retrieve-model (promise)]
    (let [local (sut/config->Local "fake" "fake")]
      (t/is (record? local)))))
