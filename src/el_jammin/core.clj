(ns el-jammin.core
  (:import [javafx.scene.input KeyCode KeyEvent]
           [javafx.scene.control DialogEvent Dialog]
           [javafx.stage FileChooser]
           [javafx.scene Node]
           [javafx.scene.canvas Canvas])
  (:require [el-jammin.server.connection]
            [el-jammin.state.state :refer :all]
            [el-jammin.rhythm.looper :refer :all]
            [el-jammin.rhythm.section :refer :all]
            [el-jammin.visualization.visualizer :refer :all]
            [el-jammin.event.action :refer :all]
            [cljfx.api :as fx]
            [cljfx.ext.node :as fx.ext.node]
            [cljfx.ext.list-view :as fx.ext.list-view])
  (:use [overtone.core]
        [overtone.inst.drum]
        [overtone.inst.piano]
        [overtone.inst.synth]
        [overtone.studio util]))

(defn button [{:keys [style-class event-type disable]}]
  {:fx/type :button
   :pref-width 50
   :pref-height 50
   :disable disable
   :style-class style-class
   :on-action {:event/type event-type}})

(defn- let-refs [refs desc]
  {:fx/type fx/ext-let-refs
   :refs refs
   :desc desc})

(defn- get-ref [ref]
  {:fx/type fx/ext-get-ref
   :ref ref})

(defn start-transition-on
  [{:keys [desc transition]}]
  (let-refs {::transition-node desc}
    (let [tn (get-ref ::transition-node)]
      (let-refs {::transition (assoc transition :node tn)}
        tn))))

(defn radio-group [{:keys [options value on-action disable]}]
  {:fx/type fx/ext-let-refs
   :refs {::toggle-group {:fx/type :toggle-group}}
   :desc {:fx/type :h-box
          :padding 20
          :spacing 10
          :children (for [option options]
                      {:fx/type :radio-button
                       :toggle-group {:fx/type fx/ext-get-ref
                                      :ref ::toggle-group}
                       :selected (= option value)
                       :text (str option)
                       :disable disable
                       :on-action (assoc on-action :option option)})}})

