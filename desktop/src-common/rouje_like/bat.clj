(ns rouje-like.bat
  (:require [brute.entity :as br.e]

            [rouje-like.components :as rj.c]
            [rouje-like.entity :as rj.e]
            [rouje-like.utils :as rj.u]
            [rouje-like.world :as rj.wr]
            [rouje-like.destructible :as rj.d]))

(declare process-input-tick!)

(defn can-move?
  [_ _ target]
  (#{:floor :gold :torch} (:type (rj.u/get-top-entity target))))

(defn move
  [system e-this target-tile]
  (let [c-position (rj.e/get-c-on-e system e-this :position)
        e-world (first (rj.e/all-e-with-c system :world))
        bat-pos [(:x c-position) (:y c-position)]
        target-pos [(:x target-tile) (:y target-tile)]]
    (-> system
        (rj.wr/update-in-world e-world target-pos
                               (fn [entities _]
                                 (vec (conj entities
                                            (rj.c/map->Entity {:type :bat
                                                               :id   e-this})))))

        (rj.wr/update-in-world e-world bat-pos
                               (fn [entities _]
                                 (vec (remove #(#{:bat} (:type %))
                                              entities))))

        (rj.e/upd-c e-this :position
                    (fn [c-position]
                      (-> c-position
                          (assoc-in [:x] (:x target-tile))
                          (assoc-in [:y] (:y target-tile))))))))

(defn add-bat
  ([system]
   (let [e-world (first (rj.e/all-e-with-c system :world))
         c-world (rj.e/get-c-on-e system e-world :world)
         world (:world c-world)
         get-rand-tile (fn [world]
                         (get-in world [(rand-int (count world))
                                        (rand-int (count (first world)))]))]
     (loop [target-tile (get-rand-tile world)]
       (if (#{:wall} (:type (rj.u/get-top-entity target-tile)))
         (recur (get-rand-tile world))
         (add-bat system target-tile)))))
  ([system target-tile]
   (let [e-world (first (rj.e/all-e-with-c system :world))
         e-bat (br.e/create-entity)]
     (-> system
         (rj.wr/update-in-world e-world [(:x target-tile) (:y target-tile)]
                                (fn [entities _]
                                  (vec
                                    (conj
                                      (remove #(#{:wall} (:type %)) entities)
                                      (rj.c/map->Entity {:id   e-bat
                                                         :type :bat})))))
         (rj.e/add-c e-bat (rj.c/map->Bat {}))
         (rj.e/add-c e-bat (rj.c/map->Position {:x (:x target-tile)
                                                :y (:y target-tile)}))
         (rj.e/add-c e-bat (rj.c/map->Mobile {:can-move? can-move?
                                              :move      move}))
         (rj.e/add-c e-bat (rj.c/map->Destructible {:hp      1
                                                    :defense 1
                                                    :take-damage-fn rj.d/take-damage}))
         (rj.e/add-c e-bat (rj.c/map->Tickable {:tick-fn process-input-tick!}))))))

(defn process-input-tick!
  [_ e-this system]
  (let [c-position (rj.e/get-c-on-e system e-this :position)

        e-world (first (rj.e/all-e-with-c system :world))
        c-world (rj.e/get-c-on-e system e-world :world)
        world (:world c-world)

        neighbor-tiles (rj.u/get-neighbors world [(:x c-position) (:y c-position)])

        target-tile (if (seq neighbor-tiles)
                 (rand-nth (conj neighbor-tiles nil))
                 nil)]
    (if (not (nil? target-tile))
      (cond
        ((:can-move? (rj.e/get-c-on-e system e-this :mobile)) system e-this target-tile)
        ((:move (rj.e/get-c-on-e system e-this :mobile)) system e-this target-tile)

        :else system)
      system)))
