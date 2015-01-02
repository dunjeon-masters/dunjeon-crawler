(ns rouje-like.troll
  (:require [rouje-like.mobile :as rj.m]
            [rouje-like.spawnable
             :refer [defentity]]
            [rouje-like.destructible :as rj.d]
            [rouje-like.entity-wrapper :as rj.e]
            [rouje-like.tickable :as rj.t]
            [rouje-like.attacker :as rj.atk]
            [rouje-like.config :as rj.cfg]))

(defn heal-fn
  [e-this _ _ system]
  (let [c-destructible (rj.e/get-c-on-e system e-this :destructible)
        hp (:hp c-destructible)]
    (rj.e/upd-c system e-this :destructible
                (fn [c-destr]
                  (update-in c-destr [:hp]
                             (fn [hp]
                               (if (< hp 4)
                                 (inc hp)
                                 hp)))))))

(defentity troll
  [{:keys [system z]}]
  [[:troll {}]
   [:position {:x    (:x tile)
               :y    (:y tile)
               :z    (:z tile)
               :type :troll}]
   [:mobile {:can-move?-fn rj.m/can-move?
             :move-fn      rj.m/move}]
   [:sight {:distance 7}]
   [:attacker {:atk              (:atk (rj.cfg/entity->stats :troll))
               :status-effects []
               :can-attack?-fn   rj.atk/can-attack?
               :attack-fn        rj.atk/attack
               :is-valid-target? (partial #{:player})}]
   [:destructible {:hp         (:hp  (rj.cfg/entity->stats :troll))
                   :max-hp     (:hp  (rj.cfg/entity->stats :troll))
                   :def        (:def (rj.cfg/entity->stats :troll))
                   :can-retaliate? false
                   :status-effects []
                   :on-death-fn nil
                   :take-damage-fn rj.d/take-damage}]
   [:killable {:experience (:exp (rj.cfg/entity->stats :troll))}]
   [:tickable {:tick-fn rj.t/process-input-tick
               :extra-tick-fn heal-fn
               :pri 0}]
   [:broadcaster {:name-fn (constantly "the troll")}]])
