(ns rouje-like.t-attacker
  (:use [midje.sweet]
        [rouje-like.test-utils]
        [rouje-like.attacker])
  (:require [rouje-like.utils :as rj.u :refer [?]]
            [rouje-like.core :as rj.core]
            [brute.entity :as br.e]
            [rouje-like.skeleton :refer [add-skeleton]]
            [rouje-like.entity-wrapper :as rj.e]))


(let [system (start)
      system (:system
               (add-skeleton {:system system
                              :z 1}))
      e-player (first (rj.e/all-e-with-c system :player))
      c-attacker (rj.e/get-c-on-e system e-player :attacker)
      e-skeleton (first (rj.e/all-e-with-c system :skeleton))
      c-attacker-skeleton (rj.e/get-c-on-e system e-skeleton :attacker)]
  (facts "can-attack?"
         (fact "success"
               (can-attack? c-attacker nil e-player system) => truthy)
         (fact "failure"
               (can-attack? c-attacker-skeleton nil e-skeleton system) => falsey))

  (let [system (attack c-attacker e-skeleton e-player system)
        {:keys [max-hp hp]} (rj.e/get-c-on-e system e-player :destructible)]
    (fact "attack"
         hp => (fn [hp]
                 (into #{}
                       (take (:atk c-attacker-skeleton)
                             (iterate dec max-hp))) hp))))
