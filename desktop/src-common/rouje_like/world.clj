(ns rouje-like.world
  (:import [com.badlogic.gdx.graphics.g2d SpriteBatch]
           [clojure.lang Keyword Atom])

  (:require [play-clj.g2d :refer :all]

            [rouje-like.components :as rj.c]
            [rouje-like.entity     :as rj.e]

            [clojure.math.numeric-tower :as math]
            [clojure.pprint :refer :all]))

(defn ^:private new-tile
  [x y {:keys [type]}]
  (let [screen-x (+ (* x rj.c/block-size) (* 1 rj.c/block-size))
        screen-y (+ (* y rj.c/block-size) (* 1 rj.c/block-size))]
    (rj.c/map->Tile {:x x :y y
                     :screen-x screen-x :screen-y screen-y
                     :entities [(rj.c/map->Entity {:type type})]})))

(defn ^:private assoc-in-world
  [world [x y] args]
  (assoc-in world [x y]
            (new-tile x y args)))

(defn ^:private init-torches
  [world torch-count
   [width height]]
  (loop [world world
         torch-count torch-count]
    (if (pos? torch-count)
      (let [x (+ (rand-int (- width 2)) 1)
            y (+ (rand-int (- height 2)) 1)]
        (if (= (:type (first (:entities (get-in world [x y])))) :floor)
          (recur (assoc-in-world world [x y] {:type :torch})
                 (dec torch-count))
          (recur world torch-count)))
      world)))

(defn ^:private init-treasure
  [world treasure-count
   [width height]]
  (loop [world world
         treasure-count treasure-count]
    (if (pos? treasure-count)
      (let [x (+ (rand-int (- width 2)) 1)
            y (+ (rand-int (- height 2)) 1)]
        (if (= (:type (first (:entities (get-in world [x y])))) :floor)
          (recur (assoc-in-world world [x y] {:type :gold})
                 (dec treasure-count))
          (recur world treasure-count)))
      world)))

(defn ^:private block-coords
  [x y dist]
  (let [∆x|y (vec (range (- 0 dist) (inc dist)))]
    (for [dx ∆x|y
          dy ∆x|y
          :when (or (if (= dist 1)
                      true)
                    (= dist (math/abs dx))
                    (= dist (math/abs dy)))]
      [(+ x dx) (+ y dy)])))

(defn ^:private get-block
  [tiles x y dist]
  (map (fn [[x y]]
         (get-in tiles [x y]
                 (new-tile x y
                           {:type :wall})))
       (block-coords x y dist)))

(defn ^:private smooth-world-v1
  [world]
  (let [get-smoothed-tile (fn [block-d1 block-d2 x y]
                            (let [tile-counts-d1 (frequencies (map (fn [tile]
                                                                     (:type (first (:entities
                                                                                     tile))))
                                                                   block-d1))
                                  tile-counts-d2 (frequencies (map (fn [tile]
                                                                     (:type (first (:entities
                                                                                     tile))))
                                                                   block-d2))
                                  wall-threshold-d1 5
                                  wall-bound-d2 2
                                  wall-count-d1 (get tile-counts-d1 :wall 0)
                                  wall-count-d2 (get tile-counts-d2 :wall 0)
                                  result (if (or (>= wall-count-d1 wall-threshold-d1)
                                                  (<= wall-count-d2 wall-bound-d2))
                                           :wall
                                           :floor)]
                              (new-tile x y
                                        {:type result})))
        get-smoothed-col (fn [tiles x]
                           (mapv (fn [y]
                                   (get-smoothed-tile (get-block tiles x y 1)
                                                      (get-block tiles x y 2)
                                                      x y))
                                 (range (count (first tiles)))))]
    (mapv (fn [x]
            (get-smoothed-col world x))
          (range (count world)))))

