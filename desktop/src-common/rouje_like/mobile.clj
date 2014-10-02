(ns rouje-like.mobile
  (:require [rouje-like.entity-wrapper :as rj.e]
            [rouje-like.utils :as rj.u]
            [rouje-like.world :as rj.wr]
            [rouje-like.components :as rj.c]))

(defn can-move?
  [_ _ target-tile _]
  ;;TODO: Refactor to put is-valid-move-target? in mobile comp
  (#{:floor :gold :torch} (:type (rj.u/tile->top-entity target-tile))))

(defn move
  [_ e-this target-tile system]
  (let [c-position (rj.e/get-c-on-e system e-this :position)
        e-world (first (rj.e/all-e-with-c system :world))
        this-pos [(:x c-position) (:y c-position)]
        target-pos [(:x target-tile) (:y target-tile)]
        this-type (:type c-position)]
    (-> system
        (rj.wr/update-in-world e-world target-pos
                               (fn [entities]
                                 (vec
                                   (conj entities
                                         (rj.c/map->Entity {:type this-type
                                                            :id   e-this})))))

        (rj.wr/update-in-world e-world this-pos
                               (fn [entities]
                                 (vec
                                   (remove
                                     #(#{this-type} (:type %))
                                     entities))))

        (rj.e/upd-c e-this :position
                    (fn [c-position]
                      (-> c-position
                          (assoc-in [:x] (:x target-tile))
                          (assoc-in [:y] (:y target-tile))))))))