(defn root [{:keys [poruka prikazi stop-rec-disabled rec-disabled set-speed-disabled file samples-title loop-line anim-status anim-duration to-x current-octave option chord-name chord-name-disabled instrument]}]
  {:fx/type fx/ext-many
   :desc [{:fx/type :stage
           :showing true
           :title "el-jammin"
           :maximized true
           :on-close-request {:event/type ::exit}
           :scene {:fx/type :scene
                   :on-key-pressed {:event/type ::press}
                   :stylesheets #{"stylesheet.css"}
                   :root {:fx/type :border-pane
                          :style-class "background"
                          :top {:fx/type :grid-pane
                                :vgap 1
                                :hgap 8
                                :padding 5
                                :children [{:fx/type fx.ext.node/with-tooltip-props
                                            :props {:tooltip {:fx/type :tooltip :text "Play instrument"}}
                                            :desc {:fx/type button
                                                   :disable false
                                                   :style-class "btnPlay"
                                                   :event-type ::play}
                                            :grid-pane/column 0
                                            :grid-pane/row 0}
                                           {:fx/type fx.ext.node/with-tooltip-props
                                            :props {:tooltip {:fx/type :tooltip :text "Stop all instruments"}}
                                            :desc {:fx/type button
                                                   :disable false
                                                   :style-class "btnStop"
                                                   :event-type ::stop}
                                            :grid-pane/column 1
                                            :grid-pane/row 0}
                                           {:fx/type fx.ext.node/with-tooltip-props
                                            :props {:tooltip {:fx/type :tooltip :text "Start recording your song"}}
                                            :desc {:fx/type button
                                                   :disable rec-disabled
                                                   :style-class "btnRec"
                                                   :event-type ::rec}
                                            :grid-pane/column 2
                                            :grid-pane/row 0}
                                           {:fx/type fx.ext.node/with-tooltip-props
                                            :props {:tooltip {:fx/type :tooltip :text "Finish recording"}}
                                            :desc {:fx/type button
                                                   :disable stop-rec-disabled
                                                   :style-class "btnStopRec"
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
                                            :disable set-speed-disabled
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
                                           {:fx/type fx.ext.node/with-tooltip-props
                                            :props {:tooltip {:fx/type :tooltip :text "Load sample"}}
                                            :desc {:fx/type button
                                                   :disable false
                                                   :style-class "btnFile"
                                                   :event-type ::ucitaj-sample}
                                            :grid-pane/column 8
                                            :grid-pane/row 0}
                                           {:fx/type fx.ext.node/with-tooltip-props
                                            :props {:tooltip {:fx/type :tooltip :text "Loop sample"}}
                                            :desc {:fx/type button
                                                   :disable false
                                                   :style-class "btnRepeat"
                                                   :event-type ::loop}
                                            :grid-pane/column 9
                                            :grid-pane/row 0}
                                           {:fx/type radio-group
                                            :options ["Piano" "Guitar" "Synthesizer"]
                                            :value instrument
                                            :disable false
                                            :on-action {:event/type ::set-instrument}
                                            :grid-pane/column 18
                                            :grid-pane/row 0}
                                           {:fx/type fx.ext.node/with-tooltip-props
                                            :props {:tooltip {:fx/type :tooltip :text "Start looping your line"}}
                                            :desc {:fx/type button
                                                   :disable false
                                                   :style-class "btnLooper"
                                                   :event-type ::looper}
                                            :grid-pane/column 19
                                            :grid-pane/row 0}
                                           {:fx/type fx.ext.node/with-tooltip-props
                                            :props {:tooltip {:fx/type :tooltip :text "Add new loop line"}}
                                            :desc {:fx/type button
                                                   :disable false
                                                   :style-class "btnAddLoop"
                                                   :event-type ::new-looper}
                                            :grid-pane/column 20
                                            :grid-pane/row 0}
                                           {:fx/type fx.ext.node/with-tooltip-props
                                            :props {:tooltip {:fx/type :tooltip :text "Restart your looper"}}
                                            :desc {:fx/type button
                                                   :disable false
                                                   :style-class "btnRestartLooper"
                                                   :event-type ::restart-looper}
                                            :grid-pane/column 21
                                            :grid-pane/row 0}
                                           {:fx/type :label
                                            :text "Octave"
                                            :grid-pane/column 26
                                            :grid-pane/row 0}
                                           {:fx/type :combo-box
                                            :value current-octave
                                            :on-value-changed {:event/type ::set-octave}
                                            :items [1 2 3 4 5 6 7 8]
                                            :grid-pane/column 27
                                            :grid-pane/row 0}
                                           {:fx/type radio-group
                                            :options ["Note" "Chord"]
                                            :value option
                                            :disable false
                                            :on-action {:event/type ::set-option}
                                            :grid-pane/column 28
                                            :grid-pane/row 0}
                                           {:fx/type radio-group
                                            :options ["Major" "Minor"]
                                            :value chord-name
                                            :disable chord-name-disabled
                                            :on-action {:event/type ::set-chord-name}
                                            :grid-pane/column 29
                                            :grid-pane/row 0}
                                           {:fx/type :label
                                            :text poruka
                                            :text-fill "#ff0000"
                                            :grid-pane/column 0
                                            :grid-pane/row 1
                                            :grid-pane/column-span 7}]}
                          :left {:fx/type :tab-pane
                                 :pref-width 350
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
                                                   :on-selected-item-changed {:event/type ::select}}}
                                        {:fx/type :tab
                                         :text "Samples"
                                         :closable false
                                         :content {:fx/type :list-view
                                                   :items samples-title
                                                   :on-selected-item-changed {:event/type ::select}}}]}
                          :center  {:fx/type :scroll-pane
                                    :fit-to-width false
                                    :content {:fx/type :grid-pane
                                              :vgap 5
                                              :hgap 10
                                              :padding 5
                                              :children  (into
                                                          (into  [{:fx/type start-transition-on
                                                                 :transition {:fx/type :translate-transition
                                                                              :duration [anim-duration :s]
                                                                              :from-x 0
                                                                              :to-x to-x
                                                                              :interpolator :ease-in               
                                                                              :cycle-count :indefinite
                                                                              :status anim-status}
                                                                 :grid-pane/row 0
                                                                 :grid-pane/column 1
                                                                 :desc {:fx/type :rectangle
                                                                        :width 5
                                                                        :height 10
                                                                        :fill :red}}]
                                                               (concat
                                                          (for [i (range 65)]
                                                          {:fx/type :label
                                                           :grid-pane/column (inc i)
                                                           :grid-pane/row 1
                                                           :grid-pane/hgrow :always
                                                           :grid-pane/vgrow :always
                                                           :style-class "loopEnd"
                                                           :text (str (double (/ i 4)))
                                                           :on-mouse-clicked {:event/type ::loop-end :i (/ i 4)}
                                                           :style (if (= i (* (@*state :loop-end) 4)) {:-fx-background-color :yellow} {})})
                                                          (for [j (range 1 13)]
                                                            {:fx/type :label
                                                             :grid-pane/column 0
                                                             :grid-pane/row (inc j)
                                                             :grid-pane/hgrow :always
                                                             :grid-pane/vgrow :always
                                                             :text (nth notes (dec j))})
                                                          (for [j (range 1 13)
                                                                i (range 65)]
                                                            {:fx/type :button
                                                            :grid-pane/column (inc i)
                                                            :grid-pane/row (inc j)
                                                            :grid-pane/hgrow :always
                                                            :grid-pane/vgrow :always
                                                             :pref-width 40
                                                             :pref-height 40
                                                             :style-class "btnNote"
                                                             :style {:-fx-background-color (if (= 0 (mod (count (filter (has-value :i i :j (dec j)) loop-line)) 2)) :lightgray :green)}
                                                             :on-action {:event/type ::play-note :j (dec j) :i i}})))
                                                          
                                                            [{:fx/type start-transition-on
                                                                 :transition {:fx/type :translate-transition
                                                                              :duration [anim-duration :s]
                                                                              :from-x 0
                                                                              :to-x to-x
                                                                              :interpolator :ease-in                
                                                                              :cycle-count :indefinite
                                                                              :status anim-status}
                                                                 :grid-pane/row 14
                                                                 :grid-pane/column 1
                                                                 :desc {:fx/type :rectangle
                                                                        :width 5
                                                                        :height 10
                                                                        :fill :red}}]
                                                            )}}
                          :bottom {:fx/type :flow-pane
                                   :vgap 5
                                   :hgap 5
                                   :padding 5
                                   :pref-height 380
                                   :children [{:fx/type :titled-pane
                                               :text "Volume control"
                                               :pref-height 360
                                               :pref-width 800
                                               :content {:fx/type :grid-pane
                                                         :vgap 20
                                                         :hgap 50
                                                         :padding 5
                                                         :children [{:fx/type :slider
                                                                     :min 0
                                                                     :max 160
                                                                     :pref-height 285
                                                                     :pref-width 50
                                                                     :value (* 100 (volume))
                                                                     :orientation :vertical
                                                                     :show-tick-marks true
                                                                     :grid-pane/column 0
                                                                     :grid-pane/row 0
                                                                     :on-value-changed {:event/type ::set-volume-k}}
                                                                    {:fx/type :slider
                                                                     :min 0
                                                                     :max 160
                                                                     :pref-height 285
                                                                     :pref-width 50
                                                                     :show-tick-marks true
                                                                     :value (* 100 (volume))
                                                                     :orientation :vertical
                                                                     :grid-pane/column 1
                                                                     :grid-pane/row 0
                                                                     :on-value-changed {:event/type ::set-volume-h}}
                                                                    {:fx/type :slider
                                                                     :min 0
                                                                     :max 160
                                                                     :pref-height 285
                                                                     :pref-width 50
                                                                     :value (* 100 (volume))
                                                                     :show-tick-marks true
                                                                     :orientation :vertical
                                                                     :grid-pane/column 2
                                                                     :grid-pane/row 0
                                                                     :on-value-changed {:event/type ::set-volume-clap}}
                                                                    {:fx/type :slider
                                                                     :min 0
                                                                     :max 160
                                                                     :pref-height 285
                                                                     :pref-width 50
                                                                     :value (* 100 (volume))
                                                                     :show-tick-marks true
                                                                     :orientation :vertical
                                                                     :grid-pane/column 3
                                                                     :grid-pane/row 0
                                                                     :on-value-changed {:event/type ::set-volume-snare}}
                                                                    {:fx/type :slider
                                                                     :min 0
                                                                     :max 160
                                                                     :pref-height 285
                                                                     :pref-width 50
                                                                     :show-tick-marks true
                                                                     :value (* 100 (volume))
                                                                     :orientation :vertical
                                                                     :grid-pane/column 4
                                                                     :grid-pane/row 0
                                                                     :on-value-changed {:event/type ::set-volume-piano}}
                                                                    {:fx/type :slider
                                                                     :min 0
                                                                     :max 160
                                                                     :pref-height 285
                                                                     :pref-width 50
                                                                     :value (* 100 (volume))
                                                                     :show-tick-marks true
                                                                     :orientation :vertical
                                                                     :grid-pane/column 5
                                                                     :grid-pane/row 0
                                                                     :on-value-changed {:event/type ::set-volume-guitar}}
                                                                    {:fx/type :slider
                                                                     :min 0
                                                                     :max 160
                                                                     :pref-height 285
                                                                     :pref-width 50
                                                                     :value (* 100 (volume))
                                                                     :show-tick-marks true
                                                                     :orientation :vertical
                                                                     :grid-pane/column 6
                                                                     :grid-pane/row 0
                                                                     :on-value-changed {:event/type ::set-volume-synth}}
                                                                    {:fx/type :slider
                                                                     :min 0
                                                                     :max 160
                                                                     :pref-height 285
                                                                     :pref-width 50
                                                                     :value (* 100 (volume))
                                                                     :show-tick-marks true
                                                                     :orientation :vertical
                                                                     :grid-pane/column 7
                                                                     :grid-pane/row 0
                                                                     :on-value-changed {:event/type ::set-volume-sample}}
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
                                                                     :grid-pane/row 1}
                                                                    {:fx/type :label
                                                                     :text "Piano"
                                                                     :grid-pane/column 4
                                                                     :grid-pane/row 1}
                                                                    {:fx/type :label
                                                                     :text "Guitar"
                                                                     :grid-pane/column 5
                                                                     :grid-pane/row 1}
                                                                    {:fx/type :label
                                                                     :text "Synth"
                                                                     :grid-pane/column 6
                                                                     :grid-pane/row 1}
                                                                    {:fx/type :label
                                                                     :text "Sample"
                                                                     :grid-pane/column 7
                                                                     :grid-pane/row 1}]}}
                                              {:fx/type :titled-pane
                                               :text "Visualizer"
                                               :pref-height 360
                                               :content {:fx/type :canvas
                                                         :width 1100
                                                         :height 300
                                                         :style-class "canvas"
                                                         :draw (fn [^Canvas canvas]
                                                                 (let [y-array (double-array 1100)
                                                                       context (.getGraphicsContext2D canvas)
                                                                       x-array (x-axis)]
                                                                   (translate-canvas context)
                                                                   (fill-canvas context x-array y-array)))}}
                                              ]}
                          }}}
          {:fx/type :text-input-dialog
           :showing prikazi
           :header-text "File name"
           :on-hidden (fn [^DialogEvent e]
                        (let [result (.getResult ^Dialog (.getSource e))]
                          (do (recording-start (str "D:/"result".wav"))
                              (swap! *state assoc :stop-rec-disabled false)
                              (swap! *state assoc :rec-disabled true)
                              (swap! *state assoc :poruka "You are recording now."))))}]})

