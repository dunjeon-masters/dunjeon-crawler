(ns rouje-like.items
  (:require [brute.entity :as br.e]
            [rouje-like.utils :as rj.u]
            [rouje-like.components :as rj.c]
            [rouje-like.entity-wrapper :as rj.e]
            [rouje-like.world :as rj.wr]))

(defn item>>world
  [system is-valid-tile? item>>entities]
  (let [e-world (first (rj.e/all-e-with-c system :world))
        world (:world (rj.e/get-c-on-e system e-world :world))]
    (loop [system system]
      (let [x (rand-int (count world))
            y (rand-int (count (first world)))]
        (if (is-valid-tile? world [x y])
          (rj.wr/update-in-world system e-world [x y]
                                 (fn [entities]
                                   (item>>entities entities)))
          (recur system))))))

(defn only-floor?
  [tile]
  (every? #(#{:floor} (:type %))
          (:entities tile)))

(defn item>>entities
  [entities e-id e-type]
  (conj entities
        (rj.c/map->Entity {:id   e-id
                           :type e-type})))

(defn add-torch
  [system]
  (let [e-torch (br.e/create-entity)

        not-near-torches? (fn [world [x y]]
                            (rj.u/not-any-radially-of-type world [x y]
                                                           #(<= % 3) [:torch]))

        is-valid-tile? (fn [world [x y]]
                         (let [tile (get-in world [x y])]
                           (and (not-near-torches? world [x y])
                                (only-floor? tile))))

        torch>>entities (fn [entities]
                          (item>>entities entities e-torch :torch))]
    (item>>world system is-valid-tile?
                 torch>>entities)))

(defn add-gold
  [system]
  (let [e-gold (br.e/create-entity)

        is-valid-tile? (fn [world [x y]]
                         (only-floor? (get-in world [x y])))

        gold>>entities (fn [entities]
                          (item>>entities entities e-gold :gold))]
    (item>>world system is-valid-tile?
                 gold>>entities)))
