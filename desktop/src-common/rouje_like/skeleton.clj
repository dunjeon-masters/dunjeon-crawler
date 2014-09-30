(ns rouje-like.skeleton
  (:require [brute.entity :as br.e]

            [rouje-like.entity :as rj.e]
            [rouje-like.utils :as rj.u]
            [rouje-like.world :as rj.wr]
            [rouje-like.components :as rj.c :refer [can-move? move
                                                    can-attack? attack]]
            [rouje-like.mobile :as rj.m]
            [rouje-like.destructible :as rj.d]
            [rouje-like.attacker :as rj.atk]))

(declare process-input-tick!)

(defn add-skeleton
  ([system]
   (let [e-world (first (rj.e/all-e-with-c system :world))
         c-world (rj.e/get-c-on-e system e-world :world)
         world (:world c-world)
         get-rand-tile (fn [world]
                         (get-in world [(rand-int (count world))
                                        (rand-int (count (first world)))]))]
     (loop [target-tile (get-rand-tile world)]
       (if (#{:floor} (:type (rj.u/get-top-entity target-tile)))
         (add-skeleton system target-tile)
         (recur (get-rand-tile world))))))
  ([system target-tile]
   (let [e-world (first (rj.e/all-e-with-c system :world))
         e-skeleton (br.e/create-entity)]
     (-> system
         (rj.wr/update-in-world e-world [(:x target-tile) (:y target-tile)]
                                (fn [entities _]
                                  (vec
                                    (conj
                                      (remove #(#{:wall} (:type %)) entities)
                                      (rj.c/map->Entity {:id   e-skeleton
                                                         :type :skeleton})))))
         (rj.e/add-c e-skeleton (rj.c/map->Skeleton {}))
         (rj.e/add-c e-skeleton (rj.c/map->Position {:x (:x target-tile)
                                                     :y (:y target-tile)
                                                     :type :skeleton}))
         (rj.e/add-c e-skeleton (rj.c/map->Mobile {:can-move?-fn rj.m/can-move?
                                                   :move-fn      rj.m/move}))
         (rj.e/add-c e-skeleton (rj.c/map->Sight {:distance 5}))
         (rj.e/add-c e-skeleton (rj.c/map->Attacker {:atk 1
                                                     :can-attack?-fn   rj.atk/can-attack?
                                                     :attack-fn        rj.atk/attack
                                                     :is-valid-target? (fn [type]
                                                                         (#{:player} type))}))
         (rj.e/add-c e-skeleton (rj.c/map->Destructible {:hp      2
                                                         :defense 1
                                                         :can-retaliate? false
                                                         :take-damage-fn rj.d/take-damage}))
         (rj.e/add-c e-skeleton (rj.c/map->Tickable {:tick-fn process-input-tick!}))))))

(defn get-closest-tile-to
  [world this-pos target-tile]
  (let [target-pos [(:x target-tile) (:y target-tile)]
        dist-from-target (rj.u/taxicab-dist this-pos target-pos)

        offset-coords-with-dir (fn [dir]
                                 (rj.u/offset-coords this-pos (rj.u/direction->offset dir)))
        shuffled-directions (shuffle [:up :down :left :right])
        offset-shuffled-directions (map #(offset-coords-with-dir %)
                                        shuffled-directions)

        is-valid-target? #{:floor :torch :gold :player}]
    (cond
      (and (< (rj.u/taxicab-dist (nth offset-shuffled-directions 0) target-pos) dist-from-target)
           (is-valid-target? (:type (rj.u/get-top-entity (get-in world (nth offset-shuffled-directions 0))))))
      (get-in world (nth offset-shuffled-directions 0))

      (and (< (rj.u/taxicab-dist (nth offset-shuffled-directions 1) target-pos) dist-from-target)
           (is-valid-target? (:type (rj.u/get-top-entity (get-in world (nth offset-shuffled-directions 1))))))
      (get-in world (nth offset-shuffled-directions 1))

      (and (< (rj.u/taxicab-dist (nth offset-shuffled-directions 2) target-pos) dist-from-target)
           (is-valid-target? (:type (rj.u/get-top-entity (get-in world (nth offset-shuffled-directions 2))))))
      (get-in world (nth offset-shuffled-directions 2))

      (and (< (rj.u/taxicab-dist (nth offset-shuffled-directions 3) target-pos) dist-from-target)
           (is-valid-target? (:type (rj.u/get-top-entity (get-in world (nth offset-shuffled-directions 3))))))
      (get-in world (nth offset-shuffled-directions 3))

      :else nil)))

(defn process-input-tick!
  [_ e-this system]
  (let [c-position (rj.e/get-c-on-e system e-this :position)
        this-pos [(:x c-position) (:y c-position)]
        c-mobile (rj.e/get-c-on-e system e-this :mobile)

        e-world (first (rj.e/all-e-with-c system :world))
        c-world (rj.e/get-c-on-e system e-world :world)
        world (:world c-world)

        neighbor-tiles (rj.u/get-neighbors world [(:x c-position) (:y c-position)])

        c-sight (rj.e/get-c-on-e system e-this :sight)
        is-player-within-range? (seq (rj.u/get-neighbors-of-type-within world this-pos [:player]
                                                                        #(<= % (:distance c-sight))))
        c-attacker (rj.e/get-c-on-e system e-this :attacker)

        target-tile (if is-player-within-range?
                      (get-closest-tile-to world this-pos (first is-player-within-range?))
                      (if (seq neighbor-tiles)
                        (rand-nth (conj neighbor-tiles nil))
                        nil))
        e-target (:id (rj.u/get-top-entity target-tile))]
    (if (not (nil? target-tile))
      (cond
        (can-move? c-mobile e-this target-tile system)
        (move c-mobile e-this target-tile system)

        (can-attack? c-attacker e-this e-target system)
        (attack c-attacker e-this e-target system)

        :else system)
      system)))