(defn ^:private smooth-world-v2
  [world]
  (let [get-smoothed-tile (fn [block-d1 x y]
                            (let [tile-counts-d1 (frequencies (map (fn [tile]
                                                                     (:type (first (:entities
                                                                                     tile))))
                                                                   block-d1))
                                  wall-threshold-d1 5
                                  wall-count-d1 (get tile-counts-d1 :wall 0)
                                  result (if (>= wall-count-d1 wall-threshold-d1)
                                           :wall
                                           :floor)]
                              (new-tile x y
                                        {:type result})))
        get-smoothed-col (fn [tiles x]
                           (mapv (fn [y]
                                   (get-smoothed-tile (get-block tiles x y 1)
                                                      x y))
                                 (range (count (first tiles)))))]
    (mapv (fn [x]
            (get-smoothed-col world x))
          (range (count world)))))

(defn generate-random-world
  [[width height]
   init-wall%
   init-torch%
   init-treasure%
   [^Atom init-player-x-pos
    ^Atom init-player-y-pos]]
  (let [world (atom (vec
                      (map vec
                           (for [x (range width)]
                             (for [y (range height)]
                               (new-tile x y
                                         {:type (if (< (rand-int 100) init-wall%)
                                                  :wall
                                                  :floor)}))))))]
    ;; SMOOTH-WORLD
    (doseq [_ (range 4)]
      (swap! world (fn [prev]
                     (smooth-world-v1 prev))))
    (doseq [_ (range 3)]
      (swap! world (fn [prev]
                     (smooth-world-v2 prev))))
    ;; GENERATE-TREASURE
    (swap! world (fn [prev]
                   (init-treasure prev (* (* width height)
                                         (/ init-treasure% 100))
                                  [width height])))
    ;; GENERATE-TORCHES
    (swap! world (fn [prev]
                   (init-torches prev (* (* width height)
                                         (/ init-torch% 100))
                                 [width height])))
    ;; ADD-PLAYER
    (swap! world (fn [prev]
                   (loop [world prev]
                     (let [x (+ (rand-int (- width 2)) 1)
                           y (+ (rand-int (- height 2)) 1)]
                       (if (not= (:type (first (:entities (get-in world [x y])))) :wall)
                         (do (reset! init-player-x-pos x)
                             (reset! init-player-y-pos y)
                             (assoc-in-world world [@init-player-x-pos @init-player-y-pos]
                                             {:type :player}))
                         (recur world))))))
    @world))

(def ^:private get-texture (memoize
                             (fn [^Keyword type]
                               (type (zipmap [:player :wall :floor :gold :torch]
                                             (map (comp texture*
                                                        (fn [s] (str s ".jpg")))
                                                  ["at-inverted" "percent-inverted"
                                                   "period-inverted" "dollar-inverted"
                                                   "letter_t"]))))))

(defn render-world
  [system this _]
  (let [renderer (new SpriteBatch)
        taxicab-dist (fn [[x y] [i j]]
                             (+ (math/abs (- i x))
                                (math/abs (- j y))))

        e-player (first (rj.e/all-e-with-c system :player))

        c-player-pos (rj.e/get-c-on-e system e-player :position)
        player-pos [@(:x c-player-pos)
                    @(:y c-player-pos)]
        show-world? @(:show-world? (rj.e/get-c-on-e system e-player :player))

        c-sight (rj.e/get-c-on-e system e-player :sight)
        sight (math/ceil @(:distance c-sight))

        c-world (rj.e/get-c-on-e system this :world)
        world @(:world c-world)]
    (.begin renderer)
    (doseq [x (range (count world))
            y (range (count (first world)))
            :let [tile (get-in world [x y])]]
      (when (or show-world?
                (> sight
                   (taxicab-dist player-pos [x y])))
        (let [texture-region (-> (:type (first (:entities tile)))
                                 (get-texture)
                                 (:object))]
          (.draw renderer
                 texture-region
                 (float (:screen-x tile))
                 (float (:screen-y tile))))))
    (.end renderer)))

