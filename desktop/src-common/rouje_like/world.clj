(ns rouje-like.world
  (:import [com.badlogic.gdx.graphics.g2d SpriteBatch TextureRegion])

  (:require [play-clj.g2d :refer :all]
            [play-clj.core :refer :all]

            [brute.entity :as br.e]

            [rouje-like.rendering :as rj.r]
            [rouje-like.components :as rj.c]
            [rouje-like.entity-wrapper :as rj.e]
            [rouje-like.utils :as rj.u :refer [?]]
            [rouje-like.items :as rj.items]
            [rouje-like.lichen :as rj.lc]
            [rouje-like.destructible :as rj.d]
            [rouje-like.bat :as rj.bt]
            [rouje-like.skeleton :as rj.sk]
            [rouje-like.portal :as rj.p]
            [rouje-like.config :as rj.cfg]))

(defn ^:private block->freqs
  [block]
  (frequencies
    (map (fn [tile]
           (:type (rj.u/tile->top-entity tile)))
         block)))

(defn ^:private get-smoothed-tile
  [block-d1 block-d2 x y z]
  (let [wall-threshold-d1 5
        wall-bound-d2 2
        top-entity (rj.u/tile->top-entity
                     (first (filter (fn [tile]
                                      (and (= x (:x tile)) (= y (:y tile))))
                                    block-d1)))
        this-id (:id top-entity)
        d1-block-freqs (block->freqs block-d1)
        d2-block-freqs (if (nil? block-d2)
                         {:wall (inc wall-bound-d2)}
                         (block->freqs block-d2))
        wall-count-d1 (get d1-block-freqs :wall 0)
        wall-count-d2 (get d2-block-freqs :wall 0)
        result (if (or (>= wall-count-d1 wall-threshold-d1)
                       (<= wall-count-d2 wall-bound-d2))
                 :wall
                 :floor)]
    (update-in (rj.c/map->Tile {:x x :y y :z z
                                :entities [(rj.c/map->Entity {:id   nil
                                                              :type :floor})]})
               [:entities] (fn [entities]
                             (if (= result :wall)
                               (conj entities
                                     (rj.c/map->Entity {:id   (if this-id
                                                                this-id
                                                                (br.e/create-entity))
                                                        :type :wall}))
                               entities)))))

