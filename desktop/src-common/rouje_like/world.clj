(ns rouje-like.world
  (:import [com.badlogic.gdx.graphics.g2d SpriteBatch TextureRegion])

  (:require [play-clj.g2d :refer :all]
            [play-clj.core :refer :all]

            [brute.entity :as br.e]

            [rouje-like.arrow-trap :as rj.arrow-trap]
            [rouje-like.attacker :as rj.atk]
            [rouje-like.bat :as rj.bt]
            [rouje-like.cave :as rj.cave]
            [rouje-like.colossal-amoeba :as rj.ca]
            [rouje-like.components :as rj.c]
            [rouje-like.config :as rj.cfg]
            [rouje-like.desert :as rj.desert]
            [rouje-like.destructible :as rj.d]
            [rouje-like.drake :as rj.dr]
            [rouje-like.entity-wrapper :as rj.e]
            [rouje-like.forest :as rj.forest]
            [rouje-like.giant-amoeba :as rj.ga]
            [rouje-like.hydra-head :as rj.hh]
            [rouje-like.hydra-neck :as rj.hn]
            [rouje-like.hydra-rear :as rj.hr]
            [rouje-like.hydra-tail :as rj.ht]
            [rouje-like.items :as rj.items]
            [rouje-like.lichen :as rj.lc]
            [rouje-like.maze :as rj.maze]
            [rouje-like.merchant :as rj.merch]
            [rouje-like.mimic :as rj.mi]
            [rouje-like.necromancer :as rj.ne]
            [rouje-like.portal :as rj.p]
            [rouje-like.rendering :as rj.r]
            [rouje-like.rooms :as rj.rm]
            [rouje-like.skeleton :as rj.sk]
            [rouje-like.slime :as rj.sl]
            [rouje-like.snake :as rj.snk]
            [rouje-like.spider :as rj.sp]
            [rouje-like.spike-trap :as rj.spike-trap]
            [rouje-like.troll :as rj.tr]
            [rouje-like.utils :as rj.u :refer [?]]
            [rouje-like.willow-wisp :as rj.ww]))

#_(in-ns 'rouje-like.world)
#_(use 'rouje-like.world :reload)

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
                                     :can-retaliate? false
                                     :status-effects nil
                                     :on-death-fn nil
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
                                     :can-retaliate? false
                                     :status-effects nil
                                     :on-death-fn nil
                                     :take-damage-fn door-take-damage-fn}]]))
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

(defn generate-random-level
  ([level-sizes z]
   (if (zero? (mod z (rj.cfg/k->boss-config :every-?-levels)))
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
     :cave (rj.cave/generate-cave
             [width height] z)

     :desert (rj.desert/generate-desert
               [width height] z)

     :merchant (vec
                 (map vec
                      (for [x (range width)]
                        (for [y (range height)]
                          (rj.c/map->Tile {:x x :y y :z z
                                           :entities [(rj.c/map->Entity {:id   nil
                                                                         :type :dune})]})))))

     :forest (rj.forest/generate-forest
               [width height] z)

     :maze (rj.maze/generate-maze
             [width height] z)

     :hydra-arena (vec
                    (map vec
                         (for [x (range width)]
                           (for [y (range height)]
                             (rj.c/map->Tile {:x x :y y :z z
                                              :entities [(rj.c/map->Entity {:id   nil
                                                                            :type :dune})]})))))
     :amoeba-arena (vec
                     (map vec
                          (for [x (range width)]
                            (for [y (range height)]
                              (rj.c/map->Tile {:x x :y y :z z
                                               :entities [(rj.c/map->Entity {:id   nil
                                                                             :type :dune})]}))))))))

(declare add-level)
(defn init-world
  [system]
  (let [z 1
        e-world (br.e/create-entity)
        merch-level (generate-random-level
                      rj.cfg/world-sizes 0
                      :merchant)
        {type1 :type
         level1 :level} (generate-random-level
                          rj.cfg/world-sizes z)
        {type2 :type
         level2 :level} (generate-random-level
                          rj.cfg/world-sizes (inc z))]
    (as-> system system
      (rj.e/add-e system e-world)
      (rj.e/add-c system e-world (rj.c/map->World {:levels [merch-level level1 level2]
                                                   :add-level-fn add-level
                                                   :merchant-level-fn rj.merch/reset-merch-level}))
      (rj.merch/init-merchant system 0)
      (init-entities system z)
      (init-themed-entities system z type1)
      (:system
        (rj.p/add-portal {:system system
                          :z z}))
      (rj.merch/add-merch-portal system z)
      (init-entities system (inc z))
      (init-themed-entities system (inc z) type2)

      (rj.e/add-c system e-world (rj.c/map->Renderable {:render-fn rj.r/render-world
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
      (let [{:keys [type level]} (generate-random-level rj.cfg/world-sizes z)]
        (as-> system system
          (rj.e/upd-c system e-world :world
                      (fn [c-world]
                        (update-in c-world [:levels]
                                   (fn [levels]
                                     (conj levels
                                           level)))))
          (init-entities system z)
          (init-themed-entities system z type)
          (rj.merch/add-merch-portal system (dec z))
          (:system (rj.p/add-portal {:system system
                                     :z (dec z)}))))
      system)))
