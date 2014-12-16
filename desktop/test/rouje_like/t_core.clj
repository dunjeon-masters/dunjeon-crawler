(ns rouje-like.t-core
  (:use [midje.sweet]
        [rouje-like.test-utils])
  (:require [rouje-like.core :as rj.core]))

(fact "about `core`"
      (rj.core/cmds->action "name pancia")
      => (just {:n "pancia"}))
