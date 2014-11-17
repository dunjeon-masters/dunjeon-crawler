(ns rouje-like.merchant
  (:require [brute.entity :as br.e]

            [rouje-like.components :as rj.c]
            [rouje-like.entity-wrapper :as rj.e]
            [rouje-like.config :as rj.cfg]
            [rouje-like.utils :as rj.u :refer [?]]))

(def merchant-pos
  {:x 5
   :y 7})

(defn add-merchant
  ([{:keys [system z]}]
     (let [e-world (first (rj.e/all-e-with-c system :world))
           c-world (rj.e/get-c-on-e system e-world :world)
           levels (:levels c-world)
           world (nth levels z)

           merchant-tile (get-in world [(:x merchant-pos) (:y merchant-pos)])]
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
