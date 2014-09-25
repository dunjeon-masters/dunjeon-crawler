(ns rouje-like.bat
  (:require [brute.entity :as br.e]

            [rouje-like.components :as rj.c]
            [rouje-like.entity :as rj.e]
            [rouje-like.utils :as rj.u]
            [rouje-like.world :as rj.wr]))

(declare process-input-tick!)

(defn take-damage
  [system this damage _]
  (let [c-destructible (rj.e/get-c-on-e system this :destructible)
        hp (:hp c-destructible)

        c-position (rj.e/get-c-on-e system this :position)

        e-world (first (rj.e/all-e-with-c system :world))]
    (if (pos? (- hp damage))
      (-> system
          (rj.e/upd-c this :destructible
                      (fn [c-destructible]
                        (update-in c-destructible [:hp] - damage))))
      (-> system
          (rj.wr/update-in-world e-world [(:x c-position) (:y c-position)]
                                 (fn [entities _]
                                   (vec
                                     (remove
                                       #(#{:bat} (:type %))
                                       entities))))
          (rj.e/kill-e this)))))

(defn can-move?
  [_ _ target]
  (#{:floor :gold :torch} (:type (rj.u/get-top-entity target))))

(defn move
  [system this target]
  (let [c-position (rj.e/get-c-on-e system this :position)
        e-world (first (rj.e/all-e-with-c system :world))
        bat-pos [(:x c-position) (:y c-position)]
        target-pos [(:x target) (:y target)]]
    (-> system
        (rj.wr/update-in-world e-world target-pos
                               (fn [entities _]
                                 (vec (conj entities
                                            (rj.c/map->Entity {:type :bat
                                                               :id   this})))))

        (rj.wr/update-in-world e-world bat-pos
                               (fn [entities _]
                                 (vec (remove #(#{:bat} (:type %))
                                              entities))))

        (rj.e/upd-c this :position
                    (fn [c-position]
                      (-> c-position
                          (assoc-in [:x] (:x target))
                          (assoc-in [:y] (:y target))))))))

(defn add-bat
  ([system]
   (let [e-world (first (rj.e/all-e-with-c system :world))
         c-world (rj.e/get-c-on-e system e-world :world)
         world (:world c-world)
         get-rand-tile (fn [world]
                         (get-in world [(rand-int (count world))
                                        (rand-int (count (first world)))]))]
     (loop [target (get-rand-tile world)]
       (if (#{:wall} (:type (rj.u/get-top-entity target)))
         (recur (get-rand-tile world))
         (add-bat system target)))))
  ([system target]
   (let [e-world (first (rj.e/all-e-with-c system :world))
         e-bat (br.e/create-entity)]
     (-> system
         (rj.wr/update-in-world e-world [(:x target) (:y target)]
                                (fn [entities _]
                                  (vec
                                    (conj
                                      (remove #(#{:wall} (:type %)) entities)
                                      (rj.c/map->Entity {:id   e-bat
                                                         :type :bat})))))
         (rj.e/add-c e-bat (rj.c/map->Bat {}))
         (rj.e/add-c e-bat (rj.c/map->Position {:x (:x target)
                                                :y (:y target)}))
         (rj.e/add-c e-bat (rj.c/map->Mobile {:can-move? can-move?
                                              :move      move}))
         (rj.e/add-c e-bat (rj.c/map->Destructible {:hp      1
                                                    :defense 1
                                                    :take-damage take-damage}))
         (rj.e/add-c e-bat (rj.c/map->Tickable {:tick-fn process-input-tick!
                                                :args    nil}))))))

(defn process-input-tick!
  [system this _]
  (let [c-position (rj.e/get-c-on-e system this :position)

        e-world (first (rj.e/all-e-with-c system :world))
        c-world (rj.e/get-c-on-e system e-world :world)
        world (:world c-world)

        targets (rj.u/get-neighbors world [(:x c-position) (:y c-position)])

        target (if (seq targets)
                 (rand-nth (conj targets nil))
                 nil)]
    (if (not (nil? target))
      (cond
        ((:can-move? (rj.e/get-c-on-e system this :mobile)) system this target)
        ((:move (rj.e/get-c-on-e system this :mobile)) system this target)

        :else system)
      system)))
