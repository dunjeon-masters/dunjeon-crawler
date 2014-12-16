(ns rouje-like.t-rooms
  (:use [midje.sweet]
        [rouje-like.test-utils]
        [rouje-like.rooms])
  (:require [rouje-like.utils :as rj.u :refer [?]]))

(fact "gen-level"
      (gen-level 3 3 :t) => {:last-add? nil,
                             :level [[[0 0 :t {}] [0 1 :t {}] [0 2 :t {}]]
                                     [[1 0 :t {}] [1 1 :t {}] [1 2 :t {}]]
                                     [[2 0 :t {}] [2 1 :t {}] [2 2 :t {}]]],
                             :rooms []})

(facts "print-level"
       (fact "bad type"
             (with-out-str
               (print-level (gen-level 3 3 :wrong)))
             => (or "  (0 1 2)\n0 (Q Q Q)\n1 (Q Q Q)\n2 (Q Q Q)\n"
                    "  (0 1 2)\r\n0 (Q Q Q)\r\n1 (Q Q Q)\r\n2 (Q Q Q)\r\n"))
       (fact "good values"
             (with-out-str
               (print-level (gen-level 3 3 :f)))
             => (or "  (0 1 2)\n0 (_ _ _)\n1 (_ _ _)\n2 (_ _ _)\n"
                    "  (0 1 2)\r\n0 (_ _ _)\r\n1 (_ _ _)\r\n2 (_ _ _)\r\n")))

(facts "flatten-level"
       (flatten-level (gen-level 3 3)) =future=> falsey)

(facts "valid-door-locs"
       (fact "odd dimensions"
             (valid-door-locs 1 1 3 3) => [[1 2] [2 1] [2 3] [3 2]])
       (fact "even dimensions"
             (valid-door-locs 1 1 4 4) => [[1 3] [3 1] [3 4] [4 3]]))

(fact "create-room"
      (create-room [1 1] [3 3]) => (contains {:height 3, :width 3, :x 1, :y 1}))

(fact "change-in-level"
      (vec (change-in-level [1 2 :f {}]
                       (:level
                         (gen-level 3 3 :f)))) => [[[0 0 :f {}] [0 1 :f {}] [0 2 :f {}]]
                                                   [[1 0 :f {}] [1 1 :f {}] [1 2 :f {}]]
                                                   [[2 0 :f {}] [2 1 :f {}] [2 2 :f {}]]])

(defn points->2DPoints [points]
  (vec (map #(vec (take 2 %)) points)))

(fact "room->points"
      (points->2DPoints (vec (room->points (create-room [1 2] [3 3]))))
      => (contains [[2 3] [3 2] [1 4] [2 2] [1 3] [2 4] [3 3] [3 4] [1 2]]
                   :in-any-order))

(facts "room-in-level?"
      (fact "not in level"
            (room-in-level? (:level (gen-level 3 3 :w))
                            (create-room [1 1] [3 3])) => false)
      (fact "in level"
            (room-in-level? (:level (gen-level 5 5 :w))
                            (create-room [1 1] [2 2])) => true))

(facts "overlapping?"
       (fact "not overlapping"
             (overlapping? [(create-room [15 15] [2 2]) (create-room [1 1] [3 3])] (create-room [7 7] [3 3])) => false)
       (fact "overlapping"
             (overlapping? [(create-room [15 15] [2 2]) (create-room [1 1] [3 3])] (create-room [2 2] [3 3])) => true))

(defn check-rooms
  [level expecting-success?]
  (let [rooms (:rooms level)
        last-add (:last-add? level)]
    (if expecting-success?
      (and (seq rooms)
           last-add)
      (and (empty? rooms)
           (not last-add)))))

(facts "add-room"
      (fact "success"
            (add-room (gen-level 5 5 :w) (create-room [1 1] [2 2])) => #(check-rooms % true))
      (fact "fail"
            (add-room (gen-level 5 5 :w) (create-room [6 6] [3 3])) => #(check-rooms % false)
            (add-room (gen-level 5 5 :wrong) (create-room [2 2] [3 3])) => #(check-rooms % false)))

(defn check-num-rooms
  [level num-rooms]
  (let [floor (:level level)
        type-vec (filter #(= :d (% 2))
                         (flatten-level floor))]
    (= (count type-vec) num-rooms)))

(fact "gen-level-with-rooms"
      (gen-level-with-rooms 10 10 1 5) => #(check-num-rooms % 1)
      (gen-level-with-rooms 12 12 2 3) => #(check-num-rooms % 2)
      (gen-level-with-rooms 12 12 4 3) => #(check-num-rooms % 4)
      (gen-level-with-rooms 2 2 4 2) => (throws java.lang.AssertionError))
