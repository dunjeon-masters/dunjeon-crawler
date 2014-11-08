(ns rouje-like.weapons
  (:require [rouje-like.config :as rj.cfg]
            [rouje-like.entity-wrapper :as rj.e]))

(def weapon-qualities [{:name :quick :stats {:atk 1}}
                        {:name :giant :stats {:atk 2}}
                        {:name :great :stats {:atk 2}}
                        {:name :tiny :stats {:atk 1}}
                        {:name :dull :stats {:atk -1}}
                        {:name :dented :stats {:atk -2}}])

(def weapons [{:name :sword :stats {:atk 1}}
              {:name :mace :stats {:atk 1}}
              {:name :axe :stats {:atk 1}}
              {:name :flail :stats {:atk 1}}
              {:name :dagger :stats {:atk 1}}])

(def weapon-effects [{:name :bloodletting}
                     {:name :pain}
                     {:name :poison}
                     {:name :paralysis}
                     {:name :power}
                     {:name :death}
                     nil])

(defn generate-random-weapon []
  "Generate a random weapon consisting of a weapon quality, a weapon type,
   and a weapon effect."
  (let [quality (rand-nth weapon-qualities)
        wpn (rand-nth weapons)
        effect (rand-nth weapon-effects)]
    (if effect
      [quality wpn {:name :of} effect] ; lame fix
      [quality wpn])))

(defn weapon-stats [weapon]
  "Return a map of the stats of WEAPON or NIL."
  (and weapon
       (let [quality (nth weapon 0)
             wpn (nth weapon 1)]
         {:atk (+ (or (:atk (:stats quality)) 0)
                  (:atk (:stats wpn)))})))

(defn weapon-name [weapon]
  "Return a string containing the name of WEAPON."
  (and weapon (reduce (fn [sym1 sym2]
                        (str (name sym1) " " (name sym2)))
                      (map :name weapon))))

(defn update-stat [system e-this stat amount]
  "Update the statistic STAT by AMOUNT on E-THIS."
  (let [comp-to-update (rj.cfg/stat->comp stat)]
    (rj.e/upd-c system e-this comp-to-update
                (fn [c-comp]
                  (update-in c-comp [stat] (partial + amount))))))

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

(defn negate-stats [stats]
  "Return a map of STATS w/ the stat values inverted."
  (if (not (empty? stats))
    (reduce (fn [r [stat val]] (assoc r stat (apply - val '()))) {} stats)
    nil))

(defn switch-weapon [system e-this new-wpn]
  "Switch the current weapon of E-THIS with NEW-WPN and update
   E-THIS's stats to reflect the change."
  (let [current-wpn (:weapon (rj.e/get-c-on-e system e-this :weapon))
        old-stats (weapon-stats current-wpn)
        new-stats (weapon-stats new-wpn)]
    (-> system
          ;; iterate over old-stats and pass them to update-stat for removal
          (update-stats e-this (negate-stats old-stats))
          ;; switch weapons
          (rj.e/upd-c e-this :weapon
                      (fn [c-weapon]
                        (assoc-in c-weapon [:weapon] new-wpn)))
          ;; iterate over new-stats and pass them to update-stat
          (update-stats e-this new-stats))))
