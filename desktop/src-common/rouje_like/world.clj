(ns rouje-like.world
  (:import [com.badlogic.gdx.graphics.g2d SpriteBatch]
           [clojure.lang Keyword])

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
  (let [{:keys [type]} args]
    (rj.c/->Tile x y type)))

(defn ^:private update-world
  [world [x y] block-size {:keys [type]}]
  (update-in world [x y]
             (fn [_] (new-tile (* x block-size)
                               (* y block-size)
                               {:type type}))))

(defn ^:private init-torches
  [world torch-count
   block-size [width height]]
  (loop [world world
         torch-count torch-count]
    (if (pos? torch-count)
      (recur (let [x (+ (rand-int (- width 2)) 1)
                   y (+ (rand-int (- height 2)) 1)]
               (update-world world [x y] block-size {:type :torch}))
             (dec torch-count))
      world)))

(defn ^:private init-walls
  [world wall-count
   block-size [width height]]
  (loop [world world
         wall-count wall-count]
    (if (pos? wall-count)
      (recur (let [x (+ (rand-int (- width 4)) 2)
                   y (+ (rand-int (- height 4)) 2)]
               (-> world
                   (update-world [x y] block-size {:type :wall})
                   (update-world [(+ x 1) y] block-size {:type :wall})
                   (update-world [(+ x 2) y] block-size {:type :wall})))
             (dec wall-count))
      world)))

(defn ^:private init-treasure
  [world treasure-count
   block-size [width height]]
  (loop [world world
         treasure-count treasure-count]
    (if (pos? treasure-count)
      (recur (let [x (+ (rand-int (- width 2)) 1)
                   y (+ (rand-int (- height 2)) 1)]
               (update-world world [x y] block-size {:type :gold}))
             (dec treasure-count))
      world)))

(defn generate-random-world
  [block-size
   [width height]
   init-player-pos
   init-treasure-percent]
  ;; GENERATE-SKELETON
  (let [board (atom (vec
                      (map vec
                           (for [x (range width)]
                             (for [y (range height)]
                               (new-tile (* x block-size)
                                         (* y block-size)
                                         {:type (if (< (rand-int 100) 45)
                                                  :wall
                                                  :floor)}))))))]
    ;; GENERATE-BODY
    (doseq [x (range 1 (dec width))
            y (range 1 (dec height))
            :let [tile (new-tile (* x block-size)
                                 (* y block-size)
                                 {:type :floor})]]
      (swap! board (fn [prev]
                     (update-in prev [x y]
                                (fn [_] tile)))))
    ;; GENERATE-TREASURE
    (swap! board (fn [prev]
                   (init-treasure prev (* (* width height)
                                         (/ init-treasure-percent 100))
                                  block-size [width height])))
    ;; GENERATE-WALLS
    (swap! board (fn [prev]
                   (init-walls prev 10
                               block-size [width height])))
    ;; GENERATE-TORCHES
    (swap! board (fn [prev]
                   (init-torches prev 10
                                 block-size [width height])))
    ;; ADD-PLAYER
    (swap! board (fn [prev]
                   (update-world prev init-player-pos block-size {:type :player})))
    board))

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

        c-sight (rj.e/get-c-on-e system e-player :sight)
        sight (math/ceil @(:distance c-sight))

        c-world (rj.e/get-c-on-e system entity :world)
        board @(:tiles c-world)]
    (.begin renderer)
    (doseq [x (range (count board))
            y (range (count (first board)))
            :let [item (get-in board [x y])]]
      (when (or (or (= x 0) (= y 0))
                (or (= x (dec (count board)))
                    (= y (dec (count (first board)))))
                (or false                                   ;; TODO: Replace with toggle (F key)
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

