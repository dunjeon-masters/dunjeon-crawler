(ns rouje-like.items
  (:require [brute.entity :as br.e]
            [rouje-like.utils :as rj.u]
            [rouje-like.components :as rj.c]
            [rouje-like.entity-wrapper :as rj.e]
            [rouje-like.world :as rj.wr]))

(defn pickup-item
  [system e-by e-this this-pos item-type]
  (let [remove-item (fn [system this-pos]
                      (let [e-world (first (rj.e/all-e-with-c system :world))]
                        (rj.wr/update-in-world system e-world this-pos
                                               (fn [entities]
                                                 (vec
                                                   (remove
                                                     #(#{item-type} (:type %))
                                                     entities))))))

        c-broadcaster (rj.e/get-c-on-e system e-this :broadcaster)
        e-relay (first (rj.e/all-e-with-c system :relay))
        broadcast-pickup (fn [system]
                           (if (not (nil? c-broadcaster))
                             (rj.e/upd-c system e-relay :relay
                                         (fn [c-relay]
                                           (update-in c-relay [:static]
                                                      conj {:message (format "%s picked up %s"
                                                                             (let [by-c-broadcaster (rj.e/get-c-on-e system e-by :broadcaster)]
                                                                               ((:msg-fn by-c-broadcaster) system e-by))
                                                                             ((:msg-fn c-broadcaster) system e-this))
                                                            :turn (let [e-counter (first (rj.e/all-e-with-c system :counter))
                                                                        c-counter (rj.e/get-c-on-e system e-counter :counter)]
                                                                    (:turn c-counter))})))
                             system))]
    (case item-type
      :torch (let [c-playersight (rj.e/get-c-on-e system e-by :playersight)
                   sight-upper-bound (:upper-bound c-playersight)
                   sight-torch-multiplier (:torch-multiplier c-playersight)

                   c-torch (rj.e/get-c-on-e system e-this :torch)
                   torch-brightness (:brightness c-torch)

                   inc-sight (fn [prev] (if (<= prev (- sight-upper-bound torch-brightness))
                                          (+ prev (* torch-brightness sight-torch-multiplier))
                                          prev))]
               (-> system
                   (rj.e/upd-c e-by :playersight
                               (fn [c-playersight]
                                 (update-in c-playersight [:distance] inc-sight)))
                   (remove-item this-pos)
                   (broadcast-pickup)))

      :gold (-> system
                (rj.e/upd-c e-by :wallet
                            (fn [c-wallet]
                              (update-in c-wallet [:gold]
                                         (partial + (:value (rj.e/get-c-on-e system e-this :gold))))))
                (remove-item this-pos)
                (broadcast-pickup))

      system)))



(defn ^:private item>>world
  [system is-valid-tile? item>>entities]
  (let [e-world (first (rj.e/all-e-with-c system :world))
        world (:world (rj.e/get-c-on-e system e-world :world))]
    (loop [system system]
      (let [x (rand-int (count world))
            y (rand-int (count (first world)))]
        (if (is-valid-tile? world [x y])
          (rj.wr/update-in-world system e-world [x y]
                                 (fn [entities]
                                   (item>>entities entities)))
          (recur system))))))

(defn ^:private only-floor?
  [tile]
  (every? #(#{:floor} (:type %))
          (:entities tile)))

(defn ^:private item>>entities
  [entities e-id e-type]
  (conj entities
        (rj.c/map->Entity {:id   e-id
                           :type e-type})))

(defn add-torch
  [system]
  (let [e-torch (br.e/create-entity)

        not-near-torches? (fn [world [x y]]
                            (rj.u/not-any-radially-of-type world [x y]
                                                           #(<= % 3) [:torch]))

        is-valid-tile? (fn [world [x y]]
                         (let [tile (get-in world [x y])]
                           (and (not-near-torches? world [x y])
                                (only-floor? tile))))

        torch>>entities (fn [entities]
                          (item>>entities entities e-torch :torch))]
    (-> (item>>world system is-valid-tile?
                     torch>>entities)
        (rj.e/add-e e-torch)
        (rj.e/add-c e-torch (rj.c/map->Item {:pickup-fn pickup-item}))
        (rj.e/add-c e-torch (rj.c/map->Torch {:brightness 2}))
        (rj.e/add-c e-torch (rj.c/map->Broadcaster {:msg-fn (constantly "a torch")})))))

(defn add-gold
  [system]
  (let [e-gold (br.e/create-entity)

        is-valid-tile? (fn [world [x y]]
                         (only-floor? (get-in world [x y])))

        gold>>entities (fn [entities]
                          (item>>entities entities e-gold :gold))]
    (-> (item>>world system is-valid-tile?
                     gold>>entities)
        (rj.e/add-e e-gold)
        (rj.e/add-c e-gold (rj.c/map->Item {:pickup-fn pickup-item}))
        (rj.e/add-c e-gold (rj.c/map->Gold {:value 1}))
        (rj.e/add-c e-gold (rj.c/map->Broadcaster {:msg-fn
                                                    (fn [system e-this]
                                                      (let [value (:value (rj.e/get-c-on-e system e-this :gold))]
                                                        (str value " gold")))})))))
