(ns rouje-like.troll
  (:require [brute.entity :as br.e]

            [rouje-like.entity-wrapper :as rj.e]
            [rouje-like.utils :as rj.u :refer [?]]
            [rouje-like.components :as rj.c :refer [can-move? move
                                                    can-attack? attack]]
            [rouje-like.mobile :as rj.m]
            [rouje-like.destructible :as rj.d]
            [rouje-like.tickable :as rj.t]
            [rouje-like.attacker :as rj.atk]
            [rouje-like.config :as rj.cfg]))

(defn add-troll
  ([{:keys [system z]}]
   (let [e-world (first (rj.e/all-e-with-c system :world))
         c-world (rj.e/get-c-on-e system e-world :world)
         levels (:levels c-world)
         world (nth levels z)

         get-rand-tile (fn [world]
                         (get-in world [(rand-int (count world))
                                        (rand-int (count (first world)))]))]
     (loop [target-tile (get-rand-tile world)]
       (if (rj.cfg/<floors> (:type (rj.u/tile->top-entity target-tile)))
         (add-troll system target-tile)
         (recur (get-rand-tile world))))))
  ([system target-tile]
   (let [e-world (first (rj.e/all-e-with-c system :world))
         e-troll (br.e/create-entity)
         system (rj.u/update-in-world system e-world [(:z target-tile) (:x target-tile) (:y target-tile)]
                                      (fn [entities]
                                        (vec
                                          (conj
                                            (remove #(#{:wall} (:type %)) entities)
                                            (rj.c/map->Entity {:id   e-troll
                                                               :type :troll})))))
         heal-fn (fn [e-this e-player e-world system]
                   (let [c-destructible (rj.e/get-c-on-e system e-this :destructible)
                         hp (:hp c-destructible)]
                     (rj.e/upd-c system e-this :destructible
                                 (fn [c-destr]
                                   (update-in c-destr [:hp]
                                              (fn [hp]
                                                (if (< hp 4)
                                                  (inc hp)
                                                  hp)))))))
         _ (? heal-fn)]
     {:system (-> (rj.e/system<<components
                    system e-troll
                    [[:troll {}]
                     [:position {:x    (:x target-tile)
                                 :y    (:y target-tile)
                                 :z    (:z target-tile)
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
                                 :pri 0}]
                     [:broadcaster {:name-fn (constantly "the troll")}]])
                  (rj.e/upd-c e-troll :tickable
                              #(assoc % :extra-tick-fn
                                      heal-fn)))
      :z (:z target-tile)})))
