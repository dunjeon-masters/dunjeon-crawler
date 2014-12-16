(ns rouje-like.world
  (:import [com.badlogic.gdx.graphics.g2d SpriteBatch TextureRegion])

  (:require [play-clj.g2d :refer :all]
            [play-clj.core :refer :all]

            [brute.entity :as br.e]

            [rouje-like.rendering :as rj.r]
            [rouje-like.components :as rj.c]
            [rouje-like.entity-wrapper :as rj.e]
            [rouje-like.utils :as rj.u :refer [?]]
            [rouje-like.attacker :as rj.atk]
            [rouje-like.items :as rj.items]
            [rouje-like.rooms :as rj.rm]
            [rouje-like.lichen :as rj.lc]
            [rouje-like.mimic :as rj.mi]
            [rouje-like.merchant :as rj.merch]
            [rouje-like.destructible :as rj.d]
            [rouje-like.bat :as rj.bt]
            [rouje-like.colossal-amoeba :as rj.ca]
            [rouje-like.hydra-head :as rj.hh]
            [rouje-like.hydra-neck :as rj.hn]
            [rouje-like.hydra-tail :as rj.ht]
            [rouje-like.hydra-rear :as rj.hr]
            [rouje-like.giant-amoeba :as rj.ga]
            [rouje-like.drake :as rj.dr]
            [rouje-like.necromancer :as rj.ne]
            [rouje-like.skeleton :as rj.sk]
            [rouje-like.slime :as rj.sl]
            [rouje-like.snake :as rj.snk]
            [rouje-like.spider :as rj.sp]
            [rouje-like.troll :as rj.tr]
            [rouje-like.willow-wisp :as rj.ww]
            [rouje-like.arrow-trap :as rj.arrow-trap]
            [rouje-like.spike-trap :as rj.spike-trap]
            [rouje-like.portal :as rj.p]
            [rouje-like.config :as rj.cfg]))

#_(in-ns 'rouje-like.world)
#_(use 'rouje-like.world :reload)

(defn- block->freqs
  [block]
  (frequencies
    (map (fn [tile]
           (:type (rj.u/tile->top-entity tile)))
         block)))

(defn- get-smoothed-tile
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

(defn- get-smoothed-col
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

(defn- smooth-level-v1
  [{:keys [level z]}]
  {:level (mapv (fn [x]
                  (get-smoothed-col level [x z] 2))
                (range (count level)))
   :z z})

(defn- smooth-level-v2
  [{:keys [level z]}]
  {:level (mapv (fn [x]
                  (get-smoothed-col level [x z] 1))
                (range (count level)))
   :z z})

(defn- forest:get-smoothed-tile
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
                                     (rj.c/map->Entity {:id   (if this-id
                                                                this-id
                                                                (br.e/create-entity))
                                                        :type :tree}))
                               entities)))))

