(ns rouje-like.status-effects
  (:require [rouje-like.components
             :refer [->3DPoint]]
            [rouje-like.entity-wrapper :as rj.e]
            [rouje-like.messaging :as rj.msg]
            [rouje-like.utils :as rj.u :refer [?]]))

(defn update-in-status-effects
  [system e-this -fn-]
  (rj.e/upd-c system e-this :destructible
              (fn [c-destructible]
                (update-in c-destructible [:status-effects]
                           (fn [status-effects]
                             (-fn- status-effects))))))

(defn apply-paralysis
  [system e-this status]
  (as-> system system
    (rj.e/upd-c system e-this :energy
                (fn [c-energy]
                  (update-in c-energy [:energy]
                             - (rand-nth [0 (inc (:value status))]))))
    (do (? (rj.e/get-c-on-e system e-this :energy))
        system)
    (if-let [c-broadcaster (rj.e/get-c-on-e system e-this :broadcaster)]
      (rj.msg/add-msg system :static
                      (format "%s was paralyzed"
                              ((:name-fn c-broadcaster) system e-this)))
      system)))

(defn apply-burn
  [system e-this {:keys [e-from value]}]
  (let [e-world (first (rj.e/all-e-with-c system :world))
        c-position (rj.e/get-c-on-e system e-this :position)
        hp (:hp (rj.e/get-c-on-e system e-this :destructible))]
    (if (pos? (- hp value))
      (as-> system system
        (rj.e/upd-c system e-this :destructible
                    (fn [c-destructible]
                      (update-in c-destructible [:hp]
                                 - value)))

        (if-let [c-broadcaster (rj.e/get-c-on-e system e-this :broadcaster)]
          (rj.msg/add-msg system :static
                          (format "%s was dealt %s burn damage"
                                  ((:name-fn c-broadcaster) system e-this) value))
          system))

      (as-> system system
        (if-let [c-broadcaster (rj.e/get-c-on-e system e-this :broadcaster)]
          (rj.msg/add-msg system :static
                          (format "%s burned %s to death"
                                  (let [atker-c-broadcaster (rj.e/get-c-on-e system e-from :broadcaster)]
                                    ((:name-fn atker-c-broadcaster) system e-from))
                                  ((:name-fn c-broadcaster) system e-this)))
          system)

        (rj.u/update-in-world system e-world
                              (->3DPoint c-position)
                              (fn [entities]
                                (vec
                                  (remove
                                    #(#{e-this} (:id %))
                                    entities))))

        (if-let [c-killable (rj.e/get-c-on-e system e-this :killable)]
          (let [c-exp (rj.e/get-c-on-e system e-from :experience)
                level-up-fn (:level-up-fn c-exp)]

            (->> (rj.e/upd-c system e-from :experience
                             (fn [c-experience]
                               (update-in c-experience [:experience]
                                          #(+ % (:experience c-killable)))))
                 (level-up-fn e-from)))
          system)
        (rj.e/kill-e system e-this)))))

(defn apply-poison
  [system e-this {:keys [e-from value]}]
  (let [e-world (first (rj.e/all-e-with-c system :world))
        c-position (rj.e/get-c-on-e system e-this :position)
        hp (:hp (rj.e/get-c-on-e system e-this :destructible))]
    (if (pos? (- hp value))
      (as-> system system
        (rj.e/upd-c system e-this :destructible
                    (fn [c-destructible]
                      (update-in c-destructible [:hp]
                                 - value)))

        (if-let [c-broadcaster (rj.e/get-c-on-e system e-this :broadcaster)]
          (rj.msg/add-msg system :static
                          (format "%s was dealt %s poison damage"
                                  ((:name-fn c-broadcaster) system e-this) value))
          system))

      (as-> system system
        (if-let [c-broadcaster (rj.e/get-c-on-e system e-this :broadcaster)]
          (rj.msg/add-msg system :static
                          (format "%s killed %s with poison"
                                  (let [atker-c-broadcaster (rj.e/get-c-on-e system e-from :broadcaster)]
                                    ((:name-fn atker-c-broadcaster) system e-from))
                                  ((:name-fn c-broadcaster) system e-this)))
          system)

        (rj.u/update-in-world system e-world
                              (->3DPoint c-position)
                              (fn [entities]
                                (vec
                                  (remove
                                    #(#{e-this} (:id %))
                                    entities))))

        (if-let [c-killable (rj.e/get-c-on-e system e-this :killable)]
          (let [c-exp (rj.e/get-c-on-e system e-from :experience)
                level-up-fn (:level-up-fn c-exp)]

            (->> (rj.e/upd-c system e-from :experience
                             (fn [c-experience]
                               (update-in c-experience [:experience]
                                          #(+ % (:experience c-killable)))))
                 (level-up-fn e-from)))
          system)
        (rj.e/kill-e system e-this)))))

(def effect-type->apply-fn
  {:fire     apply-burn
   :poison   apply-poison
   :paralyis apply-paralysis})
