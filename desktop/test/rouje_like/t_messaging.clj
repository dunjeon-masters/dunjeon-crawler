(ns rouje-like.t-messaging
  (:use [midje.sweet]
        [rouje-like.test-utils]
        [rouje-like.messaging])
  (:require [brute.entity :as br.e]
            [rouje-like.entity-wrapper :as rj.e]))

;; ========= ADD-MSG =========
(let [system (start)
      e-relay (first (rj.e/all-e-with-c system :relay))]
  (fact "add-msg :static"
        (-> (add-msg system :static "static")
            (rj.e/get-c-on-e e-relay :relay)
            (:static)) => (just {:message "static"
                                 :turn 1}))
  (fact "add-msg :dynamic"
        (-> system
            (rj.e/upd-c e-relay :renderable
                        (fn [c-renderable]
                          (assoc c-renderable :render-fn
                                 (constantly true))))
            (add-msg :blocking "blocking")
            (rj.e/get-c-on-e e-relay :relay)
            (:blocking)) => truthy))

;; ========= PROCESS-INPUT-TICK =========
(let [system (start)
      e-relay (first (rj.e/all-e-with-c system :relay))
      system (rj.e/upd-c system e-relay :renderable
                         (fn [c-renderable]
                           (assoc c-renderable :render-fn
                                  (constantly true))))
      system (add-msg system :blocking "blocking")
      system (add-msg system :static "static")

      e-counter (first (rj.e/all-e-with-c system :counter))
      c-counter-tickable (rj.e/get-c-on-e system e-counter :tickable)

      system ((:tick-fn c-counter-tickable) nil e-counter system)
      system ((:tick-fn c-counter-tickable) nil e-counter system)
      system (process-input-tick nil e-relay system)]
  (fact "after process-input-tick, blocking is empty"
        (:blocking
          (rj.e/get-c-on-e system e-relay :relay)) => [])
  (fact "after process-input-tick,
        static doesn't have messages whose turn has passed"
        (rj.e/get-c-on-e system e-relay :relay)
        => (just {:blocking empty?
                  :static empty?})))

;; ========= INIT-RELAY =========
(let [system (start)]
  (fact "init-relay"
        (seq (rj.e/all-e-with-c system :relay))   => truthy
        (seq (rj.e/all-e-with-c system :counter)) => truthy
        (let [e-counter (first (rj.e/all-e-with-c system :counter))
              c-counter (rj.e/get-c-on-e system e-counter :counter)]
          (:turn c-counter)) => 1))
