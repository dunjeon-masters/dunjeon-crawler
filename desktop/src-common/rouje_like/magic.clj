(ns rouje-like.magic
  (require [rouje-like.utils :as rj.u :refer [?]]
           [rouje-like.equipment :as rj.eq]
           [rouje-like.config :as rj.cfg]
           [rouje-like.entity-wrapper     :as rj.e]))

(def ^:private three-range-dir-vec-map
  {:left  [[-1 0] [-2 0] [-3 0]]
   :right [[1  0] [2 0] [3 0]]
   :up    [[0 -1] [0 -2] [0 -3]]
   :down  [[0  1] [0 2] [0 3]]})

(defn calc-range
  [[x y] [x-off y-off]]
  [(+ x x-off) (+ y y-off)])

(defn use-fireball
  [system e-this direction]
  (let [dir-vec (direction three-range-dir-vec-map)
        c-pos (rj.e/get-c-on-e system e-this :position)
        e-this-coords  [(:x c-pos) (:y c-pos)]
        range (map calc-range (repeat e-this-coords) dir-vec)]
    system))