(ns rouje-like.weapons)

(def weapon-qualities [{:name :quick :stats {:atk 1}}
                        {:name :giant :stats {:atk 2}}
                        {:name :great :stats {:atk 2}}
                        {:name :tiny :stats {:atk 1}}
                        {:name :dull :stats {:atk -1}}
                        {:name :dented :stats {:atk -2}}
                        nil])

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
    [quality wpn effect]))

(defn weapon-stats [weapon]
  "Return a map of the stats of WEAPON or NIL."
  (and weapon
       (let [quality (nth weapon 0)
             wpn (nth weapon 1)]
         {:atk (+ (or (:atk (:stats adj)) 0)
                  (:atk (:stats wpn)))})))

(defn weapon-name [weapon]
  "Return a vector containing the name of WEAPON."
  (and weapon (map :name weapon)))
      

