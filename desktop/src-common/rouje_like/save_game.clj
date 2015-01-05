(ns rouje-like.save-game
  (:require [rouje-like.entity-wrapper :as rj.e]
            [rouje-like.messaging :as rj.msg]
            [rouje-like.utils :as rj.u :refer [?]]
            [rouje-like.components :refer [->save-state]]))

(defn save-game [system save-name]
  (let [e-player (first (rj.e/all-e-with-c system :player))
        saveable-components [:destructible :wallet
                             :equipment :inventory
                             :energy :experience
                             :class :race :player
                             :playersight :attacker]
        save (reduce (fn [save k-comp]
                       (let [c-comp (rj.e/get-c-on-e system e-player k-comp)]
                         (merge save
                                {k-comp (->save-state c-comp)})))
                     {} saveable-components)]
    (spit (str save-name ".save.edn") (pr-str save))
    system))

(defn load-game [system save-name]
  (if-let [saved-state (try (slurp (str save-name ".save.edn"))
                            (catch Exception e))]
    (let [save-state (read-string saved-state)

          e-player (first (rj.e/all-e-with-c system :player))]
      (reduce (fn [system k-saved-comp]
                (rj.e/upd-c system e-player k-saved-comp
                            (fn [c]
                              (let [saved-comp (k-saved-comp save-state)]
                                (merge c saved-comp)))))
              system (keys save-state)))
    (rj.msg/add-msg! system
      (str "No file called " save-name ".save.edn found"))))
