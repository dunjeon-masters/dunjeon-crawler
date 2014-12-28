(ns rouje-like.t-destructible
  (:use [midje.sweet]
        [rouje-like.test-utils]
        [rouje-like.destructible])
  (:require [rouje-like.entity-wrapper :as rj.e]
            [rouje-like.core :as rj.core]
            [rouje-like.snake :as rj.snk]
            [rouje-like.utils
             :refer [?]]

            [brute.entity :as br.e]))


(fact "merge-by-with?"
      (merge-by-with [{:t 1, :v 33}
                      {:t 1, :v 20} {:t 2, :v 99}]
                     [{:t 1, :v 35} {:t 3, :v 100}]
                     :t (pick-by-with
                          :v >))
      => (just {:t 3 :v 100}
               {:t 1 :v 35}
               {:t 2 :v 99})

      (merge-by-with [{:t "x"} {:t "ab"}]
                     [{:t "y"} {:t "abc"}]
                     (comp count :t)
                     (pick-by-with
                       :t (fn [a b]
                            (= "x" a))))
      => (just {:t "abc"}
               {:t "x"}
               {:t "ab"}))

(fact "pick-by-with"
      ((pick-by-with
         :t #(> %1 %2))
       {:t 9} {:t 5})
      => {:t 9}
      ((pick-by-with
         (comp count :t) >)
       {:t "foo"} {:t "bar"})
      => {:t "bar"})

(fact "pick-by-with-"
      ((pick-by-with-
         :t max)
       {:t 5} {:t 9})
      => {:t 9}

      ((pick-by-with-
         (comp count :t) max)
       {:t "foo"} {:t "foobar"})
      => {:t "foobar"}

      ((pick-by-with-
         :t (constantly 42))
       {:t 3} {:t 3})
      => nil

      ((pick-by-with-
        :t (constantly 42) :dflt)
       {:t 3} {:t 3})
      => :dflt)

(facts "add-effects & apply-effects"
       (let [system (as-> (start) system
                      (rj.snk/add-snake {:system system :z 1})
                      (:system system))
             e-player (first (rj.e/all-e-with-c system :player))
             e-snake (first (rj.e/all-e-with-c system :snake))
             system (add-effects system e-player e-snake)]
         (fact "add-effects: clean slate, get poison"
               e-snake  => truthy
               e-player => truthy
               (first
                 (filter #(#{:poison} (:type %))
                         (:status-effects
                           (rj.e/get-c-on-e system e-player :destructible))))
               => (contains {:type :poison}))
         (fact "add-effects: had poison, should upg poison"
               (let [system (rj.e/upd-c system e-snake :attacker
                                        (fn [c-attacker]
                                          (update-in c-attacker [:status-effects]
                                                     (fn [se]
                                                       [(update-in (first se) [:value]
                                                                   + 3)]))))
                     system (add-effects system e-player e-snake)]
                 (first
                   (filter #(#{:poison} (:type %))
                           (:status-effects
                             (rj.e/get-c-on-e system e-player :destructible))))
                 => (contains {:type :poison
                               :value 5})))
         (fact "add-effects: had burn, should get poison & upg burn"
               (let [system (rj.e/upd-c system e-snake :attacker
                                        (fn [c-attacker]
                                          (update-in c-attacker [:status-effects]
                                                     #(conj % {:type :burn
                                                               :value 3
                                                               :duration 2}))))
                     system (rj.e/upd-c system e-player :destructible
                                        (fn [c-destructible]
                                          (assoc c-destructible :status-effects
                                                 [{:type :burn
                                                   :value 2
                                                   :duration 1}])))
                     system (add-effects system e-player e-snake)]
                 (:status-effects (rj.e/get-c-on-e system e-player :destructible)))
               => (fn [status-effects]
                    (let [{burn-v :value
                           burn-d :duration} (first (filter #(#{:burn} (:type %)) status-effects))
                          {pois-v :value
                           pois-d :duration} (first (filter #(#{:poison} (:type %)) status-effects))]
                      (and (= [burn-v burn-d] [3 2])
                           (= [pois-v pois-d] [2 4])))))
         (let [system (apply-effects system e-player)
               {:keys [hp max-hp]} (rj.e/get-c-on-e
                                     system e-player :destructible)]
           (fact "apply-effects: apply poison to player"
                 hp => (- max-hp 2)))))

(facts "take-damage"
       (let [system (start)
             system (:system
                      (rj.snk/add-snake {:system system
                                         :z 1}))
             e-player (first (rj.e/all-e-with-c system :player))
             c-destructible (rj.e/get-c-on-e system e-player :destructible)
             e-attacker (first (rj.e/all-e-with-c system :snake))
             c-attacker (rj.e/get-c-on-e system e-attacker :attacker)

             damage (inc (:atk c-attacker))
             system (take-damage c-destructible e-player damage e-attacker system)
             {:keys [hp max-hp]} (rj.e/get-c-on-e system e-player :destructible)]
         (fact "take-damage: lose hp"
               e-player => truthy
               e-attacker => truthy
               hp => (roughly (- max-hp damage) damage))
         (fact "take-damage: die"
               (let [system (rj.e/upd-c system e-player :destructible
                                        (fn [c-destructible]
                                          (assoc c-destructible :hp 1)))
                     c-destructible (rj.e/get-c-on-e system e-player :destructible)
                     system (take-damage c-destructible e-player damage e-attacker system)]
                 (rj.e/get-c-on-e system e-player :destructible)) => nil)))
