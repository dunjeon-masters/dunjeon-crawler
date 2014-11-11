(ns rouje-like.equipment
  (:require [rouje-like.utils :refer [?]]
            [rouje-like.config :as rj.cfg]
            [rouje-like.entity-wrapper :as rj.e]))

#_(in-ns 'rouje-like.equipment)
#_(use 'rouje-like.equipment :reload)

(def weapon-qualities
  (map #(zipmap [:quality :stats] %)
       rj.cfg/weapon-qualities))

(def weapons
  (map #(zipmap [:name :stats] %)
       rj.cfg/weapons))

(def weapon-effects
  (map #(zipmap [:effect] %)
       rj.cfg/weapon-effects))

(def armors
  (map #(zipmap [:name :stats] %)
       rj.cfg/armors))

(defn generate-random-weapon
  []
  "Generate a random weapon consisting of a weapon quality, a weapon type,
   and a weapon effect."
  (let [quality (rand-nth weapon-qualities)
        weapon  (rand-nth weapons)
        effect  (rand-nth weapon-effects)]
    (-> (if effect
          (merge-with (partial merge-with +)
                      quality weapon effect)
          (merge-with (partial merge-with +)
                      quality weapon))
        (assoc :type :weapon))))

(defn generate-random-armor
  []
 (assoc (rand-nth armors)
        :type :armor))

(defn generate-random-equipment
  []
  "Generate a random piece of equipment or a random TYPE and returns a map
   of the type of equipment generated along with the equipment."
  (rand-nth [(generate-random-weapon) (generate-random-armor)]))

(defn equipment-name
  [eq]
  "Return a string containing the name of EQ."
  (if eq
    (str (name (:name eq))
         (if-let [effect (:effect eq)]
           (str " of " (name effect))))
    ""))

(defn update-stat
  [system e-this stat amount]
  "Update the statistic STAT by AMOUNT on E-THIS."
  (let [comp-to-update (rj.cfg/stat->comp stat)]
    (rj.e/upd-c system e-this comp-to-update
                (fn [c-comp]
                  (update-in c-comp [stat]
                             (partial + amount))))))

(defn update-stats
  [system e-this stats]
  "Loop over [stat val] pairs in STATS and update them on E-THIS."
  (if (not (empty? stats))
    (reduce (fn [system [stat amount]]
              (update-stat system e-this stat amount))
            system stats)
    system))

(defn update-values
  [m f & args]
  "Apply to each value v in m, (f v args)"
  (reduce (fn [r [k v]]
            (assoc r k (apply f v args)))
          {} m))

(defn switch-equipment
  [system e-this new-eq]
  "Switch the current equipment slot on E-THIS with C-NEW-EQ and update
   E-THIS's stats to reflect the change."
  (let [eq-type (:type new-eq)

        c-old-eq (rj.e/get-c-on-e system e-this :equipment)
        old-eq (eq-type c-old-eq)

        old-stats (:stats old-eq)
        new-stats (:stats new-eq)]
    (-> system
          ;; iterate over old-stats and pass them to update-stat for removal
          (update-stats e-this (update-values old-stats -))
          ;; switch equipment
          (rj.e/upd-c e-this :equipment
                      (fn [c-eq]
                        (assoc-in c-eq [eq-type]
                                  new-eq)))
          ;; iterate over new-stats and pass them to update-stat
          (update-stats e-this new-stats))))

