(ns rouje-like.t-core
  (:use midje.sweet)
  (:require [rouje-like.core :as rj.core]))

(fact "about `core`"
  (rj.core/cmds->action "name pancia") => {:n "pancia"})
