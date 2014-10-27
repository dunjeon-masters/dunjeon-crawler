(ns rouje-like.config)

;; WORLD CONFIG
(def block-size 36)
(def padding-sizes {:top   1
                    :btm   2
                    :left  1
                    :right 1})
(def view-port-sizes [20 20])
(def world-sizes {:width  60
                  :height 60})

;; PLAYER CONFIG
(def player-stats
  {:hp  100
   :atk 4
   :def 1})

(def class->stats {:rogue   {}
                   :warrior {}
                   :mage    {}})

(def race->stats {:human {:hp 10  :atk 1}
                  :orc   {:hp 20  :atk 2}
                  :elf   {:hp -5  :atk 0}})

;; CREATURE CONFIG
(def bat-stats
  {:hp  2
   :def 0})

(def lichen-stats
  {:hp  4
   :atk 1
   :def 1})

(def skeleton-stats
  {:hp  8
   :def 1
   :atk 3})

