(ns rouje-like.hydra-tail
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

(defn add-hydra-tail
  ([{:keys [system z]}]
   (let [e-world (first (rj.e/all-e-with-c system :world))
         c-world (rj.e/get-c-on-e system e-world :world)
         levels (:levels c-world)
         world (nth levels z)
         e-neck (first (rj.e/all-e-with-c system :hydra-neck))
         c-neck-pos (rj.e/get-c-on-e system e-neck :position)
         neck-pos [(:x c-neck-pos) (:y c-neck-pos)]
         neck-neighbors (rj.u/get-neighbors-of-type world neck-pos [:floor :maze-wall])
         n-pos (first neck-neighbors)

         get-neck-tile (fn [world]
                         (get-in world [(:x n-pos)
                                        (:y n-pos)]))]
     (loop [target-tile (get-neck-tile world)]
       (add-hydra-tail system target-tile))))
  ([system target-tile]
   (let [e-world (first (rj.e/all-e-with-c system :world))
         e-hydra-tail (br.e/create-entity)
         hp (:hp rj.cfg/hydra-tail-stats)
         system (rj.u/update-in-world system e-world [(:z target-tile) (:x target-tile) (:y target-tile)]
                                      (fn [entities]
                                        (vec
                                          (conj
                                            (remove #( rj.cfg/<walls> (:type %)) entities)
                                            (rj.c/map->Entity {:id   e-hydra-tail
                                                               :type :hydra-tail})))))]
     {:system (rj.e/system<<components
                system e-hydra-tail
                [[:hydra-tail {}]
                 [:position {:x    (:x target-tile)
                             :y    (:y target-tile)
                             :z    (:z target-tile)
                             :type :hydra-tail}]
                 [:mobile {:can-move?-fn rj.m/can-move?
                           :move-fn      rj.m/move}]
                 [:sight {:distance 100}]                     ;;4
                 [:attacker {:atk              (:atk rj.cfg/hydra-tail-stats)
                             :can-attack?-fn   rj.atk/can-attack?
                             :attack-fn        rj.atk/attack
                             :status-effects   []
                             :is-valid-target? (partial #{:hydra-neck})}]
                 [:destructible {:hp         hp
                                 :max-hp     hp
                                 :def        (:def rj.cfg/hydra-tail-stats)
                                 :can-retaliate? false
                                 :take-damage-fn rj.d/take-damage
                                 :status-effects []}]
                 [:killable {:experience (:exp rj.cfg/hydra-tail-stats)}]
                 [:tickable {:tick-fn process-input-tick
                             :pri -1}]
                 [:broadcaster {:name-fn (constantly "the hydra's tail")}]])
      :z (:z target-tile)})))

(defn get-closest-tile-to
  [level this-pos target-tile]
  (let [target-pos [(:x target-tile) (:y target-tile)]
        dist-from-target (rj.u/taxicab-dist this-pos target-pos)

        this-pos+dir-offset (fn [this-pos dir]
                              (rj.u/coords+offset this-pos (rj.u/direction->offset dir)))
        shuffled-directions (shuffle [:up :down :left :right])
        offset-shuffled-directions (map #(this-pos+dir-offset this-pos %)
                                        shuffled-directions)

        is-valid-target-tile? rj.cfg/<valid-mob-targets>

        nth->offset-pos (fn [index]
                          (nth offset-shuffled-directions index))
        isa-closer-tile? (fn [target-pos+offset]
                           (and (< (rj.u/taxicab-dist target-pos+offset target-pos)
                                   dist-from-target)
                                (is-valid-target-tile?
                                  (:type (rj.u/tile->top-entity
                                           (get-in level target-pos+offset))))))]
    (cond
      (isa-closer-tile? (nth->offset-pos 0))
      (get-in level (nth->offset-pos 0))

      (isa-closer-tile? (nth->offset-pos 1))
      (get-in level (nth->offset-pos 1))

      (isa-closer-tile? (nth->offset-pos 2))
      (get-in level (nth->offset-pos 2))

      (isa-closer-tile? (nth->offset-pos 3))
      (get-in level (nth->offset-pos 3))

      :else nil)))

(defn process-input-tick
  [_ e-this system]
  (as-> (let [c-position (rj.e/get-c-on-e system e-this :position)
              this-pos [(:x c-position) (:y c-position)]
              c-mobile (rj.e/get-c-on-e system e-this :mobile)
              e-world (first (rj.e/all-e-with-c system :world))
              e-player (first (rj.e/all-e-with-c system :hydra-neck))]
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
                  is-player-within-range? (seq (rj.u/get-neighbors-of-type-within level this-pos [:hydra-neck]
                                                                                  #(<= %  (:distance c-sight))))


                  target-tile (if (and (rj.u/can-see? level (:distance c-sight) this-pos player-pos)
                                       is-player-within-range?)
                                (get-closest-tile-to level this-pos (first is-player-within-range?))
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
              e-player (first (rj.e/all-e-with-c system :hydra-neck))]
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
                  is-player-within-range? (seq (rj.u/get-neighbors-of-type-within level this-pos [:hydra-neck]
                                                                                  #(<= %  (:distance c-sight))))


                  target-tile (if (and (rj.u/can-see? level (:distance c-sight) this-pos player-pos)
                                       is-player-within-range?)
                                (get-closest-tile-to level this-pos (first is-player-within-range?))
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
