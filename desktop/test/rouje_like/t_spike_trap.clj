(ns rouje-like.t_spike_Trap
              (:use [midje.sweet]
                    [rouje-like.spike-trap])
              (:require [rouje-like.core :as rj.core]
                        [brute.entity :as br.e]
                        [rouje-like.entity-wrapper :as rj.e]
                        [rouje-like.utils :refer :all]
                        [rouje-like.components :as rj.c :refer [->3DPoint]]
                        [rouje-like.world :as rj.w]))

(defn get-system []
  (with-open [w (clojure.java.io/writer "NUL")]
    (binding [*out* w]
      (-> (br.e/create-system)
          (rj.core/init-entities {})))))

(let [system (get-system)]
  (fact "add-trap"
        (as-> system system
              (:system (add-trap {:system system :z 1}))
              (nil? (rj.e/get-c-on-e system (first (rj.e/all-e-with-c system :spike-trap)) :position)))
        => false))


