(ns rouje-like.world
  (:import [com.badlogic.gdx.graphics.g2d SpriteBatch TextureRegion]
           [clojure.lang Keyword Atom]
           [com.badlogic.gdx.graphics Texture Pixmap Color]
           [com.badlogic.gdx.files FileHandle])

  (:require [play-clj.g2d :refer :all]
            [play-clj.core :refer :all]

            [clojure.math.numeric-tower :as math]
            [brute.entity :as br.e]

            [rouje-like.components :as rj.c]
            [rouje-like.entity :as rj.e]
            [rouje-like.utils :as rj.u]))

(defn update-in-world
  [system e-world target-pos -fn-]
  (rj.e/upd-c system e-world :world
              (fn [c-world]
                (update-in c-world [:world]
                           (fn [world]
                             (update-in world target-pos
                                        (fn [tile]
                                          (update-in tile [:entities]
                                                     (fn [entities]
                                                       (-fn- entities))))))))))

(defn ^:private new-tile
  [x y {:keys [type, id]}]
  (rj.c/map->Tile {:x x :y y
                   :entities [(rj.c/map->Entity {:id   id
                                                 :type type})]}))

(defn ^:private add-torch
  [world]
  (loop [world world]
    (let [x (rand-int (count world))
          y (rand-int (count (first world)))]
      (if (and (rj.u/not-any-radially-of-type world [x y]
                                              #(<= % 3) [:torch])
               (every? #(#{:floor} (:type %))
                       (:entities (get-in world [x y]))))
        (update-in world [x y]
                   (fn [tile]
                     (update-in tile [:entities]
                                (fn [entities]
                                  (conj entities
                                        (rj.c/map->Entity {:id   nil
                                                           :type :torch}))))))
        (recur world)))))

(defn ^:private add-gold
  [world]
  (loop [world world]
    (let [x (rand-int (count world))
          y (rand-int (count (first world)))]
      (if (every? #(#{:floor} (:type %))
                  (:entities (get-in world [x y])))
        (update-in world [x y]
                   (fn [tile]
                     (update-in tile [:entities]
                                (fn [entities]
                                  (conj entities
                                        (rj.c/map->Entity {:id   nil
                                                           :type :gold}))))))
        (recur world)))))

(defn ^:private get-block-frequencies
  [block]
  (frequencies
    (map (fn [tile]
           (:type (rj.u/get-top-entity tile)))
         block)))

(defn ^:private get-smoothed-tile
  [block-d1 block-d2 x y]
  (let [wall-threshold-d1 5
        wall-bound-d2 2
        d1-block-freqs (get-block-frequencies block-d1)
        d2-block-freqs (if (nil? block-d2)
                         {:wall (inc wall-bound-d2)}
                         (get-block-frequencies block-d2))
        wall-count-d1 (get d1-block-freqs :wall 0)
        wall-count-d2 (get d2-block-freqs :wall 0)
        result (if (or (>= wall-count-d1 wall-threshold-d1)
                       (<= wall-count-d2 wall-bound-d2))
                 :wall
                 :floor)]
    (update-in (rj.c/map->Tile {:x x :y y
                                :entities [(rj.c/map->Entity {:id   nil
                                                              :type :floor})]})
               [:entities] (fn [entities]
                             (if (= result :wall)
                               (conj entities
                                     (rj.c/map->Entity {:id   nil
                                                        :type :wall}))
                               entities)))))

(defn ^:private get-smoothed-col
  [world x max-dist]
  {:pre [(#{1 2} max-dist)]}
  (mapv (fn [y]
          (get-smoothed-tile
            (rj.u/get-ring-around world [x y] 1)
            (if (= max-dist 2)
              (rj.u/get-ring-around world [x y] 2)
              nil)
            x y))
        (range (count (first world)))))

(defn ^:private smooth-world-v1
  [world]
  (mapv (fn [x]
          (get-smoothed-col world x 2))
        (range (count world))))

(defn ^:private smooth-world-v2
  [world]
  (mapv (fn [x]
          (get-smoothed-col world x 1))
        (range (count world))))

(defn generate-random-world
  [{:keys [width height]}
   init-wall%
   init-torch%
   init-treasure%]
  (let [world (vec
                (map vec
                     (for [x (range width)]
                       (for [y (range height)]
                         (update-in (rj.c/map->Tile {:x x :y y
                                                     :entities [(rj.c/map->Entity {:id   nil
                                                                                   :type :floor})]})
                                    [:entities] (fn [entities]
                                                  (if (< (rand-int 100) init-wall%)
                                                    (conj entities
                                                          (rj.c/map->Entity {:id   nil
                                                                             :type :wall}))
                                                    entities)))))))]
    ;; SMOOTH-WORLD
    (-> world
        (as-> world
              (nth (iterate smooth-world-v1 world)
                   4)
              (nth (iterate smooth-world-v2 world)
                   2)
              (nth (iterate add-gold world) (* (* width height)
                                               (/ init-treasure% 100)))
              (nth (iterate add-torch world) (* (* width height)
                                                (/ init-torch% 100)))))))

(def ^:private type->tile-info
  {:player   {:x 0 :y 4
              :width 12 :height 12
              :color {:r 255 :g 255 :b 255 :a 255}
              :tile-sheet "grim_12x12.png"}
   :wall     {:x 3 :y 2
              :width 12 :height 12
              :color {:r 255 :g 255 :b 255 :a 128}
              :tile-sheet "grim_12x12.png"}
   :gold     {:x 1 :y 9
              :width 12 :height 12
              :color {:r 255 :g 255 :b 1 :a 255}
              :tile-sheet "grim_12x12.png"}
   :lichen   {:x 15 :y 0
              :width 12 :height 12
              :color {:r 1 :g 255 :b 1 :a 255}
              :tile-sheet "grim_12x12.png"}
   :floor    {:x 14 :y 2
              :width 12 :height 12
              :color {:r 255 :g 255 :b 255 :a 64}
              :tile-sheet "grim_12x12.png"}
   :torch    {:x 1 :y 2
              :width 12 :height 12
              :color {:r 255 :g 1 :b 1 :a 255}
              :tile-sheet "grim_12x12.png"}
   :bat      {:x 0 :y 9
              :width 16 :height 16
              :color {:r 255 :g 255 :b 255 :a 128}
              :tile-sheet "DarkondDigsDeeper_16x16.png"}
   :skeleton {:x 3 :y 5
              :width 16 :height 16
              :color {:r 255 :g 255 :b 255 :a 255}
              :tile-sheet "DarkondDigsDeeper_16x16.png"}})

(def ^:private type->texture
  (memoize
    (fn [^Keyword type]
      (let [tile-info (type->tile-info type)
            tile-sheet (:tile-sheet tile-info)
            width (:width tile-info)
            height (:height tile-info)
            x (* width (:x tile-info))
            y (* height (:y tile-info))
            tile-color (:color tile-info)]
        (assoc (texture tile-sheet
                        :set-region x y width height)
          :color tile-color)))))

(defn render-world
  [_ e-this {:keys [view-port-sizes]} system]
  (let [e-player (first (rj.e/all-e-with-c system :player))

        c-player-pos (rj.e/get-c-on-e system e-player :position)
        player-pos [(:x c-player-pos)
                    (:y c-player-pos)]
        show-world? (:show-world? (rj.e/get-c-on-e system e-player :player))

        c-sight (rj.e/get-c-on-e system e-player :playersight)
        sight (math/ceil (:distance c-sight))

        c-world (rj.e/get-c-on-e system e-this :world)
        world (:world c-world)

        [vp-size-x vp-size-y] view-port-sizes

        start-x (max 0 (- (:x c-player-pos)
                          (int (/ vp-size-x 2))))
        start-y (max 0 (- (:y c-player-pos)
                          (int (/ vp-size-y 2))))

        end-x (+ start-x vp-size-x)
        end-x (min end-x (count world))

        end-y (+ start-y vp-size-y)
        end-y (min end-y (count (first world)))

        start-x (- end-x vp-size-x)
        start-y (- end-y vp-size-y)

        renderer (new SpriteBatch)]
    (.begin renderer)
    (doseq [x (range start-x end-x)
            y (range start-y end-y)
            :let [tile (get-in world [x y])]]
      (when (or show-world?
                (> sight
                   (rj.u/taxicab-dist player-pos [x y])))
        (let [texture-entity (-> (rj.u/get-top-entity tile)
                                 (:type) (type->texture))]
          (let [color-values (:color texture-entity)]
              (.setColor renderer
                         (Color. (float (/ (:r color-values) 255))
                                 (float (/ (:g color-values) 255))
                                 (float (/ (:b color-values) 255))
                                 (float (/ (:a color-values) 255)))))
          (.draw renderer
                 (:object texture-entity)
                 (float (* (+ (- x start-x)
                              (:left rj.c/padding-sizes))
                           rj.c/block-size))
                 (float (* (+ (- y start-y)
                              (:btm rj.c/padding-sizes))
                           rj.c/block-size))
                 (float rj.c/block-size) (float rj.c/block-size)))))
    (.end renderer)))
