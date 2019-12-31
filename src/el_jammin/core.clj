(ns el-jammin.core
  (:import [javafx.scene.input KeyCode KeyEvent]
          [javafx.scene.control DialogEvent Dialog])
  (:require [el-jammin.konekcija]
            [cljfx.api :as fx]
            [cljfx.ext.node :as fx.ext.node]
            [cljfx.ext.list-view :as fx.ext.list-view])
  (:use [overtone.core]
        [overtone.inst.drum]))

(def m (metronome 128))
(def kick-side-bar '("Kick & Clap 1" "Kick" "Kick & Clap 2" "Kick & Clap 3" "Kick & Snare"))
(def kontra-side-bar '("Hi-hat 1" "Hi-hat 2" "Hi-hat 3" "Hi-hat 4" "Hi-hat 5"))
(def selected-item nil)

(def *state
  (atom {:poruka ""
         :prikazi false
         :stop-rec-disabled true
         :rec-disabled false}))

(defn button [{:keys [text event-type disable]}]
  {:fx/type :button
   :text text
   :pref-width 68
   :disable disable
   :on-action {:event/type event-type}})

(defn root [{:keys [poruka prikazi stop-rec-disabled rec-disabled]}]
  {:fx/type fx/ext-many
   :desc [{:fx/type :stage
           :showing true
           :title "Pane examples"
           :scene {:fx/type :scene
                   :root {:fx/type :border-pane
                          :top {:fx/type :grid-pane
                                :vgap 1
                                :hgap 8
                                :padding 5
                                :children [{:fx/type fx.ext.node/with-tooltip-props
                                            :props {:tooltip {:fx/type :tooltip :text "Play instrument"}}
                                            :desc {:fx/type button
                                                   :text "Play"
                                                   :disable false
                                                   :event-type ::play}
                                            :grid-pane/column 0
                                            :grid-pane/row 0}
                                           {:fx/type fx.ext.node/with-tooltip-props
                                            :props {:tooltip {:fx/type :tooltip :text "Stop all instruments"}}
                                            :desc {:fx/type button
                                                   :text "Stop"
                                                   :disable false
                                                   :event-type ::stop}
                                            :grid-pane/column 1
                                            :grid-pane/row 0}
                                           {:fx/type fx.ext.node/with-tooltip-props
                                            :props {:tooltip {:fx/type :tooltip :text "Start recording your song"}}
                                            :desc {:fx/type button
                                                   :text "Rec"
                                                   :disable rec-disabled
                                                   :event-type ::rec}
                                            :grid-pane/column 2
                                            :grid-pane/row 0}
                                           {:fx/type fx.ext.node/with-tooltip-props
                                            :props {:tooltip {:fx/type :tooltip :text "Finish recording"}}
                                            :desc {:fx/type button
                                                   :text "Stop Rec"
                                                   :disable stop-rec-disabled
                                                   :event-type ::rec-stop}
                                            :grid-pane/column 3
                                            :grid-pane/row 0}
                                           {:fx/type :label
                                            :text "BPM"
                                            :grid-pane/column 4
                                            :grid-pane/row 0}
                                           {:fx/type :slider
                                            :min 50
                                            :max 210
                                            :value (:bpm m)
                                            :show-tick-marks true
                                            :on-value-changed {:event/type ::set-speed}
                                            :grid-pane/column 5
                                            :grid-pane/row 0}
                                           {:fx/type :label
                                            :text "Master volume"
                                            :grid-pane/column 6
                                            :grid-pane/row 0}
                                           {:fx/type :slider
                                            :min 0
                                            :max 160
                                            :value (* 100 (volume))
                                            :show-tick-marks true
                                            :on-value-changed {:event/type ::set-volume}
                                            :grid-pane/column 7
                                            :grid-pane/row 0}
                                           {:fx/type :label
                                            :text poruka
                                            :text-fill "#ff0000"
                                            :grid-pane/column 0
                                            :grid-pane/row 1
                                            :grid-pane/column-span 3}]}
                          :left {:fx/type :tab-pane
                                 :pref-width 300
                                 :pref-height 540
                                 :tabs [{:fx/type :tab
                                         :text "Kick"
                                         :closable false
                                         :content {:fx/type :list-view
                                                   :items kick-side-bar
                                                   :on-selected-item-changed {:event/type ::select}}}
                                        {:fx/type :tab
                                         :text "Hi-hat"
                                         :closable false
                                         :content {:fx/type :list-view
                                                   :items kontra-side-bar
                                                   :on-selected-item-changed {:event/type ::select}}}]}
                          :center  {:fx/type :label
                                    :text ";TODO"}
                          :bottom {:fx/type :flow-pane
                                   :vgap 5
                                   :hgap 5
                                   :padding 5
                                   :children [{:fx/type :titled-pane
                                               :text "Control volume pane"
                                               :content {:fx/type :grid-pane
                                                         :vgap 20
                                                         :hgap 40
                                                         :padding 5
                                                         :children [{:fx/type :slider
                                                                     :min 0
                                                                     :max 160
                                                                     :value (* 100 (volume))
                                                                     :orientation :vertical
                                                                     :show-tick-marks true
                                                                     :grid-pane/column 0
                                                                     :grid-pane/row 0
                                                                     :on-value-changed {:event/type ::set-volume-k}}
                                                                    {:fx/type :slider
                                                                     :min 0
                                                                     :max 160
                                                                     :show-tick-marks true
                                                                     :value (* 100 (volume))
                                                                     :orientation :vertical
                                                                     :grid-pane/column 1
                                                                     :grid-pane/row 0
                                                                     :on-value-changed {:event/type ::set-volume-h}}
                                                                    {:fx/type :slider
                                                                     :min 0
                                                                     :max 160
                                                                     :value (* 100 (volume))
                                                                     :show-tick-marks true
                                                                     :orientation :vertical
                                                                     :grid-pane/column 2
                                                                     :grid-pane/row 0
                                                                     :on-value-changed {:event/type ::set-volume-clap}}
                                                                    {:fx/type :slider
                                                                     :min 0
                                                                     :max 160
                                                                     :value (* 100 (volume))
                                                                     :show-tick-marks true
                                                                     :orientation :vertical
                                                                     :grid-pane/column 3
                                                                     :grid-pane/row 0
                                                                     :on-value-changed {:event/type ::set-volume-snare}}
                                                                    {:fx/type :label
                                                                     :text "Kick"
                                                                     :grid-pane/column 0
                                                                     :grid-pane/row 1}
                                                                    {:fx/type :label
                                                                     :text "Hat"
                                                                     :grid-pane/column 1
                                                                     :grid-pane/row 1}
                                                                    {:fx/type :label
                                                                     :text "Clap"
                                                                     :grid-pane/column 2
                                                                     :grid-pane/row 1}
                                                                    {:fx/type :label
                                                                     :text "Snare"
                                                                     :grid-pane/column 3
                                                                     :grid-pane/row 1}]}}
                                              ;;TODO...
                                              ;;More panels...
                                              ]}
                          :pref-width 960
                          :pref-height 540}}}
          {:fx/type :text-input-dialog
           :showing prikazi
           :header-text "File name"
           :on-hidden (fn [^DialogEvent e]
                        (let [result (.getResult ^Dialog (.getSource e))]
                          (do (recording-start (str "D:/"result".wav"))
                              (swap! *state assoc :stop-rec-disabled false)
                              (swap! *state assoc :rec-disabled true))))}]})

(defn my-sequencer [my-metronome beat pattern scale idx]
  (doseq [[sound ptn] @pattern]
    (when (= 1 (nth ptn (mod idx (count ptn))))
      (at (my-metronome beat) (sound))))
  (let [next-beat (+ scale beat)]
    (apply-by (my-metronome next-beat) my-sequencer [my-metronome next-beat pattern scale (inc idx)])))

(defn kick-clap1
  [beat]
  (let [next-beat (inc beat)]
    (at (m beat)
        (quick-kick :amp 0.5)
        (if (zero? (mod beat 2))
          (clap :amp 0.1)))

   (apply-by (m next-beat) #'kick-clap1 [next-beat])))

(defn my-hihat1
  [beat]
  (let [next-beat (inc beat)]
    (at (m beat)
        (closed-hat2 :amp 0.3))
    (at (m (+ 0.5 beat))
        (closed-hat2 :amp 0.3))

    (apply-by (m next-beat) #'my-hihat1 [next-beat])))

(defn my-hihat2
  [beat]
  (let [next-beat (inc beat)]
    (at (m beat)
        (soft-hat :decay 0.03 :amp 0.5))
    (at (m (+ 0.25 beat))
        (soft-hat :decay 0.03 :amp 0.5))
    (at (m (+ 0.5 beat))
        (soft-hat :decay 0.03 :amp 0.5))
    (at (m (+ 0.75 beat))
        (soft-hat :decay 0.03 :amp 0.5))

    (apply-by (m next-beat) #'my-hihat2 [next-beat])))

(defn my-hihat3
  [beat]
  (let [next-beat (inc beat)]
    (at (m beat)
        (closed-hat2 :amp 0.3))
    (at (m (+ 0.25 beat))
        (closed-hat2 :amp 0.3))
    (at (m (+ 0.5 beat))
        (closed-hat2 :amp 0.3))
    (at (m (+ 0.75 beat))
        (closed-hat2 :amp 0.3))

    (apply-by (m next-beat) #'my-hihat3 [next-beat])))

(defn my-hihat4
  [beat]
  (let [next-beat (inc beat)]
    (at (m beat)
        (closed-hat2 :amp 0.3))

    (apply-by (m next-beat) #'my-hihat4 [next-beat])))

(defn my-hihat5
  [beat]
  (let [next-beat (inc beat)]
    (at (m (+ 0.5 beat))
        (closed-hat2 :amp 0.3))

    (apply-by (m next-beat) #'my-hihat5 [next-beat])))

(defn my-kick
  [beat]
  (let [next-beat (inc beat)]
    (at (m beat)
        (quick-kick :amp 0.5))

    (apply-by (m next-beat) #'my-kick [next-beat])))

(def ritam3
  {quick-kick    [1 0 0 0 0 0 0 1 0 0 1 0 0 0 0 0 ]
   clap    [0 0 0 0 1 0 0 0 0 0 0 0 1 0 0 0 ]})

(def *ritam3 (atom ritam3))

(defn kick-clap2
  [beat]
  (my-sequencer m beat *ritam3 1/4 0))

(def ritam4
  {quick-kick    [1 0 0 0 0 0 0 0 1 0 1 0 0 0 0 0 ]
   clap    [0 0 0 0 1 0 0 0 0 0 0 0 1 0 0 0 ]})

(def *ritam4 (atom ritam4))

(defn kick-clap3
  [beat]
  (my-sequencer m beat *ritam4 1/4 0))

(def ritam5
  {quick-kick    [1 0 0 1 0 0 1 0 0 0 0 0 0 0 0 0 ]
   noise-snare   [0 0 0 0 0 0 0 0 1 0 0 0 0 0 0 0 ]})

(def *ritam5 (atom ritam5))

(defn kick-snare
  [beat]
    (my-sequencer m beat *ritam5 1/4 0))

(defn map-event-handler [e]
  (case (:event/type e)
    ::play (do (case selected-item
                 "Kick & Clap 1" (kick-clap1 (m))
                 "Kick" (my-kick (m))
                 "Kick & Clap 2" (kick-clap2 (m))
                 "Kick & Clap 3" (kick-clap3 (m))
                 "Kick & Snare" (kick-snare (m))
                 "Hi-hat 1" (my-hihat1 (m))
                 "Hi-hat 2" (my-hihat2 (m))
                 "Hi-hat 3" (my-hihat3 (m))
                 "Hi-hat 4" (my-hihat4 (m))
                 "Hi-hat 5" (my-hihat5 (m))
                 nil (swap! *state assoc :poruka "You must select instrument."))
               (if (not= selected-item nil)
                 (swap! *state assoc :poruka "")))
    ::stop (stop)
    ::set-volume (volume (/ (:fx/event e) 100))
    ::set-speed (m :bpm (:fx/event e))
    ::set-volume-k (inst-volume! quick-kick (/ (:fx/event e) 100))
    ::set-volume-h (inst-volume! closed-hat2 (/ (:fx/event e) 100))
    ::set-volume-clap (inst-volume! clap (/ (:fx/event e) 100))
    ::set-volume-snare (inst-volume! noise-snare (/ (:fx/event e) 100))
    ::select (def selected-item (:fx/event e))
    ::rec (swap! *state assoc :prikazi true)
    ::rec-stop (do (recording-stop)
                   (swap! *state assoc :stop-rec-disabled true)
                   (swap! *state assoc :rec-disabled false))))

(fx/mount-renderer
  *state
  (fx/create-renderer
   :middleware (fx/wrap-map-desc assoc :fx/type root)
    :opts {:fx.opt/map-event-handler map-event-handler}))
