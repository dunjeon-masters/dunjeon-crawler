(ns rouje-like.test-utils
  (:use [midje.sweet])
  (:require [brute.entity :as br.e]
            [rouje-like.core :as rj.core]))

(def dev-null
  (let [os (System/getProperty "os.name")]
    (case os
      "Mac OS X" "/dev/null"
      "Linux" "/dev/null"
       "NUL")))

(defn init []
  (with-open [w (clojure.java.io/writer dev-null)]
    (binding [*out* w]
      (br.e/create-system))))

(defn start
  ([]
   (with-open [w (clojure.java.io/writer dev-null)]
     (binding [*out* w]
       (-> (init)
           (rj.core/init-entities {})))))
  ([system]
   (with-open [w (clojure.java.io/writer dev-null)]
     (binding [*out* w]
       (-> system
           (rj.core/init-entities {})))))
  ([system world-type]
   (with-open [w (clojure.java.io/writer "NUL")]
     (binding [*out* w]
       (-> system
           (rj.core/init-entities {}))))))

(fact "init"
      (init) => {:entity-component-types {}
                 :entity-components      {}})

(fact "start"
      (type (start)) => clojure.lang.PersistentArrayMap)
