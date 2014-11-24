(ns rouje-like.merchant
  (:require [brute.entity :as br.e]

            [rouje-like.components :as rj.c]
            [rouje-like.entity-wrapper :as rj.e]
            [rouje-like.config :as rj.cfg]
            [rouje-like.utils :as rj.u :refer [?]]))

(defn merchant-tile
  [system]
  (let [e-world (first (rj.e/all-e-with-c system :world))
        c-world (rj.e/get-c-on-e system e-world :world)
        levels (:levels c-world)
        merch-level (nth levels 0)
        merch-pos rj.cfg/merchant-pos]
    (get-in merch-level [(:x merch-pos) (:y merch-pos)])))

(defn merchant-portal-tile
  [system]
  (let [e-world (first (rj.e/all-e-with-c system :world))
        c-world (rj.e/get-c-on-e system e-world :world)
        levels (:levels c-world)
        merch-level (nth levels 0)
        merch-portal-pos rj.cfg/merchant-portal-pos]
    (get-in merch-level [(:x merch-portal-pos) (:y merch-portal-pos)])))

(defn merchant-player-tile
  [system]
  (let [e-world (first (rj.e/all-e-with-c system :world))
        c-world (rj.e/get-c-on-e system e-world :world)
        levels (:levels c-world)
        merch-level (nth levels 0)
        merch-player-pos rj.cfg/merchant-player-pos]
    (get-in merch-level [(:x merch-player-pos) (:y merch-player-pos)])))

(defn merchant-item-tiles
  [system]
  (let [e-world (first (rj.e/all-e-with-c system :world))
        c-world (rj.e/get-c-on-e system e-world :world)
        levels (:levels c-world)
        merch-level (nth levels 0)
        merch-item-pos rj.cfg/merchant-item-pos]
    (map (fn [{x :x y :y}]
           (get-in merch-level [x y]))
         merch-item-pos)))

(defn add-merchant
  ([{:keys [system z]}]
     (let [e-world (first (rj.e/all-e-with-c system :world))
           c-world (rj.e/get-c-on-e system e-world :world)
           levels (:levels c-world)
           world (nth levels z)

           merchant-tile (merchant-tile system)]
       (add-merchant system merchant-tile)))
  ([system target-tile]
     (let [e-world (first (rj.e/all-e-with-c system :world))
           e-merchant (br.e/create-entity)
           system (rj.u/update-in-world system e-world [(:z target-tile) (:x target-tile) (:y target-tile)]
                                        (fn [entities]
                                          (vec
                                           (conj
                                            (remove #(#{:wall} (:type %)) entities)
                                            (rj.c/map->Entity {:id   e-merchant
                                                               :type :merchant})))))]
       {:system (rj.e/system<<components
                 system e-merchant
                 [[:merchant {}]
                  [:position {:x    (:x target-tile)
                              :y    (:y target-tile)
                              :z    (:z target-tile)
                              :type :merchant}]
                  [:broadcaster {:name-fn (constantly "the merchant")}]])
        :z (:z target-tile)})))

(defn init-merchant
  [system z]
  (as-> system system
        (add-merchant {:system system :z z})
        (:system system)))


