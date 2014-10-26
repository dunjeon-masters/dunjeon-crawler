(ns rouje-like.config)

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

(def bat-stats
  {:hp  4
   :def 0})

(def lichen-stats
  {:hp  4
   :atk 1
   :def 0})

(def skeleton-stats
  {:hp  10
   :def 0
   :atk 4})

