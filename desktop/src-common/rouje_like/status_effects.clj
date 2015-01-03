(ns rouje-like.status-effects
  (:require [rouje-like.components
             :refer [->3DPoint]]
            [rouje-like.entity-wrapper :as rj.e]
            [rouje-like.messaging :as rj.msg]
            [rouje-like.utils :as rj.u :refer [? as-?>]]))

(defn update-in-status-effects
  [system e-this -fn-]
  (rj.e/upd-c system e-this :destructible
              (fn [c-destructible]
                (update-in c-destructible [:status-effects]
                           (fn [status-effects]
                             (-fn- status-effects))))))

(defn apply-paralysis
  [system e-this status]
  (as-?> system
    (rj.e/upd-c system e-this :energy
                (fn [c-energy]
                  (update-in c-energy [:energy]
                             - (rand-nth [0 (inc (:value status))]))))

    (if-let [c-broadcaster (rj.e/get-c-on-e system e-this :broadcaster)]
      (rj.msg/add-msg system :static
                      (format "%s was paralyzed"
                              ((:name-fn c-broadcaster) system e-this))))))

(defn- deal-damage
  [system e-to e-from value effect-type]
  (let [e-world (first (rj.e/all-e-with-c system :world))
        c-position (rj.e/get-c-on-e system e-to :position)
        c-destructible (rj.e/get-c-on-e system e-to :destructible)
        hp (:hp c-destructible)]
    (if (pos? (- hp value))
      ;;e-to is still alive
      (as-?> system
        ;;deal value damage to e-to
        (rj.e/upd-c system e-to :destructible
                    (fn [c-destructible]
                      (update-in c-destructible [:hp]
                                 - value)))

        ;;notify that it took damage (of effect-type)
        (if-let [c-broadcaster (rj.e/get-c-on-e system e-to :broadcaster)]
          (rj.msg/add-msg system :static
                          (format (str "%s was dealt %s " effect-type " damage")
                                  ((:name-fn c-broadcaster) system e-to) value))))

      ;;e-to died
      (as-?> system
        ;;call the on-death-fn, eg: amoeba splitting
        (if-let [on-death (:on-death-fn c-destructible)]
          (on-death c-destructible e-to system))

        ;;exp/lvl up e-from
        (if-let [c-killable (rj.e/get-c-on-e system e-to :killable)]
          (let [{:keys [level-up-fn]} (rj.e/get-c-on-e system e-from :experience)]

            (->> (rj.e/upd-c system e-from :experience
                             (fn [c-experience]
                               (update-in c-experience [:experience]
                                          #(+ % (:experience c-killable)))))
                 (level-up-fn e-from))))

        ;;remove it from the world
        (rj.u/update-in-world system e-world
                              (->3DPoint c-position)
                              (fn [entities]
                                (vec
                                  (remove
                                    #(#{e-to} (:id %))
                                    entities))))

        ;;broadcast that it died (by effect-type)
        (if-let [c-broadcaster (rj.e/get-c-on-e system e-to :broadcaster)]
          (rj.msg/add-msg system :static
                          (format (str "%s " effect-type "ed %s to death")
                                  (let [atker-c-broadcaster (rj.e/get-c-on-e system e-from :broadcaster)]
                                    ((:name-fn atker-c-broadcaster) system e-from))
                                  ((:name-fn c-broadcaster) system e-to))))

        ;;remove e-to from system
        (rj.e/kill-e system e-to)))))

(defn apply-burn
  [system e-this {:keys [e-from value]}]
  (deal-damage system e-this e-from value "burn"))

(defn apply-poison
  [system e-this {:keys [e-from value]}]
  (deal-damage system e-this e-from value "poison"))

(def effect-type->apply-fn
  {:fire     apply-burn
   :poison   apply-poison
   :paralyis apply-paralysis})
