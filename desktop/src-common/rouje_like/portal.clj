(ns rouje-like.portal
  (:require [brute.entity :as br.e]
            [rouje-like.entity-wrapper :as rj.e]
            [rouje-like.utils :as rj.u :refer [?]]
            [rouje-like.components :as rj.c]
            [rouje-like.config :as rj.cfg]))

(defn add-portal
  ([{:keys [system z]}]
   (let [e-world (first (rj.e/all-e-with-c system :world))
         c-world (rj.e/get-c-on-e system e-world :world)
         levels (:levels c-world)
         level (nth levels z)
         level+ (nth levels (inc z))

         get-rand-tile (fn [level]
                         (get-in level [(rand-int (count level))
                                        (rand-int (count (first level)))]))]
     (loop [portal-tile (get-rand-tile level)
            target-tile (get-rand-tile level+)]
       (let [portal-tile-good? (rj.cfg/<floors> (:type (rj.u/tile->top-entity portal-tile)))
             target-tile-good? (rj.cfg/<floors> (:type (rj.u/tile->top-entity target-tile)))]
         (cond (and portal-tile-good? target-tile-good?)
               (add-portal system portal-tile target-tile :portal)

               portal-tile-good?
               (recur portal-tile (get-rand-tile level+))

               target-tile-good?
               (recur (get-rand-tile level) target-tile)

               :else
               (recur (get-rand-tile level) (get-rand-tile level+)))))))
  ([system portal-tile target-tile p-type]
   (let [e-world (first (rj.e/all-e-with-c system :world))
         e-portal (br.e/create-entity)
         system (rj.u/update-in-world system e-world (rj.c/->3DPoint portal-tile)
                                      (fn [entities]
                                        (vec
                                          (conj
                                            (remove #(rj.cfg/<walls> (:type %)) entities)
                                            (rj.c/map->Entity {:id   e-portal
                                                               :type p-type})))))]
     {:system (rj.e/system<<components
                      system e-portal
                      [[:portal {:x (:x target-tile)
                                 :y (:y target-tile)
                                 :z (:z target-tile)}]
                       [:position {:x (:x portal-tile)
                                   :y (:y portal-tile)
                                   :z (:z portal-tile)
                                   :type p-type}]])
      :z (:z portal-tile)})))

(defn portal-target-pos [system portal]
  (let [target-pos portal]
    [(:z target-pos) (:x target-pos) (:y target-pos)]))

(defn is-portal? [entity]
  (or (= (:type entity) :portal)
      (= (:type entity) :m-portal)))

