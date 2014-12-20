(ns rouje-like.maze
  (:require [brute.entity :as br.e]
            [rouje-like.utils :as rj.u]
            [rouje-like.entity-wrapper :as rj.e]
            [rouje-like.components :as rj.c]
            [rouje-like.config :as rj.cfg]))

(def ^:private direction8->offset
  {:left  [-1  0]
   :right [ 1  0]
   :up    [ 0 -1]
   :down  [ 0  1]
   :up-left    [-1 -1]
   :up-right   [ 1 -1]
   :down-left  [-1  1]
   :down-right [ 1  1]})

(def ^:private direction4->offset
  {:left  [-1 0]
   :right [1  0]
   :up    [0 -1]
   :down  [0  1]})

(defn- coords+offset
  [[x y] [dx dy]]
  [(+ x dx) (+ y dy)])

(defn- get-neighbors-coords
  [origin target]
  (if origin
    (let [y-eq? (= (origin 0) (target 0))]
      (remove #(= (if y-eq? (origin 1) (origin 0))
                  (if y-eq? (% 1) (% 0)))
              (map coords+offset
                   (repeat target) (vals direction8->offset))))
    (map coords+offset
         (repeat target) (vals direction4->offset))))

(defn- get-neighbors-of-type
  [level mark pos typ]
  (let [neighbors (get-neighbors-coords mark pos)
        neighbors (map #(get-in level % nil)
                       neighbors)
        neighbors (filter identity neighbors)]
    (filter #(= typ (% 2)) neighbors)))

(defn- get-first-valid-neighbor
  [level _ neighbors mark]
  (let [neighbors (shuffle neighbors)]
    (loop [candidate (first neighbors)
           candidates (rest neighbors)]
      (if candidate
        (let [[x y _] candidate
              candidate-pos [x y]
              cand-neighbors (get-neighbors-of-type level mark candidate-pos :w)
              wall-count (count cand-neighbors)]
          (if (= 5 wall-count)
            candidate
            (recur (first candidates)
                   (rest candidates))))
        nil))))

(defn- floor-it
  [tile]
  (assoc tile 2 :f))

(defn- floor-in-level
  [level pos]
  (update-in level pos
             floor-it))

(defn- get-candidate
  [cells alg perc]
  (case alg
    :rand  (rand-nth cells)
    :first (first cells)
    :last  (last cells)
    :rand/first (if (< (rand) perc)
                  (rand-nth cells)
                  (first cells))
    :rand/last  (if (< (rand) perc)
                  (rand-nth cells)
                  (last cells))
    :first/last (if (< (rand) perc)
                  (first cells)
                  (last cells))
    :else (first cells)))

(defn- growing-tree
  [level]
  (let [init-tile (rand-nth (rand-nth level))
        init-tile (floor-it init-tile)
        [x y _] init-tile]
    (loop [cells [init-tile]
           level (floor-in-level level [x y])]
      (if (seq cells)
        (let [candidate (get-candidate cells :first/last 0.5)
              [x y _] candidate
              candidate-pos [x y]
              neighbors (get-neighbors-of-type level nil candidate-pos :w)
              valid-neighbor (get-first-valid-neighbor level cells neighbors candidate)
              [x y _] valid-neighbor
              valid-neighbor-pos [x y]]
          (if valid-neighbor
            (recur (conj cells (floor-it valid-neighbor))
                   (floor-in-level level valid-neighbor-pos))
            (recur (remove #(= % candidate) cells)
                   level)))
        level))))

(defn- gen-walls
  [width height]
  (vec
    (for [x (range width)]
      (vec
        (for [y (range height)]
          [x y :w])))))

(defn generate-maze
  [[width height] z]
  (let [level (vec
                (map vec
                     (for [x (range width)]
                       (for [y (range height)]
                         (rj.c/map->Tile {:x x :y y :z z
                                          :entities [(rj.c/map->Entity {:id nil
                                                                        :type :floor})
                                                     (rj.c/map->Entity {:id   (br.e/create-entity)
                                                                        :type :maze-wall})]})))))
        maze (growing-tree (gen-walls width height))]
    (reduce (fn [level [x y t]]
              (if (or (= :f t)
                      (and (= :w t)
                           (< (rand-int 100)
                              rj.cfg/maze:wall->floor%)))
                (update-in level [x y]
                           (fn [tile]
                             (update-in tile [:entities]
                                        (fn [entities]
                                          (remove #(= :maze-wall (:type %))
                                                  entities)))))
                level))
            level (map vec (partition 3 (flatten maze))))))
