(ns rouje-like.equipment
  (:require [rouje-like.utils :refer [?]]
            [rouje-like.config :as rj.cfg]
            [rouje-like.entity-wrapper :as rj.e]))

#_(in-ns 'rouje-like.equipment)
#_(use 'rouje-like.equipment :reload)

(def weapon-qualities [{:quality :quick  :stats {:atk  1}}
                       {:quality :giant  :stats {:atk  2}}
                       {:quality :great  :stats {:atk  2}}
                       {:quality :tiny   :stats {:atk  1}}
                       {:quality :dull   :stats {:atk -1}}
                       {:quality :dented :stats {:atk -2}}])

(def weapons [{:name :sword  :stats {:atk 3}}
              {:name :mace   :stats {:atk 2}}
              {:name :axe    :stats {:atk 3}}
              {:name :flail  :stats {:atk 2}}
              {:name :dagger :stats {:atk 1}}])

(def weapon-effects [{:effect :bloodletting}
                     {:effect :pain}
                     {:effect :poison}
                     {:effect :paralysis}
                     {:effect :power}
                     {:effect :death}
                     nil])

(def armors [{:name :breastplate :stats {:max-hp 3}}
             {:name :tunic       :stats {:max-hp 1}}])

(defn generate-random-weapon []
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

(defn generate-random-armor []
 (assoc (rand-nth armors)
        :type :armor))

(defn generate-random-equipment []
  "Generate a random piece of equipment or a random TYPE and returns a map
   of the type of equipment generated along with the equipment."
  (rand-nth [(generate-random-weapon) (generate-random-armor)]))

(defn equipment-name [eq]
  "Return a string containing the name of EQ."
  (if eq
    (str (name (:name eq))
         (if-let [effect (:effect eq)]
           (str " of " (name effect))))
    ""))

(defn update-stat [system e-this stat amount]
  "Update the statistic STAT by AMOUNT on E-THIS."
  (let [comp-to-update (rj.cfg/stat->comp stat)]
    (rj.e/upd-c system e-this comp-to-update
                (fn [c-comp]
                  (update-in c-comp [stat]
                             (partial + amount))))))

(defn update-stats [system e-this stats]
  "Loop over [stat val] pairs in STATS and update them on E-THIS."
  (if (not (empty? stats))
    (loop [system system
           e-this e-this
           stats stats]
      (let [[stat amount] (first stats)
            more? (rest stats)]
        (if (empty? more?)
          (update-stat system e-this stat amount)
          (recur (update-stat system e-this stat amount) e-this more?))))
    system))

(defn update-values [m f & args]
  (reduce (fn [r [k v]]
            (assoc r k (apply f v args)))
          {} m))

(defn switch-equipment [system e-this new-eq]
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