(defn map-event-handler [e]
  (case (:event/type e)
    ::play (on-play e)
    ::stop (on-stop e)
    ::set-volume (volume (/ (:fx/event e) 100))
    ::set-speed (m :bpm (:fx/event e))
    ::set-volume-k (inst-volume! quick-kick (/ (:fx/event e) 100))
    ::set-volume-h (inst-volume! closed-hat2 (/ (:fx/event e) 100))
    ::set-volume-clap (inst-volume! clap (/ (:fx/event e) 100))
    ::set-volume-snare (inst-volume! noise-snare (/ (:fx/event e) 100))
    ::set-volume-piano (inst-volume! piano (/ (:fx/event e) 100))
    ::set-volume-guitar (inst-volume! string (/ (:fx/event e) 100))
    ::set-volume-synth (inst-volume! overpad (/ (:fx/event e) 100))
    ::set-volume-sample (inst-volume! sample-inst (/ (:fx/event e) 100))
    ::select (swap! *state assoc :selected-item (:fx/event e))
    ::rec (swap! *state assoc :prikazi true)
    ::rec-stop (on-rec-stop)
    ::ucitaj-sample (ucitaj-file (:fx/event e))
    ::press (on-press e)
    ::loop (on-loop e)
    ::play-note (on-play-note e)
    ::looper (on-looper e)
    ::loop-end (swap! *state assoc :loop-end (:i e))
    ::new-looper (on-new-looper)
    ::restart-looper (on-restart-looper e)
    ::set-octave (swap! *state assoc :current-octave (:fx/event e))
    ::set-option (on-set-option e)
    ::set-chord-name (swap! *state assoc :chord-name (:option e))
    ::set-instrument (on-set-instrument e)
    ::exit (on-exit)))

(fx/mount-renderer
  *state
  (fx/create-renderer
   :middleware (fx/wrap-map-desc assoc :fx/type root)
    :opts {:fx.opt/map-event-handler map-event-handler}))
