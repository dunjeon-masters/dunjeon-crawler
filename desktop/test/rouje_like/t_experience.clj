(ns rouje-like.t-experience
  (:use [midje.sweet]
        [rouje-like.experience])
  (:require [rouje-like.core :as rj.core]
            [brute.entity :as br.e]
            [rouje-like.entity-wrapper :as rj.e]
            [rouje-like.utils :refer :all]
            [rouje-like.components :as rj.c :refer [->3DPoint]]
            [rouje-like.world :as rj.w]
            [rouje-like.config :as rj.cfg]))

(defn get-system []
  (with-open [w (clojure.java.io/writer "NUL")]
    (binding [*out* w]
      (-> (br.e/create-system)
          (rj.core/init-entities {})))))

(let [system (get-system)
      system (#'rouje-like.world/init-themed-entities system 1 :maze)
      e-player (first (rj.e/all-e-with-c system :player))
      c-attacker (rj.e/get-c-on-e system e-player :attacker)
      e-slime (first (rj.e/all-e-with-c system :slime))
      e-skeleton (first (rj.e/all-e-with-c system :skeleton))
      c-attacker-skeleton (rj.e/get-c-on-e system e-skeleton :attacker)]

  (fact "level->exp"
        (level->exp 5) => (* 5 (:exp rj.cfg/level-exp))
        (level->exp 7) => (* 7 (:exp rj.cfg/level-exp)))

  (fact "wrand"
        (wrand [2 2 1]) =future=> "ANTHONY WILL DO THIS")

  (fact "level-up-stats"
        (level-up-stats system e-player) =future=> true)

  (fact "level-up"
        (level-up e-player system) =future=> true))
