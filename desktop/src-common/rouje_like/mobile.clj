(ns rouje-like.mobile
  (:require [rouje-like.entity-wrapper :as rj.e]
            [rouje-like.utils :as rj.u]
            [rouje-like.components :as rj.c]
            [rouje-like.portal :as rj.p]))

(defn can-move?
  [_ _ target-tile _]
  (#{:floor :gold :torch :portal} (:type (rj.u/tile->top-entity target-tile))))

(defn add-entity [system e-world entity target-pos]
  "Add ENTITY to the tile at TARGET-POS."
  (let [c-position (rj.e/get-c-on-e system entity :position)
        this-type (:type c-position)]
  (rj.u/update-in-world system e-world target-pos
                        (fn [entities]
                          (vec
                           (conj entities
                                 (rj.c/map->Entity {:type this-type
                                                    :id   entity})))))))

(defn remove-entity [system e-world entity target-pos]
  "Remove ENTITY from the tile at TARGET-POS."
  (let [c-position (rj.e/get-c-on-e system entity :position)
        this-type (:type c-position)]
    (rj.u/update-in-world system e-world target-pos
                          (fn [entities]
                            (vec
                             (remove
                              #(#{this-type} (:type %))
                              entities))))))

(defn update-position [system entity target-tile]
  "Update the position of ENTITY to the position of TARGET-TILE."
  (rj.e/upd-c system entity :position
              (fn [c-position]
                (-> c-position
                    (assoc-in [:x] (:x target-tile))
                    (assoc-in [:y] (:y target-tile))
                    (assoc-in [:z] (:z target-tile))))))

(defn move-entity [system e-world entity to-pos from-pos tile]
  "Move ENTITY from FROM-POS to TO-POS at TILE."
  (-> system
      (add-entity e-world entity to-pos)
      (remove-entity e-world entity from-pos)
      (update-position entity tile)))

(defn port-entity [system e-world entity from-pos portal]
  "Teleport ENTITY from FROM-POS through PORTAL."
  (let [to-pos (rj.p/portal-target-pos system portal)
        c-world (rj.e/get-c-on-e system e-world :world)
        level (nth (:levels c-world) (nth to-pos 0))
        target-tile (get-in level [(nth to-pos 1) (nth to-pos 2)])]
    (move-entity system e-world entity to-pos from-pos target-tile)))

(defn move
  [_ e-this target-tile system]
  (let [c-position (rj.e/get-c-on-e system e-this :position)
        e-world (first (rj.e/all-e-with-c system :world))
        this-pos [(:z c-position) (:x c-position) (:y c-position)]
        target-pos [(:z target-tile) (:x target-tile) (:y target-tile)]
        this-type (:type c-position)
        portal (first (filter rj.p/is-portal? (:entities target-tile)))]
    (if (and (= this-type :player) portal)
      (port-entity system e-world e-this this-pos portal)
      (move-entity system e-world e-this target-pos this-pos target-tile))))
