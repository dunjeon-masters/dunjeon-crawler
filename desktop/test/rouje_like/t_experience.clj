(ns rouje-like.t-experience
  (:use [midje.sweet]
        [rouje-like.experience])
  (:require [rouje-like.core :as rj.core]
            [brute.entity :as br.e]
            [rouje-like.entity-wrapper :as rj.e]
            [rouje-like.utils :refer :all]
            [rouje-like.components :as rj.c :refer [->3DPoint]]
            [rouje-like.world :as rj.w]
            [rouje-like.config :as rj.cfg]))

(defn get-system []
  (with-open [w (clojure.java.io/writer "NUL")]
    (binding [*out* w]
      (-> (br.e/create-system)
          (rj.core/init-entities {})))))

(let [system (get-system)
      e-player (first (rj.e/all-e-with-c system :player))]

  (fact "level->exp"
        (level->exp 5) => (* 5 (:exp rj.cfg/level-exp))
        (level->exp 7) => (* 7 (:exp rj.cfg/level-exp)))

  (fact "wrand"
        (wrand [2 2 1]) =future=> "ANTHONY WILL DO THIS")

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

         #_(fact "successfully upgrade a spell at level 10"
               (as-> system system
                     (let [c-magic (rj.e/get-c-on-e system e-player :magic)
                           spells (:spells c-magic)
                           spell (spells 0)]
                       (rj.e/upd-c system e-player :experience
                                   (fn [c-experience]
                                     (update-in c-experience [:level]
                                                + 9)))

                       (let [s (level-up-stats system e-player)]
                         (? (:value ((:spells
                                  (rj.e/get-c-on-e s
                                                   (first (rj.e/all-e-with-c s :player))
                                                   :magic)) 0))))
                       =future=> (+ 2 (:value spell))))))

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
