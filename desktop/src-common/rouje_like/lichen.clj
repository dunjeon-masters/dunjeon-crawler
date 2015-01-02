(ns rouje-like.lichen
  (:import [clojure.lang Atom])
  (:require [rouje-like.entity-wrapper :as rj.e]
            [rouje-like.utils :as rj.u]
            [rouje-like.status-effects :as rj.stef]
            [rouje-like.destructible :as rj.d]
            [rouje-like.attacker :as rj.atk]
            [rouje-like.spawnable
             :refer [defentity]]
            [rouje-like.status-effects :as rj.stef]
            [rouje-like.config :as rj.cfg]))

(declare process-input-tick)

(defentity lichen
  [[:lichen {:grow-chance% 4
             :max-blob-size 8}]
   [:position {:x (:x tile)
               :y (:y tile)
               :z (:z tile)
               :type :lichen}]
   [:destructible {:hp     (:hp  (rj.cfg/entity->stats :lichen))
                   :max-hp (:hp  (rj.cfg/entity->stats :lichen))
                   :def    (:def (rj.cfg/entity->stats :lichen))
                   :can-retaliate? true
                   :on-death-fn    nil
                   :status-effects nil
                   :take-damage-fn rj.d/take-damage}]
   [:attacker {:atk (:atk (rj.cfg/entity->stats :lichen))
               :can-attack?-fn   rj.atk/can-attack?
               :attack-fn        rj.atk/attack
               :status-effects [{:type :poison
                                 :duration 2
                                 :value 1
                                 :e-from e-this
                                 :apply-fn rj.stef/apply-poison}]
               :is-valid-target? (constantly true)}]
   [:tickable {:tick-fn process-input-tick
               :extra-tick-fn nil
               :pri 0}]
   [:broadcaster {:name-fn (constantly "the lichen")}]])

(defn get-size-of-lichen-blob
  [world origin]
  (loop [current (get-in world origin)
         explored #{}
         un-explored (into #{} (rj.u/get-neighbors-of-type world origin [:lichen]))]
    (if (empty? un-explored)
      (count explored)
      (recur (first un-explored)
             (conj explored current)
             (into (rest un-explored)
                   (remove #(or (#{current} %)
                                (explored %))
                           (rj.u/get-neighbors-of-type world
                                                  [(:x (first un-explored))
                                                   (:y (first un-explored))]
                                                  [:lichen])))))))

(defn process-input-tick
  [_ e-this system]
  (let [c-position (rj.e/get-c-on-e system e-this :position)
        {:keys [x y z]} c-position

        e-world (first (rj.e/all-e-with-c system :world))
        {:keys [levels]} (rj.e/get-c-on-e system e-world :world)
        world (nth levels z)

        {:keys [grow-chance%
                max-blob-size]} (rj.e/get-c-on-e system e-this :lichen)

        empty-neighbors (rj.u/get-neighbors-of-type world [x y]
                                                    rj.cfg/<empty>)]
    (if (and (seq empty-neighbors)
             (< (rand 100) grow-chance%)
             (< (get-size-of-lichen-blob world [x y])
                max-blob-size))
      (:system (add-lichen system (rand-nth empty-neighbors)))
      system)))
