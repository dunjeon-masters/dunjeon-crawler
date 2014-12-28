(ns rouje-like.mimic
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

(defn- mimic:take-damage
  [c-this e-this damage e-from system]
  (as-> (rj.d/take-damage c-this e-this damage e-from system) system
    (if (rj.e/get-c-on-e system e-this :mimic)
      (as-> system system
        (rj.e/upd-c system e-this :position
                    (fn [c-position]
                      (assoc c-position :type :mimic)))
        (let [c-position (rj.e/get-c-on-e system e-this :position)
              e-world (first (rj.e/all-e-with-c system :world))]
          (rj.u/update-in-world system e-world
                                [(:z c-position) (:x c-position) (:y c-position)]
                                (fn [entities]
                                  (vec
                                    (conj (remove
                                            #(#{e-this} (:id %))
                                            entities)
                                          (rj.c/map->Entity {:id e-this
                                                             :type :mimic})))))))
      system)))

(defn add-mimic
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
         (add-mimic system target-tile)
         (recur (get-rand-tile world))))))
  ([system target-tile]
   (let [e-world (first (rj.e/all-e-with-c system :world))
         e-mimic (br.e/create-entity)
         system (rj.u/update-in-world system e-world
                                      [(:z target-tile) (:x target-tile) (:y target-tile)]
                                      (fn [entities]
                                        (vec
                                          (conj
                                            (remove #(#{:wall} (:type %)) entities)
                                            (rj.c/map->Entity {:id   e-mimic
                                                               :type :hidden-mimic})))))]
     {:system (rj.e/system<<components
                system e-mimic
                [[:mimic {}]
                 [:position {:x    (:x target-tile)
                             :y    (:y target-tile)
                             :z    (:z target-tile)
                             :type :hidden-mimic}]
                 [:mobile {:can-move?-fn (fn [c e t s]
                                           (let [{type :type}
                                                 (rj.e/get-c-on-e s e :position)]
                                             (if (#{:mimic} type)
                                               (rj.m/can-move? c e t s)
                                               false)))
                           :move-fn      rj.m/move}]
                 [:sight {:distance 4}]
                 [:attacker {:atk              (:atk (rj.cfg/entity->stats :mimic))
                             :status-effects []
                             :can-attack?-fn   (fn [c e t s]
                                                 (let [{type :type}
                                                       (rj.e/get-c-on-e s e :position)]
                                                   (if (#{:mimic} type)
                                                     (rj.atk/can-attack? c e t s)
                                                     false)))
                             :attack-fn        rj.atk/attack
                             :is-valid-target? (partial #{:player})}]
                 [:destructible {:hp         (:hp  (rj.cfg/entity->stats :mimic))
                                 :max-hp     (:hp  (rj.cfg/entity->stats :mimic))
                                 :def        (:def (rj.cfg/entity->stats :mimic))
                                 :can-retaliate? false
                                 :status-effects []
                                 :on-death-fn nil
                                 :take-damage-fn mimic:take-damage}]
                 [:killable {:experience (:exp (rj.cfg/entity->stats :mimic))}]
                 [:tickable {:tick-fn rj.t/process-input-tick
                             :pri 0}]
                 [:broadcaster {:name-fn (constantly "the mimic")}]])
      :z (:z target-tile)})))
