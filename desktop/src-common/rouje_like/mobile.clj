(ns rouje-like.mobile
  (:require [rouje-like.entity-wrapper :as rj.e]
            [rouje-like.utils :as rj.u]
            [rouje-like.components :as rj.c
             :refer [->3DPoint]]
            [rouje-like.portal :as rj.p]
            [rouje-like.config :as rj.cfg]
            [clojure.set :refer [union]]))

#_(use 'rouje-like.mobile :reload)

(defn -can-move?
  [_ _ target-tile _]
  (rj.cfg/<valid-move-targets> (:type (rj.u/tile->top-entity target-tile))))

(defn can-move?
  [c e target-tile s]
  (and (> 80 (rand-int 100))
       (-can-move? c e target-tile s)))

(defn add-entity
  [system e-world entity target-pos]
  "Add ENTITY to the tile at TARGET-POS."
  (let [{:keys [type]} (rj.e/get-c-on-e system entity :position)]
    (rj.u/update-in-world system e-world target-pos
                          (fn [entities]
                            (vec
                              (conj entities
                                    (rj.c/map->Entity {:type type
                                                       :id   entity})))))))

(defn remove-entity
  [system e-world entity target-pos]
  "Remove ENTITY from the tile at TARGET-POS."
  (let [{:keys [type]} (rj.e/get-c-on-e system entity :position)]
    (rj.u/update-in-world system e-world target-pos
                          (fn [entities]
                            (vec
                              (remove
                                #(#{type} (:type %))
                                entities))))))

(defn update-position
  [system entity {:keys [x y z]}]
  "Update the position of ENTITY to the position of TARGET-TILE."
  (rj.e/upd-c system entity :position
              #(merge % {:x x, :y y, :z z})))

(defn move-entity
  [system e-world entity to-pos from-pos tile]
  "Move ENTITY from FROM-POS to TO-POS at TILE."
  (-> system
      (add-entity e-world entity to-pos)
      (remove-entity e-world entity from-pos)
      (update-position entity tile)))

(defn port-entity
  [system e-world entity from-pos e-portal portal-pos p-type]
  "Teleport ENTITY from FROM-POS through PORTAL."
  (let [portal (rj.e/get-c-on-e system e-portal :portal)
        [z x y] (rj.p/portal-target-pos system portal)
        c-world (rj.e/get-c-on-e system e-world :world)
        level (nth (:levels c-world) z)
        target-tile (get-in level [x y])]
    (if (= p-type :m-portal)
      (as-> system system
        (remove-entity system e-world e-portal portal-pos)
        (move-entity system e-world entity [z x y] from-pos target-tile)
        ((:merchant-level-fn c-world) system from-pos))
      (as-> system system
        (move-entity system e-world entity [z x y] from-pos target-tile)
        ((:add-level-fn c-world) system (inc z))))))

(defn move
  [_ e-this target-tile system]
  (let [{:keys [type] :as c-position} (rj.e/get-c-on-e system e-this :position)
        e-world (first (rj.e/all-e-with-c system :world))
        this-pos (->3DPoint c-position)
        target-pos (->3DPoint target-tile)

        portal (first (filter rj.p/is-portal? (:entities target-tile)))
        e-portal (:id portal)]
    (if (and (= type :player) portal)
      (port-entity system e-world e-this this-pos e-portal target-pos (:type portal))
      (move-entity system e-world e-this target-pos this-pos target-tile))))
