(ns rouje-like.giant-amoeba
  (:require [brute.entity :as br.e]

            [rouje-like.entity-wrapper :as rj.e]
            [rouje-like.utils :as rj.u :refer [?]]
            [rouje-like.components :as rj.c :refer [can-move? move
                                                    can-attack? attack]]
            [rouje-like.mobile :as rj.m]
            [rouje-like.destructible :as rj.d]
            [rouje-like.tickable :as rj.t]
            [rouje-like.attacker :as rj.atk]
            [rouje-like.config :as rj.cfg]
            [rouje-like.status-effects :as rj.stef]
            [rouje-like.messaging :as rj.msg]
            [rouje-like.large-amoeba :as rj.la]))

(defn on-death
  [_ e-this system]
  (let [c-position (rj.e/get-c-on-e system e-this :position)
        this-pos [(:x c-position) (:y c-position)]

        e-world (first (rj.e/all-e-with-c system :world))
        c-world (rj.e/get-c-on-e system e-world :world)
        levels (:levels c-world)
        world (nth levels (:z c-position))

        ring-coords (rj.u/get-ring-around world this-pos 1)]
    ;; spawn large amoebas at the first
    ;; (:split-rate (rj.cfg/entity->stats :giant-amoeba))
    ;; open spots around the giant amoeba
    (loop [ring-tiles ring-coords
           amoebas 0
           spawn-tiles nil]
      (let [target-tile (first ring-tiles)]
        (cond (empty? ring-tiles)
              system

              (= amoebas (:split-rate (rj.cfg/entity->stats :giant-amoeba)))
              (as-> system system
                    (rj.msg/add-msg system :static (format "the giant amoeba split into %d large amoebas"
                                                           amoebas))
                    (reduce (fn [sys tile]
                              (:system (rj.la/add-large-amoeba sys tile)))
                            system spawn-tiles))

              (rj.cfg/<floors> (:type (rj.u/tile->top-entity target-tile)))
              (recur (rest ring-tiles) (inc amoebas) (conj spawn-tiles target-tile))

              :else
              (recur (rest ring-tiles) amoebas spawn-tiles))))))

(defn add-giant-amoeba
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
         (add-giant-amoeba system target-tile)
         (recur (get-rand-tile world))))))
  ([system target-tile]
   (let [e-world (first (rj.e/all-e-with-c system :world))
         e-giant-amoeba (br.e/create-entity)
         system (rj.u/update-in-world system e-world [(:z target-tile) (:x target-tile) (:y target-tile)]
                                      (fn [entities]
                                        (vec
                                          (conj
                                            (remove #(#{:wall} (:type %)) entities)
                                            (rj.c/map->Entity {:id   e-giant-amoeba
                                                               :type :giant-amoeba})))))]
     {:system (rj.e/system<<components
                system e-giant-amoeba
                [[:giant-amoeba {}]
                 [:position {:x    (:x target-tile)
                             :y    (:y target-tile)
                             :z    (:z target-tile)
                             :type :giant-amoeba}]
                 [:mobile {:can-move?-fn rj.m/can-move?
                           :move-fn      rj.m/move}]
                 [:sight {:distance 2}]
                 [:attacker {:atk              (:atk (rj.cfg/entity->stats :giant-amoeba))
                             :can-attack?-fn   rj.atk/can-attack?
                             :attack-fn        rj.atk/attack
                             :status-effects   [{:type :paralysis
                                                 :duration 2
                                                 :value 1
                                                 :e-from e-giant-amoeba
                                                 :apply-fn rj.stef/apply-paralysis}]
                             :is-valid-target? (partial #{:player})}]
                 [:destructible {:hp         (:hp  (rj.cfg/entity->stats :giant-amoeba))
                                 :max-hp     (:hp  (rj.cfg/entity->stats :giant-amoeba))
                                 :def        (:def (rj.cfg/entity->stats :giant-amoeba))
                                 :can-retaliate? false
                                 :status-effects []
                                 :take-damage-fn rj.d/take-damage
                                 :on-death-fn on-death}]
                 [:killable {:experience (:exp (rj.cfg/entity->stats :giant-amoeba))}]
                 [:tickable {:tick-fn rj.t/process-input-tick
                             :pri 0}]
                 [:broadcaster {:name-fn (constantly "the giant-amoeba")}]])
      :z (:z target-tile)})))
