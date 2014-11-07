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

(defn ^:private entity-ize-walls
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
                        e-wall (:id wall)]
                    (rj.e/system<<components
                      system e-wall
                      [[:position {:x (:x tile)
                                   :y (:y tile)
                                   :z z
                                   :type wall-type}]
                       [:destructible {:hp (:hp (rj.cfg/wall->stats wall-type))
                                       :def 0
                                       :take-damage-fn (if (= :maze-wall wall-type)
                                                        (fn [c e _ f s]
                                                          (rj.d/take-damage c e 0 f s))
                                                        rj.d/take-damage)}]]))
                  system)))
            system (flatten level))))

(def ^:private init-wall% 45)
(def ^:private init-torch% 2)
(def ^:private init-gold% 5)
(def ^:private init-lichen% 1)
(def ^:private init-bat% 1)
(def ^:private init-skeleton% 1)

(defn ^:private init-entities
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

(defn ^:private add-portal
  [system z]
  ;; Add portal
  (as-> system system
    (do (println "core::add-portal " (not (nil? system))) system)
    (rj.p/add-portal {:system system :z z})
    (:system system)))

(def ^:private direction8->offset
  {:left  [-1 0]
   :right [1  0]
   :up    [0 -1]
   :down  [0  1]
   :up-left [-1 -1]
   :up-right [1 -1]
   :down-left [-1 1]
   :down-right [1 1]})

(def ^:private direction4->offset
  {:left  [-1 0]
   :right [1  0]
   :up    [0 -1]
   :down  [0  1]})

(defn ^:private coords+offset
  [[x y] [dx dy]]
  [(+ x dx) (+ y dy)])

(defn ^:private get-neighbors-coords
  [origin target]
  (if origin
    (let [y-eq? (= (origin 0) (target 0))]
      (remove #(= (if y-eq? (origin 1) (origin 0))
                  (if y-eq? (% 1) (% 0)))
              (map coords+offset
                   (repeat target) (vals direction8->offset))))
    (map coords+offset
         (repeat target) (vals direction4->offset))))

(defn ^:private get-neighbors-of-type
  [level mark pos typ]
  (let [neighbors (get-neighbors-coords mark pos)
        neighbors (map #(get-in level % nil)
                       neighbors)
        neighbors (filter identity neighbors)]
    (filter #(= typ (% 2)) neighbors)))

(defn ^:private get-first-valid-neighbor
  [level cells neighbors mark]
  (let [neighbors (shuffle neighbors)]
    (loop [candidate (first neighbors)
           candidates (rest neighbors)]
      (if candidate
        (let [[x y t] candidate
              candidate-pos [x y]
              cand-neighbors (get-neighbors-of-type level mark candidate-pos :w)
              wall-count (count cand-neighbors)]
          (if (= 5 wall-count)
            candidate
            (recur (first candidates)
                   (rest candidates))))
        nil))))

(defn ^:private floor-it
  [tile]
  (assoc tile 2 :f))

(defn ^:private floor-in-level
  [level pos]
  (update-in level pos
             floor-it))

(defn ^:private growing-tree
  [level]
  (let [init-tile (rand-nth (rand-nth level))
        init-tile (floor-it init-tile)
        [x y t] init-tile]
    (loop [cells [init-tile]
           level (floor-in-level level [x y])]
      (if (seq cells)
        (let [candidate (rand-nth cells)
              [x y t] candidate
              candidate-pos [x y]
              neighbors (get-neighbors-of-type level nil candidate-pos :w)
              valid-neighbor (get-first-valid-neighbor level cells neighbors candidate)
              [x y t] valid-neighbor
              valid-neighbor-pos [x y]]
          (if valid-neighbor
            (recur (conj cells (floor-it valid-neighbor))
                   (floor-in-level level valid-neighbor-pos))
            (recur (remove #(= % candidate) cells)
                   level)))
        level))))

(defn ^:private gen-walls
  [width height]
  (vec
    (for [x (range width)]
      (vec
        (for [y (range height)]
          [x y :w])))))

(defn ^:private generate-maze
  [level]
  (let [maze (growing-tree (gen-walls (count level) (count (first level))))]
    (reduce (fn [level cell]
              (if (= :f (cell 2))
                (update-in level [(cell 0) (cell 1)]
                           (fn [tile]
                             (update-in tile [:entities]
                                        (fn [entities]
                                          (remove #(= :maze-wall (:type %))
                                                  entities)))))
                level))
            level (map vec (partition 3 (flatten maze))))))

;[:cave :desert :maze]
(defn ^:private generate-random-level
  ([level-sizes z]
   (let [world-types [#_:cave #_:desert :maze]]
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

     :maze (let [level (vec
                         (map vec
                              (for [x (range width)]
                                (for [y (range height)]
                                  (rj.c/map->Tile {:x x :y y :z z
                                                   :entities [(rj.c/map->Entity {:id nil
                                                                                 :type :floor})
                                                              (rj.c/map->Entity {:id   (br.e/create-entity)
                                                                                 :type :maze-wall})]})))))]
             ;; CREATE MAZE
             (generate-maze level)))))

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

