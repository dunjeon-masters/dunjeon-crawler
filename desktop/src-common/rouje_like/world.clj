(ns rouje-like.world
  (:import [com.badlogic.gdx.graphics.g2d SpriteBatch]
           [clojure.lang Keyword])

  (:require [play-clj.g2d :refer :all]

            [rouje-like.components :as rj.c]
            [rouje-like.entity     :as rj.e]))

(def get-texture (memoize
                   (fn [^Keyword type]
                     (type (zipmap [:player :wall :empty :gold]
                                   (map (comp texture* (fn [s] (str s ".jpg")))
                                        ["at" "percent" "period" "dollar"]))))))

(defn new-tile
  [x y & args]
  (let [{:keys [type]} args]
    (rj.c/->Tile x y
                 type)))

(defn init-treasure
  [world treasure-count
   block-size [width height]]
  (loop [world world
         treasure-count treasure-count]
    (if (pos? treasure-count)
      (recur (let [x (inc (rand-int (- width 2)))
                   y (inc (rand-int (- height 2)))]
               (update-in world [x y]
                          (fn [_] (new-tile (* x block-size)
                                            (* y block-size)
                                            :type :gold))))
             (dec treasure-count))
      world)))

(defn generate-random-world
  [block-size [width height]]
  ;; GENERATE-SKELETON
  (let [board (atom (vec
                      (map vec
                           (for [x (range width)]
                             (for [y (range height)]
                               (new-tile (* x block-size)
                                         (* y block-size)
                                         :type :wall))))))]
    ;; GENERATE-BODY
    (doseq [x (range 1 (dec width))
            y (range 1 (dec height))
            :let [tile (new-tile (* x block-size)
                                 (* y block-size)
                                 :type :empty)]]
      (swap! board (fn [old]
                     (update-in old [x y]
                                (fn [_] tile)))))
    ;; GENERATE-TREASURE
    (swap! board (fn [old]
                   (init-treasure old (* (* width height)
                                         (/ 50 100))
                                  block-size [width height])))
    ;; ADD-PLAYER, hardcoded at [10, 10]
    (swap! board (fn [old]
                   (update-in old [10 10]
                              (fn [_] (new-tile (* 10 block-size)
                                                (* 10 block-size)
                                                :type :player)))))
    board))

(defn render-world!
  [system entity]
  (let [renderer (new SpriteBatch)
        c-world (rj.e/get-c system entity :world)
        board @(:tiles c-world)]
    (.begin renderer)
    (doseq [col board
            row col]
      (let [texture-region (-> (:type row)
                               (get-texture)
                               (:object))]
        (.draw renderer
               texture-region
               (float (:x row))
               (float (:y row)))))
    (.end renderer)))

