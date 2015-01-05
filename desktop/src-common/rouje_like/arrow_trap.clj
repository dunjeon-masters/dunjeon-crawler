(ns rouje-like.arrow-trap
  (:require [brute.entity :as br.e]

            [rouje-like.attacker :as rj.atk]
            [rouje-like.entity-wrapper :as rj.e]
            [rouje-like.components :as rj.c
             :refer [can-move? move
                     can-attack? attack
                     ->3DPoint]]
            [rouje-like.utils :as rj.u :refer [?]]
            [rouje-like.messaging :as rj.msg]
            [rouje-like.config :as rj.cfg]))

#_(use 'rouje-like.arrow-trap :reload)

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
   (let [e-world (first (rj.e/all-e-with-c system :world))
         e-trap (br.e/create-entity)
         system (rj.u/update-in-world system e-world
                                      (->3DPoint target-tile)
                                      (fn [entities]
                                        (vec
                                          (conj
                                            (remove #(#{:wall} (:type %)) entities)
                                            (rj.c/map->Entity {:id   e-trap
                                                               :type :arrow-trap})))))]
     {:system (add-trap system target-tile e-trap)
      :z (:z target-tile)}))

  ([system target-tile e-trap]
   (let [dir ({:down :up
               :up   :down
               :left :left
               :right :right} (:dir (:extra (rj.u/tile->top-entity target-tile))))]
       (rj.e/system<<components
         system e-trap
         [[:arrow-trap {:dir dir
                        :ready? false}]
          [:position {:x    (:x target-tile)
                      :y    (:y target-tile)
                      :z    (:z target-tile)
                      :type :arrow-trap}]
          [:sight {:distance (:sight (rj.cfg/entity->stats :arrow-trap))}]
          [:attacker {:atk              (:atk (rj.cfg/entity->stats :arrow-trap))
                      :can-attack?-fn   rj.atk/can-attack?
                      :attack-fn        rj.atk/attack
                      :status-effects   []
                      :is-valid-target? (partial #{:player})}]
          [:tickable {:tick-fn process-input-tick
                      :extra-tick-fn nil
                      :pri 0}]
          [:broadcaster {:name-fn (constantly (str "the "
                                                   (name :arrow-trap)))}]]))))

(defn process-input-tick
  [_ e-this system]
  (let [{:keys [x y z]} (rj.e/get-c-on-e system e-this :position)
        this-pos [x y]
        c-mobile (rj.e/get-c-on-e system e-this :mobile)

        e-player (first (rj.e/all-e-with-c system :player))
        c-player-pos (rj.e/get-c-on-e system e-player :position)
        player-pos [(:x c-player-pos) (:y c-player-pos)]

        e-world (first (rj.e/all-e-with-c system :world))
        c-world (rj.e/get-c-on-e system e-world :world)
        levels (:levels c-world)
        level (nth levels z)

        c-sight (rj.e/get-c-on-e system e-this :sight)
        c-attacker (rj.e/get-c-on-e system e-this :attacker)
        c-arrow-trap (rj.e/get-c-on-e system e-this :arrow-trap)

        can-see-player? (if c-arrow-trap
                          (let [dir (:dir c-arrow-trap)
                                this-pos+ (rj.u/coords+offset this-pos (rj.u/direction->offset dir))
                                [t-x t-y] this-pos
                                [p-x p-y] player-pos
                                can-see? (rj.u/can-see? level (:distance c-sight) this-pos+ player-pos)]
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
                              can-see?))
                          (rj.u/can-see? level (:distance c-sight) this-pos player-pos))

        target-tile (get-in level player-pos nil)]
    (if can-see-player?
      (let [e-target (:id (rj.u/tile->top-entity target-tile))]
        (if (:ready? c-arrow-trap)
          (cond
            (can-attack? c-attacker e-this e-target system)
            (attack c-attacker e-this e-target system)

            :else system)
          (as-> (rj.e/upd-c system e-this :arrow-trap
                            (fn [c-arrow-trap]
                              (assoc c-arrow-trap :ready? true))) system
            (rj.msg/add-msg system :static
                            (format "%s hears a ticking noise"
                                    (let [player-c-broadcaster (rj.e/get-c-on-e system e-target :broadcaster)]
                                      ((:name-fn player-c-broadcaster) system e-target)))))))
      (rj.e/upd-c system e-this :arrow-trap
                  (fn [c-arrow-trap]
                    (assoc c-arrow-trap :ready? false))))))
