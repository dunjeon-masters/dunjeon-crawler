(ns rouje-like.world
  (:import [com.badlogic.gdx.graphics.g2d SpriteBatch])

  (:require [play-clj.g2d :refer :all]

            [rouje-like.components :as rj.c]
            [rouje-like.entity     :as rj.e]))

(def block-size 27)
(def world-sizes [20 20])

(defn new-tile
  [x y & args]
  (let [{:keys [type]} args]
    (rj.c/->Tile x y
                 type)))

(defn init-treasure [world percent-treasure]
  (loop [world world
         treasure-count (* (* (- (world-sizes 0) 2)
                              (- (world-sizes 1) 2))
                           (/ percent-treasure 100))]
    (if (pos? treasure-count)
      (recur (let [x (inc (rand-int (- (world-sizes 0) 2)))
                   y (inc (rand-int (- (world-sizes 1) 2)))]
               (update-in world [x y]
                          (fn [_] (new-tile (* x block-size)
                                            (* y block-size)
                                            :type :gold))))
             (dec treasure-count))
      world)))

(defn generate-random-world
  ([] (generate-random-world world-sizes))

  ([[width height]]
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
                    (init-treasure old 50)))
     ;; ADD-PLAYER, hardcoded at [10, 10]
     (swap! board (fn [old]
                    (update-in old [10 10]
                               (fn [_] (new-tile (* 10 block-size)
                                                 (* 10 block-size)
                                                 :type :player)))))
     board)))

(defn render-world!
  [system entity]
  #_(println "rj.wr/render-world!")
  (let [renderer (new SpriteBatch)
        c-world (rj.e/get-c system entity :world)
        board @(:tiles c-world)
        textures {:player (texture "at.jpg")
                  :wall   (texture "percent.jpg")
                  :empty  (texture "period.jpg")
                  :gold   (texture "dollar.jpg")}]
    (.begin renderer)
    (doseq [col board
            row col]
      (let [texture-region (:object ((:type row) textures))]
        (.draw renderer
               texture-region
               (float (:x row))
               (float (:y row)))))
    (.end renderer))
  system)

