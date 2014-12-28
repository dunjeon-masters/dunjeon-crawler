(ns rouje-like.large-amoeba
  (:require [brute.entity :as br.e]

            [rouje-like.entity-wrapper :as rj.e]
            [rouje-like.utils :as rj.u :refer [?]]
            [rouje-like.components :as rj.c :refer [can-move? move
                                                    can-attack? attack]]
            [rouje-like.mobile :as rj.m]
            [rouje-like.destructible :as rj.d]
            [rouje-like.attacker :as rj.atk]
            [rouje-like.tickable :as rj.t]
            [rouje-like.config :as rj.cfg]))

(defn add-large-amoeba
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
         (add-large-amoeba system target-tile)
         (recur (get-rand-tile world))))))
  ([system target-tile]
   (let [e-world (first (rj.e/all-e-with-c system :world))
         e-large-amoeba (br.e/create-entity)
         system (rj.u/update-in-world system e-world [(:z target-tile) (:x target-tile) (:y target-tile)]
                                      (fn [entities]
                                        (vec
                                          (conj
                                            (remove #(#{:wall} (:type %)) entities)
                                            (rj.c/map->Entity {:id   e-large-amoeba
                                                               :type :large-amoeba})))))]
     {:system (rj.e/system<<components
                system e-large-amoeba
                [[:large-amoeba {}]
                 [:position {:x    (:x target-tile)
                             :y    (:y target-tile)
                             :z    (:z target-tile)
                             :type :large-amoeba}]
                 [:mobile {:can-move?-fn rj.m/can-move?
                           :move-fn      rj.m/move}]
                 [:sight {:distance 3}]
                 [:attacker {:atk              (:atk (rj.cfg/entity->stats :large-amoeba))
                             :can-attack?-fn   rj.atk/can-attack?
                             :attack-fn        rj.atk/attack
                             :status-effects   []
                             :is-valid-target? (partial #{:player})}]
                 [:destructible {:hp         (:hp  (rj.cfg/entity->stats :large-amoeba))
                                 :max-hp     (:hp  (rj.cfg/entity->stats :large-amoeba))
                                 :def        (:def (rj.cfg/entity->stats :large-amoeba))
                                 :can-retaliate? false
                                 :status-effects []
                                 :on-death-fn nil
                                 :take-damage-fn rj.d/take-damage}]
                 [:killable {:experience (:exp (rj.cfg/entity->stats :large-amoeba))}]
                 [:tickable {:tick-fn rj.t/process-input-tick
                             :pri 0}]
                 [:broadcaster {:name-fn (constantly "the large-amoeba")}]])
      :z (:z target-tile)})))
