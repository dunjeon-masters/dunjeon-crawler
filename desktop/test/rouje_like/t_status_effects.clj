(ns rouje-like.t-status-effects
  (:use [midje.sweet]
        [rouje-like.test-utils]
        [rouje-like.status-effects])
  (:require [rouje-like.utils :as rj.u :refer [?]]
            [rouje-like.core :as rj.core]
            [brute.entity :as br.e]
            [rouje-like.entity-wrapper :as rj.e]
            [rouje-like.components :as rj.c]
            [rouje-like.world :as rj.w]))

(defn check-status-duration
  [system e-this status-effect]
  (seq (filter #(= status-effect (:type %))
               (:status-effects (rj.e/get-c-on-e system e-this :destructible)))))

(let [system (start)
      system (upgrade system :maze)
      e-player (first (rj.e/all-e-with-c system :player))]

  (facts "apply-paralysis"
         (fact "success"
               (as-> system system
                     (rj.e/upd-c system e-player :destructible
                                 (fn [c-destructible]
                                   (update-in c-destructible [:status-effects]
                                              (fn [status-effects]
                                                (vec (concat status-effects
                                                             [{:type     :paralysis
                                                               :duration 2
                                                               :value    2
                                                               :e-from   e-player
                                                               :apply-fn apply-paralysis}]))))))

                     (check-status-duration
                       (apply-paralysis system e-player
                                        (first (:status-effects (rj.e/get-c-on-e system e-player :destructible))))
                       e-player :paralysis))
               => truthy)

        (fact "failure"
              (as-> system system
                    (rj.e/upd-c system e-player :destructible
                                (fn [c-destructible]
                                  (update-in c-destructible [:status-effects]
                                             (fn [status-effects]
                                               (vec (concat status-effects
                                                            [{:type     :poison
                                                              :duration 2
                                                              :value    1
                                                              :e-from   e-player
                                                              :apply-fn apply-poison}]))))))
                    (check-status-duration
                      (apply-paralysis system e-player
                                       (first (:status-effects (rj.e/get-c-on-e system e-player :destructible))))
                      e-player :paralysis))
              => falsey))

  (facts "apply-burn"
         (fact "success"
               (as-> system system
                     (rj.e/upd-c system e-player :destructible
                                 (fn [c-destructible]
                                   (update-in c-destructible [:status-effects]
                                              (fn [status-effects]
                                                (vec (concat status-effects
                                                             [{:type     :burn
                                                               :duration 2
                                                               :value    2
                                                               :e-from   e-player
                                                               :apply-fn apply-burn}]))))))

                     (check-status-duration
                       (apply-burn system e-player
                                   (first (:status-effects (rj.e/get-c-on-e system e-player :destructible))))
                       e-player :burn))
               => truthy)

         (fact "failure"
               (as-> system system
                     (rj.e/upd-c system e-player :destructible
                                 (fn [c-destructible]
                                   (update-in c-destructible [:status-effects]
                                              (fn [status-effects]
                                                (vec (concat status-effects
                                                             [{:type     :poison
                                                               :duration 2
                                                               :value    1
                                                               :e-from   e-player
                                                               :apply-fn apply-poison}]))))))
                     (check-status-duration
                       (apply-burn system e-player
                                   (first (:status-effects (rj.e/get-c-on-e system e-player :destructible))))
                       e-player :burn))
               => falsey))

  (facts "apply-poison"
         (fact "success"
               (as-> system system
                     (rj.e/upd-c system e-player :destructible
                                 (fn [c-destructible]
                                   (update-in c-destructible [:status-effects]
                                              (fn [status-effects]
                                                (vec (concat status-effects
                                                             [{:type     :poison
                                                               :duration 2
                                                               :value    2
                                                               :e-from   e-player
                                                               :apply-fn apply-poison}]))))))

                     (check-status-duration
                       (apply-poison system e-player
                                     (first (:status-effects (rj.e/get-c-on-e system e-player :destructible))))
                       e-player :poison))
               => truthy)

         (fact "failure"
               (as-> system system
                     (rj.e/upd-c system e-player :destructible
                                 (fn [c-destructible]
                                   (update-in c-destructible [:status-effects]
                                              (fn [status-effects]
                                                (vec (concat status-effects
                                                             [{:type     :burn
                                                               :duration 2
                                                               :value    1
                                                               :e-from   e-player
                                                               :apply-fn apply-burn}]))))))
                     (check-status-duration
                       (apply-burn system e-player
                                   (first (:status-effects (rj.e/get-c-on-e system e-player :destructible))))
                       e-player :poison))
               => falsey)))
