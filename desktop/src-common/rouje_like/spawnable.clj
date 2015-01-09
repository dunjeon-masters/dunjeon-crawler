(ns rouje-like.spawnable
  (:require [rouje-like.utils :as rj.u]
            [rouje-like.utils :refer [?]]
            [brute.entity :as br.e]
            [rouje-like.entity-wrapper :as rj.e]
            [rouje-like.config :as rj.cfg]
            [rouje-like.components :as rj.c
             :refer [->3DPoint]]
            [rouje-like.status-effects :as rj.stef]))

(defn get-tile
  [system z]
  (let [e-world (first (rj.e/all-e-with-c system :world))
        {:keys [levels]} (rj.e/get-c-on-e system e-world :world)
        world (nth levels z)
        get-rand-tile (fn [world]
                        (get-in world [(rand-int (count world))
                                       (rand-int (count (first world)))]))]
    (loop [target-tile (get-rand-tile world)]
      (if (rj.cfg/<floors> (:type (rj.u/tile->top-entity target-tile)))
        target-tile
        (recur (get-rand-tile world))))))

(defn new-entity []
  (br.e/create-entity))

(defn put-in-world [type-e tile entity z system]
  (let [e-world (first (rj.e/all-e-with-c system :world))]
    (rj.u/update-in-world
      system e-world
      (->3DPoint tile)
      (fn [entities]
        (vec
          (conj
            (remove #(#{:wall} (:type %)) entities)
            (rj.c/map->Entity {:id   entity
                               :type type-e})))))))

(defn give-position
  [e-this tile system]
  (let [{:keys [x y z]} (rj.e/get-c-on-e system e-this :position)]
    (cond-> system
      (nil? x) (rj.e/upd-c e-this :position
                    #(assoc % :x (:x tile)))
      (nil? y) (rj.e/upd-c e-this :position
                    #(assoc % :y (:y tile)))
      (nil? z) (rj.e/upd-c e-this :position
                    #(assoc % :z (:z tile))))))

(defn give-stef-e-from
  [e-this system]
  (rj.e/upd-c system e-this :destructible
              (fn [c-destructible]
                (update-in c-destructible [:status-effects]
                           (fn [status-effects]
                             (->> status-effects
                                  (map #(assoc %
                                          :e-from e-this))
                                  vec))))))

(defn give-stef-apply-fn
  [e-this system]
  (rj.e/upd-c system e-this :attacker
              (fn [c-attacker]
                (update-in c-attacker [:status-effects]
                           (fn [status-effects]
                             (->> status-effects
                                  (map #(assoc %
                                               :apply-fn (rj.stef/effect-type->apply-fn (:type %))))
                                  vec))))))

(defmacro defentity
  [name components]
  (let [fn-name (symbol (str "add-" name))]
    `(defn ~fn-name
       ([{:keys [~'system ~'z]}]
        (let [~'tile (rouje-like.spawnable/get-tile ~'system ~'z)]
          (~fn-name ~'system ~'tile)))
       ([system# tile#]
        (let [e-this#  (rouje-like.spawnable/new-entity)
              type-e# ~(keyword name)
              z#       (:z tile#)]
          (->>
            (rj.e/system<<components
              system# e-this#
              ~components)
            (rouje-like.spawnable/put-in-world type-e# tile# e-this# z#)
            (rouje-like.spawnable/give-position e-this# tile#)
            (rouje-like.spawnable/give-stef-e-from e-this#)
            (rouje-like.spawnable/give-stef-apply-fn e-this#)
            (assoc {} :z z# :system)))))))
