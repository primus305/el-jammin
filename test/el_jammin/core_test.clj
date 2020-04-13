(ns el-jammin.core-test
  (:require [clojure.test :refer :all]
            [el-jammin.core :refer :all]
            [cljfx.api :as fx]))

(defn state-fixture
  [prepare-state]
  (def *state (atom {:selected-item nil
                     :poruka ""
                     :prikazi false
                     :stop-rec-disabled true
                     :rec-disabled false
                     :set-speed-disabled false
                     :file nil
                     :samples '()
                     :samples-title '()
                     :loop-line '()
                     :loop-end 0.0
                     :loop-line-id 1
                     :loop-lines []
                     :loop-lines-end []
                     :anim-status :stopped
                     :anim-duration 0.0
                     :to-x 0
                     :current-octave 1
                     :option "Note"
                     :chord-name "Major"
                     :chord-name-disabled true
                     :instrument "Piano"
                     :loop-instrument "Piano"
                     :l true
                     :looper-live false
                     :add-loop-line false}))
  (prepare-state))

(use-fixtures :once state-fixture)

(deftest root-test
  (testing "Root window displayed."
    (fx/mount-renderer
     *state
     (fx/create-renderer
      :middleware (fx/wrap-map-desc assoc :fx/type root)
      :opts {:fx.opt/map-event-handler map-event-handler}))))
