(ns rouje-like.t-experience
  (:use [midje.sweet]
        [rouje-like.test-utils]
        [rouje-like.experience])
  (:require [rouje-like.core :as rj.core]
            [brute.entity :as br.e]
            [rouje-like.entity-wrapper :as rj.e]
            [rouje-like.utils :refer :all]
            [rouje-like.components :as rj.c :refer [->3DPoint]]
            [rouje-like.world :as rj.w]))

(let [system (start)
      system (upgrade system :maze)
      e-player (first (rj.e/all-e-with-c system :player))
      c-attacker (rj.e/get-c-on-e system e-player :attacker)
      e-slime (first (rj.e/all-e-with-c system :slime))
      e-skeleton (first (rj.e/all-e-with-c system :skeleton))
      c-attacker-skeleton (rj.e/get-c-on-e system e-skeleton :attacker)]

  (fact "level-exp")

  (fact "wrand"
        (clojure.set/rename-keys
          (frequencies
            (take 10000
                  (repeatedly
                    #(wrand [2 2 1]))))
          {0 :a, 1 :b, 2 :c})
        => (fn [{:keys [a b c]}]
             ((roughly 2 1/5)
              (/ (* 1/2 (+ a b)) c))))

  (facts "level-up-stats"
         (fact "successfully increment players attribute"
               (as-> system system
                 (rj.e/upd-c system e-player :experience
                             (fn [c-experience]
                               (update-in c-experience [:level]
                                          + 4)))
                 (let [s (level-up-stats system e-player :atk)]
                   (:atk
                     (rj.e/get-c-on-e s
                                      (first (rj.e/all-e-with-c s :player))
                                      :attacker)))) => (inc (:atk (rj.e/get-c-on-e system e-player :attacker))))

         (fact "successfully give player a spell at level 5"
               (as-> system system
                 (rj.e/upd-c system e-player :experience
                             (fn [c-experience]
                               (update-in c-experience [:level]
                                          + 4)))
                 (let [s (level-up-stats system e-player)]
                   (count (:spells
                            (rj.e/get-c-on-e s
                                             (first (rj.e/all-e-with-c s :player))
                                             :magic))))) => 1)

         (fact "successfully upgrade a spell at level 10"
               (let [system (rj.e/upd-c system e-player :experience
                               (fn [c-experience]
                                 (update-in c-experience [:level]
                                            + 9)))
                     system (level-up-stats system e-player)
                     {:keys [spells]} (rj.e/get-c-on-e system e-player :magic)
                     spell (first spells)

                     system (level-up-stats system e-player)
                     {:keys [spells]} (rj.e/get-c-on-e system e-player :magic)]
                 (:value (first spells))
                 => (+ 2 (:value spell)))))

  (fact "level-up"
        (as-> system system
          (rj.e/upd-c system e-player :experience
                      (fn [c-experience]
                        (update-in c-experience [:experience]
                                   + (level->exp 2))))
          (let [s (level-up e-player system)]
            (:level
              (rj.e/get-c-on-e s
                               (first (rj.e/all-e-with-c s :player))
                               :experience)) => 2))))
