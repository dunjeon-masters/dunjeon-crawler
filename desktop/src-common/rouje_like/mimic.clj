(ns rouje-like.mimic
  (:require [rouje-like.entity-wrapper :as rj.e]
            [rouje-like.utils :as rj.u :refer [?]]
            [rouje-like.components :as rj.c
             :refer [can-move? move
                     can-attack? attack
                     ->3DPoint]]
            [rouje-like.mobile :as rj.m]
            [rouje-like.spawnable
             :refer [defentity]]
            [rouje-like.destructible :as rj.d]
            [rouje-like.tickable :as rj.t]
            [rouje-like.attacker :as rj.atk]
            [rouje-like.config :as rj.cfg]))

(defn- mimic:take-damage
  [c-this e-this damage e-from system]
  (as-> (rj.d/take-damage c-this e-this damage e-from system) system
    (if (rj.e/get-c-on-e system e-this :mimic)
      (as-> system system
        (rj.e/upd-c system e-this :position
                    (fn [c-position]
                      (assoc c-position :type :visible-mimic)))
        (let [c-position (rj.e/get-c-on-e system e-this :position)
              e-world (first (rj.e/all-e-with-c system :world))]
          (rj.u/update-in-world
            system e-world
            (->3DPoint c-position)
            (fn [entities]
              (vec
                (conj (remove
                        #(#{e-this} (:id %))
                        entities)
                      (rj.c/map->Entity {:id e-this
                                         :type :visible-mimic})))))))
      system)))

(defentity mimic
  [{:keys [system z]}]
  [[:mimic {}]
   [:position {:x    (:x tile)
               :y    (:y tile)
               :z    (:z tile)
               :type :mimic}]
   [:mobile {:can-move?-fn (fn [c e t s]
                             (let [{type :type}
                                   (rj.e/get-c-on-e s e :position)]
                               (when (#{:visible-mimic} type)
                                 (rj.m/can-move? c e t s))))
             :move-fn      rj.m/move}]
   [:sight {:distance 4}]
   [:attacker {:atk              (:atk (rj.cfg/entity->stats :mimic))
               :status-effects   []
               :can-attack?-fn   (fn [c e t s]
                                   (let [{type :type}
                                         (rj.e/get-c-on-e s e :position)]
                                     (when (#{:visible-mimic} type)
                                       (rj.atk/can-attack? c e t s))))
               :attack-fn        rj.atk/attack
               :is-valid-target? #{:player}}]
   [:destructible {:hp         (:hp  (rj.cfg/entity->stats :mimic))
                   :max-hp     (:hp  (rj.cfg/entity->stats :mimic))
                   :def        (:def (rj.cfg/entity->stats :mimic))
                   :can-retaliate? false
                   :status-effects []
                   :on-death-fn nil
                   :take-damage-fn mimic:take-damage}]
   [:killable {:experience (:exp (rj.cfg/entity->stats :mimic))}]
   [:tickable {:tick-fn rj.t/process-input-tick
               :extra-tick-fn nil
               :pri 0}]
   [:broadcaster {:name-fn (constantly "the mimic")}]])
