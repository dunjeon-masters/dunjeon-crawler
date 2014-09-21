(ns rouje-like.components
  (:import [com.badlogic.gdx.graphics.g2d TextureRegion]
           [clojure.lang Atom Fn Keyword PersistentVector]))

(def block-size 27)

(defrecord World [^Atom world])
(defrecord Tile [^Number x ^Number y
                 ^Number screen-x ^Number screen-y
                 ^PersistentVector entities])
(defrecord Entity [^Keyword Type])

(defrecord Player [^Atom show-world?])
(defrecord Digger [^Fn can-dig? ^Fn dig!])
(defrecord MovesLeft [^Atom moves-left])
(defrecord Gold [^Atom gold])
(defrecord Sight [^Atom distance ^Atom decline-rate
                  ^Atom lower-bound ^Atom upper-bound])

(defrecord Position [^Atom world ^Atom x ^Atom y])
(defrecord Mobile [^Fn can-move? ^Fn move!])

;; FOR EVERYTHING:
(defrecord Renderable [^Number pri ^Fn render-fn args]) #_(args-type=map)

;; Workaround for not being able to get record's type "statically"
(def get-type {:world      (type (->World nil))
               :tile       (type (->Tile nil nil nil nil nil))
               :entity     (type (->Entity nil))
               :player     (type (->Player nil))
               :digger     (type (->Digger nil nil))
               :position   (type (->Position nil nil nil))
               :mobile     (type (->Mobile nil nil))
               :moves-left (type (->MovesLeft nil))
               :gold       (type (->Gold nil))
               :sight      (type (->Sight nil nil nil nil))
               :renderable (type (->Renderable nil nil nil))})

