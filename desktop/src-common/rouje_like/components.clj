(ns rouje-like.components
  (:import [com.badlogic.gdx.graphics.g2d TextureRegion]))

;; FOR ENTITY E-BOARD:
(defrecord World [tiles]) ;; 2D Vector of Tile's
(defrecord Tile [x y type])

;; FOR ENTITY E-PLAYER:
(defrecord Player [tiles]) ;; 2D Vector of Tile's
(defrecord Position [x y])
(defrecord MovesLeft [moves-left])
(defrecord Score [score])

;; FOR EVERYTHING ELSE:
(defrecord Renderable [fn])

;; Workaround for not being able to get record's type "statically"
(def get-type {:world      (type (->World nil))
               :tile       (type (->Tile nil nil nil))
               :player     (type (->Player nil))
               :position   (type (->Position nil nil))
               :moves-left (type (->MovesLeft nil))
               :score      (type (->Score nil))
               :renderable (type (->Renderable nil))})
