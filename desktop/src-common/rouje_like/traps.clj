(ns rouje-like.traps
  (:require [brute.entity :as br.e]

            [rouje-like.attacker :as rj.atk]
            [rouje-like.entity-wrapper :as rj.e]
            [rouje-like.components :as rj.c :refer [can-move? move
                                                    can-attack? attack]]
            [rouje-like.utils :as rj.u :refer [?]]
            [rouje-like.config :as rj.cfg]))

(declare process-input-tick)

(defn add-trap
  ([{:keys [system z]}]
   (let [e-world (first (rj.e/all-e-with-c system :world))
         c-world (rj.e/get-c-on-e system e-world :world)
         levels (:levels c-world)
         world (nth levels z)

         get-rand-tile (fn [world]
                         (get-in world [(rand-int (count world))
                                        (rand-int (count (first world)))]))]
     (loop [target-tile (get-rand-tile world)]
       (if (rj.cfg/<floors> (:type (rj.u/tile->top-entity target-tile)))
         (add-trap system target-tile)
         (recur (get-rand-tile world))))))

  ([system target-tile]
   (add-trap system target-tile (rand-nth rj.cfg/trap-types)))

  ([system target-tile trap-type]
   (let [e-world (first (rj.e/all-e-with-c system :world))
         e-trap (br.e/create-entity)
         system (rj.u/update-in-world system e-world
                                      [(:z target-tile) (:x target-tile) (:y target-tile)]
                                      (fn [entities]
                                        (vec
                                          (conj
                                            (remove #(#{:wall} (:type %)) entities)
                                            (rj.c/map->Entity {:id   e-trap
                                                               :type trap-type})))))]
     {:system (add-trap system target-tile trap-type e-trap)
      :z (:z target-tile)}))

  ([system target-tile trap-type e-trap]
   (if (= :trap trap-type)
     (rj.e/system<<components
       system e-trap
       [[:trap {}]
        [:position {:x    (:x target-tile)
                    :y    (:y target-tile)
                    :z    (:z target-tile)
                    :type trap-type}]
        [:attacker {:atk              (:atk rj.cfg/trap-stats)
                    :can-attack?-fn   rj.atk/can-attack?
                    :attack-fn        rj.atk/attack
                    :status-effects   []
                    :is-valid-target? (partial #{:player})}]
        [:tickable {:tick-fn process-input-tick
                    :pri 0}]
        [:broadcaster {:name-fn (constantly (str "the "
                                                 (name trap-type)))}]])
     (let [dir ({:down :up
                 :up   :down
                 :left :left
                 :right :right} (:dir (:extra (rj.u/tile->top-entity target-tile))))]
       (rj.e/system<<components
         system e-trap
         [[:arrow-trap {:dir dir}]
          [:position {:x    (:x target-tile)
                      :y    (:y target-tile)
                      :z    (:z target-tile)
                      :type trap-type}]
          [:sight {:distance 4}]
          [:attacker {:atk              (:atk rj.cfg/trap-stats)
                      :can-attack?-fn   rj.atk/can-attack?
                      :attack-fn        rj.atk/attack
                      :status-effects   []
                      :is-valid-target? (partial #{:player})}]
          [:tickable {:tick-fn process-input-tick
                      :pri 0}]
          [:broadcaster {:name-fn (constantly (str "the "
                                                   (name trap-type)))}]])))))

(defn process-input-tick
  [_ e-this system]
  (let [c-position (rj.e/get-c-on-e system e-this :position)
        this-pos [(:x c-position) (:y c-position)]
        c-mobile (rj.e/get-c-on-e system e-this :mobile)

        e-player (first (rj.e/all-e-with-c system :player))
        c-player-pos (rj.e/get-c-on-e system e-player :position)
        player-pos [(:x c-player-pos) (:y c-player-pos)]

        e-world (first (rj.e/all-e-with-c system :world))
        c-world (rj.e/get-c-on-e system e-world :world)
        levels (:levels c-world)
        level (nth levels (:z c-position))

        c-sight (rj.e/get-c-on-e system e-this :sight)

        c-attacker (rj.e/get-c-on-e system e-this :attacker)

        c-arrow-trap (rj.e/get-c-on-e system e-this :arrow-trap)

        target-tile (if
                      (if c-arrow-trap
                        (let [dir (:dir c-arrow-trap)
                              [t-x t-y] this-pos
                              [p-x p-y] player-pos]
                          (and
                            (case dir
                              :up    (and (= t-x p-x)
                                          (< t-y p-y))
                              :down  (and (= t-x p-x)
                                          (> t-y p-y))
                              :left  (and (= t-y p-y)
                                          (> t-x p-x))
                              :right (and (= t-y p-y)
                                          (< t-x p-x)))
                            (rj.u/can-see? level (:distance c-sight) this-pos player-pos)))
                        (rj.u/can-see? level (:distance c-sight) this-pos player-pos))
                      (get-in level player-pos nil)
                      nil)
        e-target (:id (rj.u/tile->top-entity target-tile))]
    (if (not (nil? target-tile))
      (-> (cond
            (can-attack? c-attacker e-this e-target system)
            (attack c-attacker e-this e-target system)

            :else system))
      system)))