(defn- forest:get-smoothed-col
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

(defn- forest:smooth-level-v1
  [{:keys [level z]}]
  {:level (mapv (fn [x]
                  (forest:get-smoothed-col level [x z] 2))
                (range (count level)))
   :z z})

(defn- forest:smooth-level-v2
  [{:keys [level z]}]
  {:level (mapv (fn [x]
                  (forest:get-smoothed-col level [x z] 1))
                (range (count level)))
   :z z})

(defn- entity-ize-level
  [system z]
  (letfn [(entity-ize-wall [system tile]
            (let [entities (:entities tile)
                  wall (filter #(rj.cfg/<walls> (:type %)) entities)]
              (if (seq wall)
                (let [wall (first wall)
                      wall-type (:type wall)
                      e-wall (:id wall)
                      hp (:hp (rj.cfg/entity->stats wall-type))]
                  (rj.e/system<<components
                    system e-wall
                    [[:position {:x (:x tile)
                                 :y (:y tile)
                                 :z z
                                 :type wall-type}]
                     [:destructible {:hp hp
                                     :max-hp hp
                                     :def 0
                                     :take-damage-fn (if (rj.cfg/<indestructible-walls> wall-type)
                                                       (fn [c e _ f s]
                                                         (rj.d/take-damage c e 0 f s))
                                                       rj.d/take-damage)}]]))
                system)))
          (entity-ize-spike-trap [system tile]
            (let [entities (:entities tile)
                  trap (filter #(#{:hidden-spike-trap} (:type %)) entities)]
              (if (seq trap)
                (let [trap (first trap)
                      trap-type (:type trap)
                      e-trap (:id trap)]
                  (rj.spike-trap/add-trap system tile e-trap))
                system)))
          (entity-ize-arrow-trap [system tile]
            (let [entities (:entities tile)
                  trap (filter #(#{:arrow-trap} (:type %)) entities)]
              (if (seq trap)
                (let [trap (first trap)
                      trap-type (:type trap)
                      e-trap (:id trap)]
                  (rj.arrow-trap/add-trap system tile e-trap))
                system)))
          (door-take-damage-fn [c-this e-this damage e-from system]
            (if-let [c-door (rj.e/get-c-on-e system e-this :door)]
              (as-> (rj.e/upd-c system e-this :position
                                (fn [c-position]
                                  (assoc-in c-position [:type]
                                            :open-door))) system
                (let [c-position (rj.e/get-c-on-e system e-this :position)
                      e-world (first (rj.e/all-e-with-c system :world))]
                  (rj.u/update-in-world system e-world
                                        [(:z c-position) (:x c-position) (:y c-position)]
                                        (fn [entities]
                                          (map
                                            #(if (#{e-this} (:id %))
                                               (assoc-in % [:type]
                                                         :open-door)
                                               %)
                                            entities)))))
              system))
          (entity-ize-door [system tile]
            (let [entities (:entities tile)
                  door (filter #(#{:door} (:type %)) entities)]
              (if (seq door)
                (let [door (first door)
                      door-type (:type door)
                      e-door (:id door)
                      target-tile tile]
                  (rj.e/system<<components
                    system e-door
                    [[:door {}]
                     [:position {:x (:x tile)
                                 :y (:y tile)
                                 :z z
                                 :type :door}]
                     [:destructible {:hp 1
                                     :max-hp 1
                                     :def 0
                                     :take-damage-fn
                                     door-take-damage-fn}]]))
                system)))]
    (let [e-world (first (rj.e/all-e-with-c system :world))
          c-world (rj.e/get-c-on-e system e-world :world)
          levels (:levels c-world)
          level (nth levels z)]
      (reduce (fn [system tile]
                (reduce (fn [system entity-izer]
                          (entity-izer system tile))
                        system [entity-ize-wall entity-ize-arrow-trap entity-ize-spike-trap entity-ize-door]))
              system (flatten level)))))

(defn- init-entities
  [system z]
  (-> system
      (as-> system
        (entity-ize-level system z))

      ;; Add Items: Gold, Torches...
      (as-> system
        (nth (iterate rj.items/add-gold {:system system :z z})
             (* (/ (:gold rj.cfg/world-entity->spawn%)
                   100)
                (apply * (vals rj.cfg/world-sizes))))
        (:system system)
        (do (? "core::add-gold: " (not (nil? system))) system))
      (as-> system
        (nth (iterate rj.items/add-torch {:system system :z z})
             (* (/ (:torch rj.cfg/world-entity->spawn%)
                   100)
                (apply * (vals rj.cfg/world-sizes))))
        (:system system)
        (do (? "core::add-torch " (not (nil? system))) system))
      (as-> system
        (nth (iterate rj.items/add-health-potion {:system system :z z})
             (* (/ (:hp rj.cfg/world-entity->spawn%)
                   100)
                (apply * (vals rj.cfg/world-sizes))))
        (:system system)
        (do (? "core::add-health-potion " (not (nil? system))) system))
      (as-> system
        (nth (iterate rj.items/add-magic-potion {:system system :z z})
             (* (/ (:mp rj.cfg/world-entity->spawn%)
                   100)
                (apply * (vals rj.cfg/world-sizes))))
        (:system system)
        (do (? "core::add-magic-potion " (not (nil? system))) system))

      ;; Spawn equipment
      (as-> system
        (nth (iterate rj.items/add-equipment {:system system :z z})
             (* (/ (:eq rj.cfg/world-entity->spawn%)
                   100)
                (apply * (vals rj.cfg/world-sizes))))
        (:system system)
        (do (? "core::add-equipment " (not (nil? system))) system))))

(defn- init-themed-entities
  [system z theme]
  (case theme
    :desert (as-> system system
              ;; Spawn Will-o-Wisps
              (nth (iterate rj.ww/add-willow-wisp {:system system :z z})
                   (* (max 0
                           (min 0.05
                                (/ (+ (:willow-wisp rj.cfg/mob->init-spawn%)
                                      (* 0.2 (- z (:willow-wisp rj.cfg/mob->init-floor))))
                                   100)))
                      (apply * (vals rj.cfg/world-sizes))))
              (:system system)
              (do (? "core::add-willow-wisp " (not (nil? system))) system)
              ;; Spawn Snakes
              (nth (iterate rj.snk/add-snake {:system system :z z})
                   (* (max 0
                           (min 0.05
                                (/ (+ (:snake rj.cfg/mob->init-spawn%)
                                      (* 0.2 (- z (:snake rj.cfg/mob->init-floor))))
                                   100)))
                      (apply * (vals rj.cfg/world-sizes))))
              (:system system)
              (do (? "core::add-snake " (not (nil? system))) system)
              ;;Spawn Necromancer
              (nth (iterate rj.ne/add-necro {:system system :z z})
                   (* (max 0
                           (min 0.05
                                (/ (+ (:necro rj.cfg/mob->init-spawn%)
                                      (* 0.2 (- z (:necro rj.cfg/mob->init-floor))))
                                   100)))
                      (apply * (vals rj.cfg/world-sizes))))
              (:system system)
              (do (? "core::add-necro " (not (nil? system))) system))
    :hydra-arena (as-> system system
                   ;;Spawn Hydra Head (currently using G.Amoeba spawn stats)
                   (:system (rj.hh/add-hydra-head {:system system :z z}))
                   (do (? "core::add-hydra-head " (not (nil? system))) system)
                   ;;Spawn Hydra Neck
                   (:system (rj.hn/add-hydra-neck {:system system :z z}))
                   (do (? "core::add-hydra-neck " (not (nil? system))) system)
                   ;;Spawn Hydra Tail
                   (:system (rj.ht/add-hydra-tail {:system system :z z}))
                   (do (? "core::add-hydra-tail " (not (nil? system))) system)
                   ;;Spawn Hydra Rear
                   (:system (rj.hr/add-hydra-rear {:system system :z z}))
                   (do (? "core::add-hydra-rear " (not (nil? system))) system))
    :amoeba-arena (as-> system system
                    ;; Spawn Colossal Amoeba
                    (nth (iterate rj.ca/add-colossal-amoeba {:system system :z z})
                         (* (/ (:boss rj.cfg/mob->init-spawn%)
                               100)
                            (apply * (vals rj.cfg/world-sizes))))
                    (:system system)
                    (do (? "core::add-colossal-amoeba " (not (nil? system))) system))
    :forest (as-> system system
              ;; Spawn Trolls
              (nth (iterate rj.tr/add-troll {:system system :z z})
                   (* (max 0
                           (min 0.05
                                (/ (+ (:troll rj.cfg/mob->init-spawn%)
                                      (* 0.2 (- z (:troll rj.cfg/mob->init-floor))))
                                   100)))
                      (apply * (vals rj.cfg/world-sizes))))
              (:system system)
              (do (? "core::add-troll " (not (nil? system))) system)
              ;; Spawn Spider
              (nth (iterate rj.sp/add-spider {:system system :z z})
                   (* (max 0
                           (min 0.05
                                (/ (+ (:spider rj.cfg/mob->init-spawn%)
                                      (* 0.2 (- z (:spider rj.cfg/mob->init-floor))))
                                   100)))
                      (apply * (vals rj.cfg/world-sizes))))
              (:system system)
              (do (? "core::add-spider " (not (nil? system))) system)
              ;; Spawn Giant Amoeba
              (nth (iterate rj.ga/add-giant-amoeba {:system system :z z})
                   (* (max 0
                           (min 0.05
                                (/ (+ (:giant-amoeba rj.cfg/mob->init-spawn%)
                                      (* 0.2 (- z (:giant-amoeba rj.cfg/mob->init-floor))))
                                   100)))
                      (apply * (vals rj.cfg/world-sizes))))
              (:system system)
              (do (? "core::add-giant-amoeba " (not (nil? system))) system))
    :maze (as-> system system
            ;; Spawn Slimes
            (nth (iterate rj.sl/add-slime {:system system :z z})
                 (* (max 0
                         (min 0.05
                              (/ (+ (:slime rj.cfg/mob->init-spawn%)
                                    (* 0.2 (- z (:slime rj.cfg/mob->init-floor))))
                                 100)))
                    (apply * (vals rj.cfg/world-sizes))))
            (:system system)
            (do (? "core::add-slime " (not (nil? system))) system)
            ;; Spawn Skeleton
            (nth (iterate rj.sk/add-skeleton {:system system :z z})
                 (* (max 0
                         (min 0.05
                              (/ (+ (:skeleton rj.cfg/mob->init-spawn%)
                                    (* 0.2 (- z (:skeleton rj.cfg/mob->init-floor))))
                                 100)))
                    (apply * (vals rj.cfg/world-sizes))))
            (:system system)
            (do (? "core::add-skeleton " (not (nil? system))) system)
            ;; Spawn Mimics
            (nth (iterate rj.mi/add-mimic {:system system :z z})
                 (* (max 0
                         (min
                           0.05 (/ (+ (:mimic rj.cfg/mob->init-spawn%)
                                      (* 0.2 (- z (:mimic rj.cfg/mob->init-floor))))
                                   100)))
                    (apply * (vals rj.cfg/world-sizes))))
            (:system system)
            (do (? "core::add-mimic " (not (nil? system))) system))
:cave (as-> system system
        ;; Spawn Lichen
        (nth (iterate rj.lc/add-lichen {:system system :z z})
             (* (/ (:lichen rj.cfg/mob->init-spawn%)
                   100)
                (apply * (vals rj.cfg/world-sizes))))
        (:system system)
        (do (? "core::add-lichen " (not (nil? system))) system)
        ;; Spawn Bats
        (nth (iterate rj.bt/add-bat {:system system :z z})
             (* (/ (:bat rj.cfg/mob->init-spawn%)
                   100)
                (apply * (vals rj.cfg/world-sizes))))
        (:system system)
        (do (? "core::add-bat " (not (nil? system))) system)
        ;; Spawn Drakes
        (nth (iterate rj.dr/add-drake {:system system :z z})
             (* (max 0
                     (min 0.05
                          (/ (+ (:drake rj.cfg/mob->init-spawn%)
                                (* 0.2 (- z (:drake rj.cfg/mob->init-floor))))
                             100)))
                (apply * (vals rj.cfg/world-sizes))))
        (:system system)
        (do (? "core::add-drake " (not (nil? system))) system))
system))

(defn- add-portal
  [system z]
  ;; Add portal
  (as-> system system
    (rj.p/add-portal {:system system :z z})
    (:system system))
    (do (? "core::add-portal " (not (nil? system))) system))

(def ^:private maze:direction8->offset
  {:left  [-1  0]
   :right [ 1  0]
   :up    [ 0 -1]
   :down  [ 0  1]
   :up-left    [-1 -1]
   :up-right   [ 1 -1]
   :down-left  [-1  1]
   :down-right [ 1  1]})

(def ^:private maze:direction4->offset
  {:left  [-1 0]
   :right [1  0]
   :up    [0 -1]
   :down  [0  1]})

(defn- maze:coords+offset
  [[x y] [dx dy]]
  [(+ x dx) (+ y dy)])

(defn- maze:get-neighbors-coords
  [origin target]
  (if origin
    (let [y-eq? (= (origin 0) (target 0))]
      (remove #(= (if y-eq? (origin 1) (origin 0))
                  (if y-eq? (% 1) (% 0)))
              (map maze:coords+offset
                   (repeat target) (vals maze:direction8->offset))))
    (map maze:coords+offset
         (repeat target) (vals maze:direction4->offset))))

(defn- maze:get-neighbors-of-type
  [level mark pos typ]
  (let [neighbors (maze:get-neighbors-coords mark pos)
        neighbors (map #(get-in level % nil)
                       neighbors)
        neighbors (filter identity neighbors)]
    (filter #(= typ (% 2)) neighbors)))

(defn- maze:get-first-valid-neighbor
  [level _ neighbors mark]
  (let [neighbors (shuffle neighbors)]
    (loop [candidate (first neighbors)
           candidates (rest neighbors)]
      (if candidate
        (let [[x y _] candidate
              candidate-pos [x y]
              cand-neighbors (maze:get-neighbors-of-type level mark candidate-pos :w)
              wall-count (count cand-neighbors)]
          (if (= 5 wall-count)
            candidate
            (recur (first candidates)
                   (rest candidates))))
        nil))))

(defn- maze:floor-it
  [tile]
  (assoc tile 2 :f))

(defn- maze:floor-in-level
  [level pos]
  (update-in level pos
             maze:floor-it))

(defn- maze:get-candidate
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

(defn- maze:growing-tree
  [level]
  (let [init-tile (rand-nth (rand-nth level))
        init-tile (maze:floor-it init-tile)
        [x y _] init-tile]
    (loop [cells [init-tile]
           level (maze:floor-in-level level [x y])]
      (if (seq cells)
        (let [candidate (maze:get-candidate cells :first/last 0.5)
              [x y _] candidate
              candidate-pos [x y]
              neighbors (maze:get-neighbors-of-type level nil candidate-pos :w)
              valid-neighbor (maze:get-first-valid-neighbor level cells neighbors candidate)
              [x y _] valid-neighbor
              valid-neighbor-pos [x y]]
          (if valid-neighbor
            (recur (conj cells (maze:floor-it valid-neighbor))
                   (maze:floor-in-level level valid-neighbor-pos))
            (recur (remove #(= % candidate) cells)
                   level)))
        level))))

(defn- maze:gen-walls
  [width height]
  (vec
    (for [x (range width)]
      (vec
        (for [y (range height)]
          [x y :w])))))

(defn- generate-maze
  [level [width height]]
  (let [maze (maze:growing-tree (maze:gen-walls width height))]
    (reduce (fn [level cell]
              (if (or (= :f (cell 2))
                      (and (= :w (cell 2))
                           (< (rand-int 100) 20)))
                (update-in level [(cell 0) (cell 1)]
                           (fn [tile]
                             (update-in tile [:entities]
                                        (fn [entities]
                                          (remove #(= :maze-wall (:type %))
                                                  entities)))))
                level))
            level (map vec (partition 3 (flatten maze))))))

(defn- generate-desert
  [level [width height]]
  (let [desert (:level (rj.rm/print-level
                         (rj.rm/gen-level-with-rooms
                           width height (/ (* width height) 100) 5)))]
    (reduce (fn [level cell]
              (case (cell 2)
                :w (update-in level [(cell 0) (cell 1)]
                              (fn [tile]
                                (update-in tile [:entities]
                                           conj (rj.c/map->Entity {:id (br.e/create-entity)
                                                                   :type :temple-wall}))))
                :f (update-in level [(cell 0) (cell 1)]
                              (fn [tile]
                                (update-in tile [:entities]
                                           (fn [entities]
                                             (remove #(#{:wall} (:type %))
                                                     entities)))))
                :st (update-in level [(cell 0) (cell 1)]
                              (fn [tile]
                                (update-in tile [:entities]
                                           conj (rj.c/map->Entity {:id (br.e/create-entity)
                                                                   :type :hidden-spike-trap
                                                                   :extra (cell 3)}))))
                :at (update-in level [(cell 0) (cell 1)]
                              (fn [tile]
                                (update-in tile [:entities]
                                           conj (rj.c/map->Entity {:id (br.e/create-entity)
                                                                   :type :arrow-trap
                                                                   :extra (cell 3)}))))
                :d (update-in level [(cell 0) (cell 1)]
                              (fn [tile]
                                (update-in tile [:entities]
                                           conj (rj.c/map->Entity {:id (br.e/create-entity)
                                                                   :type :door}))))
                level))
            level (map vec (partition 4 (flatten desert))))))

(defn generate-random-level
  ([level-sizes z]
   (if (zero? (mod z 5))
     (let [boss-levels [:hydra-arena :amoeba-arena]
           boss-level (rand-nth boss-levels)]
       {:type boss-level
        :level (generate-random-level level-sizes z boss-level)})
     (let [world-types [:cave :desert :maze :forest]
           world-type (rand-nth world-types)]
       {:type world-type
        :level (generate-random-level level-sizes z world-type)})))

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
                                                           (if (< (rand-int 100)
                                                                  (:wall rj.cfg/world-entity->spawn%))
                                                             (conj entities
                                                                   (rj.c/map->Entity {:id   (br.e/create-entity)
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

     :desert (let [level (vec
                           (map vec
                                (for [x (range width)]
                                  (for [y (range height)]
                                    (rj.c/map->Tile {:x x :y y :z z
                                                     :entities [(rj.c/map->Entity {:id   nil
                                                                                   :type :dune})]})))))]
               (generate-desert level [width height]))

     :merchant (vec
                 (map vec
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
                                                             (if (< (rand-int 100)
                                                                    (:wall rj.cfg/world-entity->spawn%))
                                                               (conj entities
                                                                     (rj.c/map->Entity {:id   (br.e/create-entity)
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
             (generate-maze level [width height]))

     :hydrarena (vec (map vec
                          (for [x (range width)]
                            (for [y (range height)]
                              (rj.c/map->Tile {:x x :y y :z z
                                               :entities [(rj.c/map->Entity {:id   nil
                                                                             :type :dune})]})))))
     :amoebarena (vec (map vec
                           (for [x (range width)]
                             (for [y (range height)]
                               (rj.c/map->Tile {:x x :y y :z z
                                                :entities [(rj.c/map->Entity {:id   nil
                                                                              :type :dune})]})))))

     )))

;;;; MERCHANT LEVEL CODE
(defn generate-merchant-level
  []
  (generate-random-level rj.cfg/world-sizes 0 :merchant))

(defn add-merch-items
  [system]
  (reduce (fn [sys tile]
            (:system (rj.items/add-purchasable sys tile)))
          system
          (rj.merch/merchant-item-tiles system)))

(defn remove-merch-items
  [system]
  (reduce (fn [sys {x :x y :y}]
            (rj.items/remove-item sys [0 x y] :purchasable))
          system
          rj.cfg/merchant-item-pos))

(defn reset-merch-level
  [system [z x y]]
  (let [e-world (first (rj.e/all-e-with-c system :world))
        c-world (rj.e/get-c-on-e system e-world :world)
        levels (:levels c-world)
        level (nth levels z)
        target-tile (get-in level [x y])]
    (as-> system system
          (remove-merch-items system)
          (add-merch-items system)
          (:system (rj.p/add-portal system (rj.merch/merchant-portal-tile system) target-tile :portal)))))

(defn add-merch-portal
  [system z]
  (let [e-world (first (rj.e/all-e-with-c system :world))
        c-world (rj.e/get-c-on-e system e-world :world)
        levels (:levels c-world)
        level (nth levels z)
        merch-level (nth levels 0)

        merch-player-tile (rj.merch/merchant-player-tile system)
        get-rand-tile (fn [level]
                        (get-in level [(rand-int (count level))
                                       (rand-int (count (first level)))]))]
    (loop [portal-tile (get-rand-tile level)]
      (if (rj.cfg/<floors> (:type (rj.u/tile->top-entity portal-tile)))
        (:system (rj.p/add-portal system portal-tile merch-player-tile :m-portal))
        (recur (get-rand-tile level))))))

(declare add-level)
(defn init-world
  [system]
  (let [z 1
        e-world  (br.e/create-entity)
        merch-level (generate-merchant-level)
        level1 (generate-random-level
                rj.cfg/world-sizes z)
        type1 (:type level1)
        level1 (:level level1)
        level2 (generate-random-level
                rj.cfg/world-sizes (inc z))
        type2 (:type level2)
        level2 (:level level2)]
    (-> system
        (rj.e/add-e e-world)
        (rj.e/add-c e-world (rj.c/map->World {:levels [merch-level level1 level2]
                                              :add-level-fn add-level
                                              :merchant-level-fn reset-merch-level}))
        (rj.merch/init-merchant 0)
        (init-entities z)
        (init-themed-entities z type1)
        (add-portal z)
        (add-merch-portal z)
        (init-entities (inc z))
        (init-themed-entities (inc z) type2)

        (rj.e/add-c e-world (rj.c/map->Renderable {:render-fn rj.r/render-world
                                                   :args      {:view-port-sizes rj.cfg/view-port-sizes}})))))

(defn add-level
  [system z]
  (let [e-player (first (rj.e/all-e-with-c system :player))
        player-pos (rj.e/get-c-on-e system e-player :position)
        player-z (:z player-pos)

        e-world (first (rj.e/all-e-with-c system :world))
        levels (:levels (rj.e/get-c-on-e system e-world :world))
        n-levels (count levels)]
    (if (= player-z (dec n-levels))
      (let [new-level (generate-random-level rj.cfg/world-sizes z)
            newtype (:type new-level)
            new-level (:level new-level)]
        (-> system
            (rj.e/upd-c e-world :world
                        (fn [c-world]
                          (update-in c-world [:levels]
                                     (fn [levels]
                                       (conj levels
                                             new-level)))))
            (init-entities z)
            (init-themed-entities z newtype)
            (add-merch-portal (dec z))
            (add-portal (dec z))))
      system)))