(defn ^:private get-smoothed-col
  [level [x z] max-dist]
  {:pre [(#{1 2} max-dist)]}
  (mapv (fn [y]
          (get-smoothed-tile
            (rj.u/get-ring-around level [x y] 1)
            (if (= max-dist 2)
              (rj.u/get-ring-around level [x y] 2)
              nil)
            x y z))
        (range (count (first level)))))

(defn ^:private smooth-level-v1
  [{:keys [level z]}]
  {:level (mapv (fn [x]
                  (get-smoothed-col level [x z] 2))
                (range (count level)))
   :z z})

(defn ^:private smooth-level-v2
  [{:keys [level z]}]
  {:level (mapv (fn [x]
                  (get-smoothed-col level [x z] 1))
                (range (count level)))
   :z z})

(defn ^:private forest:get-smoothed-tile
  [block-d1 block-d2 x y z]
  (let [wall-threshold-d1 5
        wall-bound-d2 2
        d1-block-freqs (block->freqs block-d1)
        d2-block-freqs (if (nil? block-d2)
                         {:tree (inc wall-bound-d2)}
                         (block->freqs block-d2))
        wall-count-d1 (get d1-block-freqs :tree 0)
        wall-count-d2 (get d2-block-freqs :tree 0)
        result (if (or (>= wall-count-d1 wall-threshold-d1)
                       (<= wall-count-d2 wall-bound-d2))
                 :tree
                 :forest-floor)]
    (update-in (rj.c/map->Tile {:x x :y y :z z
                                :entities [(rj.c/map->Entity {:id   nil
                                                              :type :forest-floor})]})
               [:entities] (fn [entities]
                             (if (= result :tree)
                               (conj entities
                                     (rj.c/map->Entity {:id   nil
                                                        :type :tree}))
                               entities)))))

(defn ^:private forest:get-smoothed-col
  [level [x z] max-dist]
  {:pre [(#{1 2} max-dist)]}
  (mapv (fn [y]
          (forest:get-smoothed-tile
            (rj.u/get-ring-around level [x y] 1)
            (if (= max-dist 2)
              (rj.u/get-ring-around level [x y] 2)
              nil)
            x y z))
        (range (count (first level)))))

(defn ^:private forest:smooth-level-v1
  [{:keys [level z]}]
  {:level (mapv (fn [x]
                  (forest:get-smoothed-col level [x z] 2))
                (range (count level)))
   :z z})

(defn ^:private forest:smooth-level-v2
  [{:keys [level z]}]
  {:level (mapv (fn [x]
                  (forest:get-smoothed-col level [x z] 1))
                (range (count level)))
   :z z})

(defn ^:private maze:get-smoothed-tile
  [block x y z]
  (let [wall-threshold 2
        wall-bound     5
        wall-birth 3
        block-freqs (block->freqs block)
        wall-count (get block-freqs :maze-wall 0)
        top-entity (rj.u/tile->top-entity
                      (first (filter (fn [tile]
                                       (and (= x (:x tile)) (= y (:y tile))))
                                     block)))
        this-id (:id top-entity)
        this-type (:type top-entity)
        result (if (and (= this-type :maze-wall)
                        (<= wall-count wall-bound)
                        (>= wall-count wall-threshold))
                 :maze-wall
                 (if (and (= this-type :floor)
                          (= wall-count wall-birth))
                   :maze-wall
                   :floor))]
    (update-in (rj.c/map->Tile {:x x :y y :z z
                                :entities [(rj.c/map->Entity {:id   nil
                                                              :type :floor})]})
               [:entities] (fn [entities]
                             (if (= result :maze-wall)
                               (conj entities
                                     (rj.c/map->Entity {:id   (if this-id
                                                                this-id
                                                                (br.e/create-entity))
                                                        :type :maze-wall}))
                               entities)))))

(defn ^:private maze:get-smoothed-col
  [level [x z]]
  (mapv (fn [y]
          (maze:get-smoothed-tile
            (rj.u/get-ring-around level [x y] 1)
            x y z))
        (range (count (first level)))))

(defn ^:private maze:smooth-level
  "cells survive from one generation to the next if they have at least one
  and at most five neighbours, and if a cell has exactly three neighbours, it is born."
  [{:keys [level z]}]
  {:level (mapv (fn [x]
                  (maze:get-smoothed-col level [x z]))
                (range (count level)))
   :z z})

(defn entity-ize-walls
  [system z]
  (let [e-world (first (rj.e/all-e-with-c system :world))
        c-world (rj.e/get-c-on-e system e-world :world)
        levels (:levels c-world)
        level (nth levels z)]
    (reduce (fn [system tile]
              (let [entities (:entities tile)
                    wall (filter #(rj.cfg/<walls> (:type %)) entities)]
                (if (seq wall)
                  (let [wall (first wall)
                        wall-type (:type wall)
                        e-wall (:id wall)
                        hp (:hp (rj.cfg/wall->stats wall-type))]
                    (rj.e/system<<components
                      system e-wall
                      [[:position {:x (:x tile)
                                   :y (:y tile)
                                   :z z
                                   :type wall-type}]
                       [:destructible {:hp hp
                                       :max-hp hp
                                       :def 0
                                       :take-damage-fn rj.d/take-damage}]]))
                  system)))
            system (flatten level))))

(def ^:private init-wall% 45)
(def ^:private init-torch% 2)
(def ^:private init-gold% 5)
(def ^:private init-health-potion% 2)
(def ^:private init-lichen% 1)
(def ^:private init-bat% 1)
(def ^:private init-skeleton% 1)

(defn init-entities
  [system z]
  (-> system
      ;; If wall, add an entity to it
      (as-> system
        (entity-ize-walls system z))
      ;; Add Items: Gold, Torches...
      (as-> system
        (do (println "core::add-gold: " (not (nil? system))) system)
        (nth (iterate rj.items/add-gold {:system system :z z})
             (* (/ init-gold% 100)
                (apply * (vals rj.cfg/world-sizes))))
        (:system system))
      (as-> system
        (do (println "core::add-torch " (not (nil? system))) system)
        (nth (iterate rj.items/add-torch {:system system :z z})
             (* (/ init-torch% 100)
                (apply * (vals rj.cfg/world-sizes))))
        (:system system))
      (as-> system
            (do (println "core::add-health-potion " (not (nil? system))) system)
            (nth (iterate rj.items/add-health-potion {:system system :z z})
                 (* (/ init-health-potion% 100)
                    (apply * (vals rj.cfg/world-sizes))))
            (:system system))

      ;; Spawn lichens
      (as-> system
        (do (println "core::add-lichen " (not (nil? system))) system)
        (nth (iterate rj.lc/add-lichen {:system system :z z})
             (* (/ init-lichen% 100)
                (apply * (vals rj.cfg/world-sizes))))
        (:system system))

      ;; Spawn bats
      (as-> system
        (do (println "core::add-bat " (not (nil? system))) system)
        (nth (iterate rj.bt/add-bat {:system system :z z})
             (* (/ init-bat% 100)
                (apply * (vals rj.cfg/world-sizes))))
        (:system system))

      ;; Spawn Skeletons
      (as-> system
        (do (println "core::add-skeleton " (not (nil? system))) system)
        (nth (iterate rj.sk/add-skeleton {:system system :z z})
             (* (/ init-skeleton% 100)
                (apply * (vals rj.cfg/world-sizes))))
        (:system system))))

(defn add-portal
  [system z]
  ;; Add portal
  (as-> system system
    (do (println "core::add-portal " (not (nil? system))) system)
    (rj.p/add-portal {:system system :z z})
    (:system system)))

(defn generate-random-level
  ([level-sizes z]
   (let [world-types [:cave :desert :maze :forest]]
     (generate-random-level level-sizes z (rand-nth world-types))))

  ([{:keys [width height]} z world-type]
   (case world-type
     :cave (let [level (vec
                         (map vec
                              (for [x (range width)]
                                (for [y (range height)]
                                  (update-in (rj.c/map->Tile {:x x :y y :z z
                                                              :entities [(rj.c/map->Entity {:id   nil
                                                                                            :type :floor})]})
                                             [:entities] (fn [entities]
                                                           (if (< (rand-int 100) init-wall%)
                                                             (conj entities
                                                                   (rj.c/map->Entity {:id   nil
                                                                                      :type :wall}))
                                                             entities)))))))]
             ;; SMOOTH-WORLD
             (as-> level level
               (nth (iterate smooth-level-v1 {:level level
                                              :z z})
                    4)
               (:level level)
               (nth (iterate smooth-level-v2 {:level level
                                              :z z})
                    2)
               (:level level)))

     :desert (vec (map vec
                       (for [x (range width)]
                         (for [y (range height)]
                           (rj.c/map->Tile {:x x :y y :z z
                                            :entities [(rj.c/map->Entity {:id   nil
                                                                          :type :dune})]})))))
     :forest (let [level (vec
                         (map vec
                              (for [x (range width)]
                                (for [y (range height)]
                                  (update-in (rj.c/map->Tile {:x x :y y :z z
                                                              :entities [(rj.c/map->Entity {:id   nil
                                                                                            :type :forest-floor})]})
                                             [:entities] (fn [entities]
                                                           (if (< (rand-int 100) 45)
                                                             (conj entities
                                                                   (rj.c/map->Entity {:id   nil
                                                                                      :type :tree}))
                                                             entities)))))))]
             ;; SMOOTH-WORLD
             (as-> level level
                   (nth (iterate forest:smooth-level-v1 {:level level
                                                  :z z})
                        2)
                   (:level level)
                   (nth (iterate forest:smooth-level-v2 {:level level
                                                  :z z})
                        3)
                   (:level level)))

     :maze (let [level (vec (map vec
                                 (for [x (range width)]
                                   (for [y (range height)]
                                     (update-in (rj.c/map->Tile {:x x :y y :z z
                                                              :entities [(rj.c/map->Entity {:id   nil
                                                                                            :type :floor})]})
                                             [:entities] (fn [entities]
                                                           (if (< (rand-int 100) init-wall%)
                                                             (conj entities
                                                                   (rj.c/map->Entity {:id   (br.e/create-entity)
                                                                                      :type :maze-wall}))
                                                             entities)))))))]
             ;; CREATE MAZE
             (as-> level level
               (nth (iterate maze:smooth-level {:level level
                                                :z z})
                    5)
               (:level level))))))

(declare add-level)
(defn init-world
  [system]
  (let [z 0
        e-world  (br.e/create-entity)
        level0 (generate-random-level
                 rj.cfg/world-sizes z)
        level1 (generate-random-level
                 rj.cfg/world-sizes (inc z))]
    (-> system
        (rj.e/add-e e-world)
        (rj.e/add-c e-world (rj.c/map->World {:levels [level0 level1]
                                              :add-level-fn add-level}))
        (init-entities z)
        (add-portal z)
        (init-entities (inc z))

        (rj.e/add-c e-world (rj.c/map->Renderable {:render-fn rj.r/render-world
                                                   :args      {:view-port-sizes rj.cfg/view-port-sizes}})))))

(defn add-level
  [system z]
  (let [e-world (first (rj.e/all-e-with-c system :world))
        new-level (generate-random-level rj.cfg/world-sizes z)]
    (-> system
        (rj.e/upd-c e-world :world
                    (fn [c-world]
                      (update-in c-world [:levels]
                                 (fn [levels]
                                   (conj levels
                                         new-level)))))
        (init-entities z)
        (add-portal (dec z)))))

