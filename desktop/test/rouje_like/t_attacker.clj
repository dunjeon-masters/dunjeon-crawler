(ns rouje-like.t-attacker
  (:use [midje.sweet]
        [rouje-like.attacker])
  (:require [rouje-like.utils :as rj.u :refer [?]]
            [rouje-like.core :as rj.core]
            [brute.entity :as br.e]
            [rouje-like.entity-wrapper :as rj.e]))

(defn get-system []
  (with-open [w (clojure.java.io/writer "NUL")]
    (binding [*out* w]
      (-> (br.e/create-system)
          (rj.core/init-entities {})))))

(let [system (get-system)
      system (#'rouje-like.world/init-themed-entities system 1 :maze)
      e-player (first (rj.e/all-e-with-c system :player))
      c-attacker (rj.e/get-c-on-e system e-player :attacker)
      e-slime (first (rj.e/all-e-with-c system :slime))
      e-skeleton (first (rj.e/all-e-with-c system :skeleton))
      c-attacker-skeleton (rj.e/get-c-on-e system e-skeleton :attacker)]
  (facts "can-attack?"
         (fact "success"
               (can-attack? c-attacker nil e-slime system)) => true
         (fact "failure"
               (can-attack? c-attacker-skeleton nil e-slime system) => false))

  #_(fact "attack"
        (attack (? c-attacker) (? e-player) (? e-skeleton) (? system)) => truthy))
