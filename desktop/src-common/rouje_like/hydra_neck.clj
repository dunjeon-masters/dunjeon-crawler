(ns rouje-like.hydra-neck
  (:require [brute.entity :as br.e]

            [rouje-like.entity-wrapper :as rj.e]
            [rouje-like.utils :as rj.u :refer [?]]
            [rouje-like.components :as rj.c :refer [can-move? move
                                                    can-attack? attack]]
            [rouje-like.mobile :as rj.m]
            [rouje-like.messaging :as rj.msg]
            [rouje-like.destructible :as rj.d]
            [rouje-like.attacker :as rj.atk]
            [rouje-like.status-effects :as rj.stef]
            [rouje-like.config :as rj.cfg]
            [clojure.set :refer [union]]))

(declare process-input-tick)

(defn add-hydra-neck
  ([{:keys [system z]}]
   (let [e-world (first (rj.e/all-e-with-c system :world))
         c-world (rj.e/get-c-on-e system e-world :world)
         levels (:levels c-world)
         world (nth levels z)
         e-head (first (rj.e/all-e-with-c system :hydra-head))
         c-head-pos (rj.e/get-c-on-e system e-head :position)
         head-pos [(:x c-head-pos) (:y c-head-pos)]
         head-neighbors (rj.u/get-neighbors-of-type world head-pos [:dune])
         n-pos (first head-neighbors)

         get-head-tile (fn [world]
                         (get-in world [(:x n-pos)
                                        (:y n-pos)]
                                 nil))]
     (loop [target-tile (get-head-tile world)]
       (add-hydra-neck system target-tile))))
  ([system target-tile]
   (let [e-world (first (rj.e/all-e-with-c system :world))
         e-hydra-neck (br.e/create-entity)
         hp (:hp (rj.cfg/entity->stats :hydra-neck))
         _ (? target-tile)
         system (rj.u/update-in-world system e-world [(:z target-tile) (:x target-tile) (:y target-tile)]
                                      (fn [entities]
                                        (vec
                                          (conj
                                            (remove #(rj.cfg/<walls> (:type %)) entities)
                                            (rj.c/map->Entity {:id   e-hydra-neck
                                                               :type :hydra-neck})))))]
     {:system (rj.e/system<<components
                system e-hydra-neck
                [[:hydra-neck {}]
                 [:position {:x    (:x target-tile)
                             :y    (:y target-tile)
                             :z    (:z target-tile)
                             :type :hydra-neck}]
                 [:mobile {:can-move?-fn rj.m/can-move?
                           :move-fn      rj.m/move}]
                 [:sight {:distance 100}]                     ;;4
                 [:attacker {:atk              (:atk (rj.cfg/entity->stats :hydra-neck))
                             :can-attack?-fn   rj.atk/can-attack?
                             :attack-fn        rj.atk/attack
                             :status-effects   []
                             :is-valid-target? (partial #{:hydra-head})}]
                 [:destructible {:hp         hp
                                 :max-hp     hp
                                 :def        (:def (rj.cfg/entity->stats :hydra-neck))
                                 :can-retaliate? false
                                 :take-damage-fn rj.d/take-damage
                                 :on-death-fn nil
                                 :status-effects []}]
                 [:killable {:experience (:exp (rj.cfg/entity->stats :hydra-neck))}]
                 [:tickable {:tick-fn process-input-tick
                             :pri 0}]
                 [:broadcaster {:name-fn (constantly "the hydra's neck")}]])
      :z (:z target-tile)})))

(defn process-input-tick
  [_ e-this system]
  (as-> (let [c-position (rj.e/get-c-on-e system e-this :position)
              this-pos [(:x c-position) (:y c-position)]
              c-mobile (rj.e/get-c-on-e system e-this :mobile)
              e-world (first (rj.e/all-e-with-c system :world))
              e-player (first (rj.e/all-e-with-c system :hydra-head))]
              (if (nil? e-player)
                (as-> system system
                      (rj.u/update-in-world system e-world [(:z c-position) (:x c-position) (:y c-position)]
                                            (fn [entities]
                                              (vec
                                                (remove
                                                  #(#{e-this} (:id %))
                                                  entities))))
                      (rj.e/kill-e system e-this))
                (let [c-player-pos (rj.e/get-c-on-e system e-player :position)
                      player-pos [(:x c-player-pos) (:y c-player-pos)]

                      e-world (first (rj.e/all-e-with-c system :world))
                      c-world (rj.e/get-c-on-e system e-world :world)
                      levels (:levels c-world)
                      level (nth levels (:z c-position))

                      neighbor-tiles (rj.u/get-neighbors level [(:x c-position) (:y c-position)])

                      c-sight (rj.e/get-c-on-e system e-this :sight)
                      is-player-within-range? (seq (rj.u/get-neighbors-of-type-within level this-pos [:hydra-head]
                                                                                      #(<= %  (:distance c-sight))))


                      target-tile (if (and (rj.u/can-see? level (:distance c-sight) this-pos player-pos)
                                           is-player-within-range?)
                                    (rj.u/get-closest-tile-to level this-pos (first is-player-within-range?))
                                    (if (seq neighbor-tiles)
                                      (rand-nth (conj neighbor-tiles nil))
                                      nil))]
                  (if (not (nil? target-tile))
                    (cond
                      (can-move? c-mobile e-this target-tile system)
                      (move c-mobile e-this target-tile system)

                      :else system)
                    system)))
          ) system
        (let [c-position (rj.e/get-c-on-e system e-this :position)
              this-pos [(:x c-position) (:y c-position)]
              c-mobile (rj.e/get-c-on-e system e-this :mobile)
              e-player (first (rj.e/all-e-with-c system :hydra-head))]
              (if (nil? e-player)
                system
                (let [c-player-pos (rj.e/get-c-on-e system e-player :position)
                      player-pos [(:x c-player-pos) (:y c-player-pos)]

                      e-world (first (rj.e/all-e-with-c system :world))
                      c-world (rj.e/get-c-on-e system e-world :world)
                      levels (:levels c-world)
                      level (nth levels (:z c-position))

                      neighbor-tiles (rj.u/get-neighbors level [(:x c-position) (:y c-position)])

                      c-sight (rj.e/get-c-on-e system e-this :sight)
                      is-player-within-range? (seq (rj.u/get-neighbors-of-type-within level this-pos [:hydra-head]
                                                                                      #(<= %  (:distance c-sight))))


                      target-tile (if (and (rj.u/can-see? level (:distance c-sight) this-pos player-pos)
                                           is-player-within-range?)
                                    (rj.u/get-closest-tile-to level this-pos (first is-player-within-range?))
                                    (if (seq neighbor-tiles)
                                      (rand-nth (conj neighbor-tiles nil))
                                      nil))]
                  (if (not (nil? target-tile))
                    (cond
                      (can-move? c-mobile e-this target-tile system)
                      (move c-mobile e-this target-tile system)
                      :else system)
                    system)))
          )
        system))

