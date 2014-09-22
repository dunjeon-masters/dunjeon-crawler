(ns rouje-like.components
  (:import [com.badlogic.gdx.graphics.g2d TextureRegion]
           [clojure.lang Atom Fn Keyword PersistentVector]))

(def block-size 27)


(defrecord World [world])

(defrecord Tile [^Number x ^Number y
                 ^PersistentVector entities])

(defrecord Entity [^Keyword id
                   ^Keyword type])

(defrecord Player [show-world?])

(defrecord Digger [^Fn can-dig?
                   ^Fn dig!])

(defrecord MovesLeft [moves-left])

(defrecord Gold [gold])

(defrecord Sight [distance
                  decline-rate
                  lower-bound
                  upper-bound])

(defrecord Position [x y])

(defrecord Mobile [^Fn can-move?
                   ^Fn move!])

(defrecord Attacker [attack
                     ^Fn attack!
                     ^Fn can-attack?])

(defrecord Destructible [hp
                         defense
                         ^Fn take-damage!])

(defrecord Tickable [^Fn tick-fn
                     args])

(defrecord Lichen [grow-chance%])

(defrecord Renderable [^Number pri
                       ^Fn render-fn
                       args #_(args-type=map)])

;; Workaround for not being able to get record's type "statically"
(def get-type {:world        (type (->World nil))
               :tile         (type (->Tile nil nil nil))
               :entity       (type (->Entity nil nil))
               :player       (type (->Player nil))
               :digger       (type (->Digger nil nil))
               :position     (type (->Position nil nil))
               :mobile       (type (->Mobile nil nil))
               :moves-left   (type (->MovesLeft nil))
               :gold         (type (->Gold nil))
               :sight        (type (->Sight nil nil nil nil))
               :renderable   (type (->Renderable nil nil nil))
               :attacker     (type (->Attacker nil nil nil))
               :destructible (type (->Destructible nil nil nil))
               :tickable     (type (->Tickable nil nil))
               :lichen       (type (->Lichen nil))})

(def get-pri {:floor 1
              :torch 2
              :gold 3
              :lichen 3
              :wall 4
              :player 10
              :else 1})

(defn sort-by-pri
  ([coll]
   (sort-by-pri get-pri coll))

  ([get-pri coll]
   (sort (fn [arg1 arg2]
           (let [t1 (:type arg1)
                 t2 (:type arg2)]
             (if (= t1 t2)
               0
               (- (get get-pri t2
                       (get get-pri :else 1))
                  (get get-pri t1
                       (get get-pri :else 1))))))
         coll)))
