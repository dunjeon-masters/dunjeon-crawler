(ns rouje-like.lichen
  (:import [clojure.lang Atom])
  (:require [brute.entity :as br.e]
            [clojure.pprint :refer [pprint]]

            [rouje-like.components :as rj.c]
            [rouje-like.entity :as rj.e]
            [rouje-like.utils :as rj.u]
            [rouje-like.world :as rj.wr]
            [rouje-like.destructible :as rj.d]))

#_(defn take-damage
  [system this damage from]
  (let [c-destructible (rj.e/get-c-on-e system this :destructible)
        hp (:hp c-destructible)

        c-attacker (rj.e/get-c-on-e system this :attacker)
        attack (:attack c-attacker)

        c-position (rj.e/get-c-on-e system this :position)

        e-world (first (rj.e/all-e-with-c system :world))]
    (if (pos? (- hp damage))
      (-> system
          (rj.e/upd-c this :destructible
                      (fn [c-destructible]
                        (update-in c-destructible [:hp] - damage)))
>>>>>>>>>>>>>>>>>>>>>>>(attack this from))<<<<<<<<
      (-> system
>>>>>>>>>>>>>>>>>>>>>>>(attack this from) <<<<<<<<<<
          (rj.wr/update-in-world e-world [(:x c-position) (:y c-position)]
                           (fn [entities _]
                             (vec
                               (remove #(#{:lichen} (:type %))
                                          entities))))
          (rj.e/kill-e this)))))

(defn can-attack?
  [system _ target]
  (not (nil? (rj.e/get-c-on-e system target :destructible))))

(defn attack
  [system this target]
  (if (can-attack? system this target)
    (let [atk (:atk (rj.e/get-c-on-e system this :attacker))
          damage (if (< (rand-int 100) 30)
                   atk 0)

          c-destr (rj.e/get-c-on-e system target :destructible)]
      (rj.c/take-damage c-destr target damage this system))
    system))

(declare process-input-tick)

(defn add-lichen
  ([system]
   (let [e-world (first (rj.e/all-e-with-c system :world))
         c-world (rj.e/get-c-on-e system e-world :world)
         world (:world c-world)]
     (loop [target (get-in world [(rand-int (count world))
                                  (rand-int (count (first world)))])]
       (if (#{:wall} (:type (rj.u/get-top-entity target)))
         (recur (get-in world [(rand-int (count world))
                               (rand-int (count (first world)))]))
         (add-lichen system target)))))
  ([system target]
   (let [e-world (first (rj.e/all-e-with-c system :world))
         e-lichen (br.e/create-entity)]
     (-> system
         (rj.wr/update-in-world e-world [(:x target) (:y target)]
                                (fn [entities _]
                                  (vec (conj (remove #(#{:wall} (:type %)) entities)
                                             (rj.c/map->Entity {:id   e-lichen
                                                                :type :lichen})))))
         (rj.e/add-c e-lichen (rj.c/map->Lichen {:grow-chance% 4
                                                 :max-blob-size 8}))
         (rj.e/add-c e-lichen (rj.c/map->Position {:x (:x target)
                                                   :y (:y target)}))
         (rj.e/add-c e-lichen (rj.c/map->Destructible {:hp      1
                                                       :defense 1
                                                       :take-damage-fn rj.d/take-damage}))
         (rj.e/add-c e-lichen (rj.c/map->Attacker {:atk 1
                                                   :can-attack? can-attack?
                                                   :attack      attack}))
         (rj.e/add-c e-lichen (rj.c/map->Tickable {:tick-fn process-input-tick}))))))

(defn get-size-of-lichen-blob
  [world origin]
  (loop [current (get-in world origin)
         explored #{}
         un-explored (into #{} (rj.u/get-neighbors-of-type world origin [:lichen]))]
    (if (empty? un-explored)
      (count explored)
      (recur (first un-explored)
             (conj explored current)
             (into (rest un-explored)
                   (remove #(or (#{current} %)
                                (explored %))
                           (rj.u/get-neighbors-of-type world
                                                  [(:x (first un-explored))
                                                   (:y (first un-explored))]
                                                  [:lichen])))))))

(defn process-input-tick
  [_ e-this system]
  (let [c-position (rj.e/get-c-on-e system e-this :position)
        x (:x c-position)
        y (:y c-position)

        e-world (first (rj.e/all-e-with-c system :world))
        c-world (rj.e/get-c-on-e system e-world :world)
        world (:world c-world)

        c-lichen (rj.e/get-c-on-e system e-this :lichen)
        grow-chance% (:grow-chance% c-lichen)
        max-blob-size (:max-blob-size c-lichen)

        empty-neighbors (rj.u/get-neighbors-of-type world [x y]
                                               [:floor :torch :gold])]
    (if (and (seq empty-neighbors)
             (< (rand 100) grow-chance%)
             (< (get-size-of-lichen-blob world [x y])
                max-blob-size))
      (add-lichen system (rand-nth empty-neighbors))
      system)))