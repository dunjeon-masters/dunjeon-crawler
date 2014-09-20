(ns rouje-like.world
  (:import [com.badlogic.gdx.graphics.g2d SpriteBatch]
           [clojure.lang Keyword Atom])

  (:require [play-clj.g2d :refer :all]

            [rouje-like.components :as rj.c]
            [rouje-like.entity     :as rj.e]

            [clojure.math.numeric-tower :as math]
            [clojure.pprint :refer :all]))

(def get-texture (memoize
                   (fn [^Keyword type]
                     (type (zipmap [:player :wall :floor :gold :torch]
                                   (map (comp texture*
                                              (fn [s] (str s ".jpg")))
                                        ["at-inverted" "percent-inverted"
                                         "period-inverted" "dollar-inverted"
                                         "letter_t"]))))))

(defn ^:private new-tile
  [x y args]
  (let [{:keys [type]} args
        x (+ (* x rj.c/block-size) (* 1 rj.c/block-size))
        y (+ (* y rj.c/block-size) (* 1 rj.c/block-size))]
    (rj.c/->Tile x y type)))

(defn ^:private update-world
  [world [x y] {:keys [type]}]
  (update-in world [x y]
             (fn [_] (new-tile x y
                               {:type type}))))

(defn ^:private init-torches
  [world torch-count
   [width height]]
  (loop [world world
         torch-count torch-count]
    (if (pos? torch-count)
      (let [x (+ (rand-int (- width 2)) 1)
            y (+ (rand-int (- height 2)) 1)]
        (if (= (:type (get-in world [x y])) :floor)
          (recur (update-world world [x y] {:type :torch})
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
        (if (= (:type (get-in world [x y])) :floor)
          (recur (update-world world [x y] {:type :gold})
                 (dec treasure-count))
          (recur world treasure-count)))
      world)))

(defn smooth-world-d2
  [world]
  (let [block-coords-d1 (fn [x y]
                          (for [dx [-1 0 1]
                                dy [-1 0 1]]
                            [(+ x dx) (+ y dy)]))
        block-coords-d2 (fn [x y]
                          (for [dx [-2 -1 0 1 2]
                                dy [-2 -1 0 1 2]
                                :when (or (= 2 (math/abs dx))
                                          (= 2 (math/abs dy)))]
                            [(+ x dx) (+ y dy)]))
        get-block-d1 (fn [tiles x y]
                       (map (fn [[x y]]
                              (get-in tiles [x y]
                                      (new-tile x y
                                                {:type :wall})))
                            (block-coords-d1 x y)))
        get-block-d2 (fn [tiles x y]
                       (map (fn [[x y]]
                              (get-in tiles [x y]
                                      (new-tile x y
                                                {:type :wall})))
                            (block-coords-d2 x y)))
        get-smoothed-tile (fn [block-d1 block-d2 x y]
                            (let [tile-counts-d1 (frequencies (map :type block-d1))
                                  tile-counts-d2 (frequencies (map :type block-d2))
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
                                   (get-smoothed-tile (get-block-d1 tiles x y) (get-block-d2 tiles x y) x y))
                                 (range (count (first tiles)))))]
    (mapv (fn [x]
            (get-smoothed-col world x))
          (range (count world)))))

(defn smooth-world-d1
  [world]
  (let [block-coords-d1 (fn [x y]
                          (for [dx [-1 0 1]
                                dy [-1 0 1]]
                            [(+ x dx) (+ y dy)]))
        get-block-d1 (fn [tiles x y]
                       (map (fn [[x y]]
                              (get-in tiles [x y]
                                      (new-tile x y
                                                {:type :wall})))
                            (block-coords-d1 x y)))
        get-smoothed-tile (fn [block-d1 x y]
                            (let [tile-counts-d1 (frequencies (map :type block-d1))
                                  wall-threshold-d1 5
                                  wall-count-d1 (get tile-counts-d1 :wall 0)
                                  result (if (>= wall-count-d1 wall-threshold-d1)
                                           :wall
                                           :floor)]
                              (new-tile x y
                                        {:type result})))
        get-smoothed-col (fn [tiles x]
                           (mapv (fn [y]
                                   (get-smoothed-tile (get-block-d1 tiles x y) x y))
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
                     (smooth-world-d2 prev))))
    (doseq [_ (range 3)]
      (swap! world (fn [prev]
                     (smooth-world-d1 prev))))
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
                       (if (not= (:type (get-in world [x y])) :wall)
                         (do (reset! init-player-x-pos x)
                             (reset! init-player-y-pos y)
                             (update-world world [@init-player-x-pos @init-player-y-pos]
                                           {:type :player}))
                         (recur world))))))
    world))

(defn render-world
  [system entity _]
  (let [renderer (new SpriteBatch)
        taxicab-dist (fn [[x y] [i j]]
                             (+ (math/abs (- i x))
                                (math/abs (- j y))))

        e-player (first (rj.e/all-e-with-c system :position))
        c-position (rj.e/get-c-on-e system e-player :position)
        player-pos [@(:x c-position)
                    @(:y c-position)]
        show-world? @(:show-world? (rj.e/get-c-on-e system e-player :player))

        c-sight (rj.e/get-c-on-e system e-player :sight)
        sight (math/ceil @(:distance c-sight))

        c-world (rj.e/get-c-on-e system entity :world)
        world @(:tiles c-world)]
    (.begin renderer)
    (doseq [x (range (count world))
            y (range (count (first world)))
            :let [item (get-in world [x y])]]
      (when (or #_(or (= x 0) (= y 0))
                #_(or (= x (dec (count world)))
                    (= y (dec (count (first world)))))
                (or show-world?
                     (> sight
                        (taxicab-dist player-pos [x y]))))
        (let [texture-region (-> (:type item)
                                 (get-texture)
                                 (:object))]
          (.draw renderer
                 texture-region
                 (float (:x item))
                 (float (:y item))))))
    (.end renderer)))

