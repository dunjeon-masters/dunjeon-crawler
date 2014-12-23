(ns rouje-like.hydra-head
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

(defn add-hydra-head
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
         (add-hydra-head system target-tile)
         (recur (get-rand-tile world))))))
  ([system target-tile]
   (let [e-world (first (rj.e/all-e-with-c system :world))
         e-hydra-head (br.e/create-entity)
         hp (:hp (rj.cfg/entity->stats :hydra-head))
         system (rj.u/update-in-world system e-world [(:z target-tile) (:x target-tile) (:y target-tile)]
                                      (fn [entities]
                                        (vec
                                          (conj
                                            (remove #( rj.cfg/<walls> (:type %)) entities)
                                            (rj.c/map->Entity {:id   e-hydra-head
                                                               :type :hydra-head})))))]
     {:system (rj.e/system<<components
                system e-hydra-head
                [[:hydra-head {}]
                 [:position {:x    (:x target-tile)
                             :y    (:y target-tile)
                             :z    (:z target-tile)
                             :type :hydra-head}]
                 [:mobile {:can-move?-fn rj.m/can-move?
                           :move-fn      rj.m/move}]
                 [:sight {:distance 999}]
                 [:attacker {:atk              (:atk (rj.cfg/entity->stats :hydra-head))
                             :can-attack?-fn   rj.atk/can-attack?
                             :attack-fn        rj.atk/attack
                             :status-effects   []
                             :is-valid-target? (partial #{:player})}]
                 [:destructible {:hp         hp
                                 :max-hp     hp
                                 :def        (:def (rj.cfg/entity->stats :hydra-head))
                                 :on-death-fn nil
                                 :can-retaliate? false
                                 :take-damage-fn rj.d/take-damage
                                 :status-effects []}]
                 [:killable {:experience (:exp (rj.cfg/entity->stats :hydra-head))}]
                 [:tickable {:tick-fn process-input-tick
                             :pri 1}]
                 [:broadcaster {:name-fn (constantly "the hydra's head'")}]])
      :z (:z target-tile)})))

(defn process-input-tick
  [_ e-this system]
  (let [c-position (rj.e/get-c-on-e system e-this :position)
        this-pos [(:x c-position) (:y c-position)]
        c-mobile (rj.e/get-c-on-e system e-this :mobile)

        e-player (first (rj.e/all-e-with-c system :player))
        c-player-pos (rj.e/get-c-on-e system e-player :position)
        player-pos [(:x c-player-pos) (:y c-player-pos)]

        e-world (first (rj.e/all-e-with-c system :world))
        c-world (rj.e/get-c-on-e system e-world :world)
        levels (:levels c-world)
        level (nth levels (:z c-position))

        neighbor-tiles (rj.u/get-neighbors level [(:x c-position) (:y c-position)])

        c-sight (rj.e/get-c-on-e system e-this :sight)
        is-player-within-range? (seq (rj.u/get-neighbors-of-type-within level this-pos [:player]
                                                                        #(<= %  (:distance c-sight))))

        c-attacker (rj.e/get-c-on-e system e-this :attacker)

        target-tile (if (and (rj.u/can-see? level (:distance c-sight) this-pos player-pos)
                             is-player-within-range?)
                      (rj.u/get-closest-tile-to level this-pos (first is-player-within-range?))
                      (if (seq neighbor-tiles)
                        (rand-nth (conj neighbor-tiles nil))
                        nil))
        e-target (:id (rj.u/tile->top-entity target-tile))]
    (if (not (nil? target-tile))
      (-> (cond
            (and (< (rand-int 100) 80)
                 (can-move? c-mobile e-this target-tile system))
            (move c-mobile e-this target-tile system)

            (can-attack? c-attacker e-this e-target system)
            (attack c-attacker e-this e-target system)

            :else system)
          (rj.d/apply-effects e-this))
      system)))
