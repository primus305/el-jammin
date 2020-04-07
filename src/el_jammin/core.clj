(ns el-jammin.core
  (:import [javafx.scene.input KeyCode KeyEvent]
           [javafx.scene.control DialogEvent Dialog]
           [javafx.stage FileChooser]
           [javafx.scene Node]
           [javafx.scene.canvas Canvas])
  (:require [el-jammin.konekcija]
            [cljfx.api :as fx]
            [cljfx.ext.node :as fx.ext.node]
            [cljfx.ext.list-view :as fx.ext.list-view])
  (:use [overtone.core]
        [overtone.inst.drum]
        [overtone.inst.piano]
        [overtone.inst.synth]
        [overtone.studio util]))

(def m (metronome 128))
(def kick-side-bar '("Kick & Clap 1" "Kick" "Kick & Clap 2" "Kick & Clap 3" "Kick & Snare"))
(def kontra-side-bar '("Hi-hat 1" "Hi-hat 2" "Hi-hat 3" "Hi-hat 4" "Hi-hat 5"))
(def selected-item nil)
(def notes '("C" "C#" "D" "D#" "E" "F" "F#" "G" "G#" "A" "A#" "B"))

(defonce SCOPE-BUF-SIZE 2048)
(def scope
  (atom {:first-time true
         :playing false}))

(def *state
  (atom {:poruka ""
         :prikazi false
         :stop-rec-disabled true
         :rec-disabled false
         :set-speed-disabled false
         :file nil
         :samples '()
         :samples-title '()
         :loop-line '()
         :loop-end 0.0
         :loop-lines []
         :loop-lines-end []
         :anim-status :stopped
         :anim-duration 0.0
         :to-x 0
         :current-octave 1
         :option "Note"
         :chord-name "Major"
         :chord-name-disabled true
         :instrument "Piano"}))

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

(defn parse-int [s]
  (Integer. (re-find  #"\d+" s )))

(defn has-value [key1 value1 key2 value2]
  (fn [m]
    (and (= value1 (m key1)) (= value2 (m key2)))))

(defn update-scope-data
  [s]
  (let [{:keys [buf size y-arrays x-array]} s
        frames    (if (buffer-live? buf)
                    (buffer-read buf)
                    [])
        step      (/ (:size buf) 1100)
        y-scale   100
        [y-a y-b] @y-arrays]

    (when-not (empty? frames)
      (dotimes [x 1100]
        (aset ^doubles y-b x
              (double (* y-scale
                         (aget ^floats frames (unchecked-multiply x step))))))
      (reset! y-arrays [y-b y-a]))))

(defn x-axis
  []
  (let [x-array (double-array 1100)]
    (dotimes [i 1100]
      (aset ^doubles x-array i (double i)))
    x-array))

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
                                            :grid-pane/column 24
                                            :grid-pane/row 0}
                                           {:fx/type fx.ext.node/with-tooltip-props
                                            :props {:tooltip {:fx/type :tooltip :text "Start looping your line"}}
                                            :desc {:fx/type button
                                                   :disable false
                                                   :style-class "btnLooper"
                                                   :event-type ::looper}
                                            :grid-pane/column 25
                                            :grid-pane/row 0}
                                           {:fx/type fx.ext.node/with-tooltip-props
                                            :props {:tooltip {:fx/type :tooltip :text "Add new loop"}}
                                            :desc {:fx/type button
                                                   :disable false
                                                   :style-class "btnAddLoop"
                                                   :event-type ::new-looper}
                                            :grid-pane/column 26
                                            :grid-pane/row 0}
                                           {:fx/type :label
                                            :text "Octave"
                                            :grid-pane/column 27
                                            :grid-pane/row 0}
                                           {:fx/type :combo-box
                                            :value current-octave
                                            :on-value-changed {:event/type ::set-octave}
                                            :items [1 2 3 4 5 6 7 8]
                                            :grid-pane/column 28
                                            :grid-pane/row 0}
                                           {:fx/type radio-group
                                            :options ["Note" "Chord"]
                                            :value option
                                            :disable false
                                            :on-action {:event/type ::set-option}
                                            :grid-pane/column 29
                                            :grid-pane/row 0}
                                           {:fx/type radio-group
                                            :options ["Major" "Minor"]
                                            :value chord-name
                                            :disable chord-name-disabled
                                            :on-action {:event/type ::set-chord-name}
                                            :grid-pane/column 30
                                            :grid-pane/row 0}
                                           {:fx/type :label
                                            :text poruka
                                            :text-fill "#ff0000"
                                            :grid-pane/column 0
                                            :grid-pane/row 1
                                            :grid-pane/column-span 5}]}
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
                                                             :pref-width 30
                                                             :pref-height 30
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
                                   :hgap 50
                                   :padding 5
                                   :pref-height 380
                                   :children [{:fx/type :titled-pane
                                               :text "Volume control"
                                               :pref-height 360
                                               :content {:fx/type :grid-pane
                                                         :vgap 20
                                                         :hgap 40
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
                                                                   (when (= (:first-time @scope) true)
                                                                     (do
                                                                       (.translate context 0.0 150.0)
                                                                       (swap! scope assoc :first-time false)))
                                                                   (dotimes [x 1100]
                                                                      (.setFill context javafx.scene.paint.Color/BLUE)
                                                                      (.fillOval context (int (nth x-array x)) (int (nth y-array x)) 1 1))))}}
                                              ]}
                          }}}
          {:fx/type :text-input-dialog
           :showing prikazi
           :header-text "File name"
           :on-hidden (fn [^DialogEvent e]
                        (let [result (.getResult ^Dialog (.getSource e))]
                          (do (recording-start (str "D:/"result".wav"))
                              (swap! *state assoc :stop-rec-disabled false)
                              (swap! *state assoc :rec-disabled true))))}]})

(definst string [note 60 amp 1.0 dur 0.5 decay 30 coef 0.3 gate 1]
  (let [freq (midicps note)
        noize (* 0.8 (white-noise))
        dly   (/ 1.0 freq)
        plk   (pluck noize gate dly dly decay coef)
        dist  (distort plk)
        filt  (rlpf dist (* 12 freq) 0.6)
        clp   (clip2 filt 0.8)
        reverb (free-verb clp 0.4 0.8 0.2)]
    (* amp (env-gen (perc 0.0001 dur) :action 0) reverb)))

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

(defn ucitaj-file
  [event]
  (let [window (.getWindow (.getScene ^Node (.getTarget event)))
        chooser (doto (FileChooser.)
                  (.setTitle "Open File"))]
    (when-let [file @(fx/on-fx-thread (.showOpenDialog chooser window))]
      (swap! *state assoc :file file)
      (swap! *state assoc :samples (cons (sample (str file)) (@*state :samples)))
      (swap! *state assoc :samples-title (cons (:name (sample (str file))) (@*state :samples-title))))))

(definst sample-inst
  [note 59 level 1 rate 1 loop? 0
   attack 0 decay 1 sustain 1 release 0.1 curve -4 gate 1 buf (buffer 2048)]
  (let [env (env-gen (adsr attack decay sustain release level curve)
                     :gate gate
                     :action FREE)]
    (* env (scaled-play-buf 1 buf :level level :loop loop? :action FREE))))

(defn play-sample
  [smpl loop]
  (sample-inst :loop? loop :buf (first (filter (fn [x] (= (:name x) smpl)) (@*state :samples)))))

(defn play-chord [instrument root chord-name]
  (doseq [note (chord root chord-name)]
    (instrument note)))

(defn play-note
  [event instr]
  (let [j (:j event)
        i (:i event)
        octave (@*state :current-octave)
        option (@*state :option)
        chord-name (keyword (clojure.string/lower-case (@*state :chord-name)))]
    (swap! *state assoc :loop-line (cons (assoc {} :i i :j j :beat (* i 0.25) :note (str (nth notes j)octave) :option option :chord-name chord-name) (@*state :loop-line)))
    (if (= option "Note")
      (instr (note (str (nth notes j)octave)))
      (play-chord instr (note (str (nth notes j)octave)) chord-name))))

(defn play-looper
  [beat x instr]
    (let [loop-line (x (@*state :loop-lines))
          next-beat (+ (x (@*state :loop-lines-end)) beat)
          chord-name (keyword (clojure.string/lower-case (@*state :chord-name)))]
      (dotimes [x (count loop-line)]
        (at (m (+ (:beat (nth loop-line x)) beat))
            (if (= (:option (nth loop-line x)) "Note")
              (instr (note (:note (nth loop-line x))))
              (play-chord instr (note (:note (nth loop-line x))) (:chord-name (nth loop-line x))))))
      (apply-by (m next-beat) #'play-looper [next-beat x instr])))

(defn remove-from-loop-line
  []
  (loop [loop-line (@*state :loop-line)
         cleaned-line (@*state :loop-line)]
    (if (empty? loop-line)
      cleaned-line
      (let [i (:i (first loop-line))
            j (:j (first loop-line))]
        (recur (rest loop-line) (if (= 0 (mod (count (filter (has-value :i i :j j) cleaned-line)) 2))
                                  (remove #(= (first (filter (has-value :i i :j j) cleaned-line)) %) cleaned-line)
                                  cleaned-line))))))

(defn cleaned-loop-line
  []
  (loop [loop-line (remove-from-loop-line)
         cleaned-line '()]
    (if (empty? loop-line)
      cleaned-line
      (let [i (:i (first loop-line))
            j (:j (first loop-line))]
        (recur (rest loop-line) (if (= (count (filter (has-value :i i :j j) cleaned-line)) 0)
                                  (cons (first (filter (has-value :i i :j j) loop-line)) cleaned-line)
                                  cleaned-line))))))

(defn clean-loop-line
  []
  (when (< (count (@*state :loop-lines)) 3)
    (let [cleaned-line-loop (cleaned-loop-line)]
      (swap! *state assoc :loop-line cleaned-line-loop)
      (swap! *state assoc :loop-lines (conj (@*state :loop-lines) cleaned-line-loop))
      (swap! *state assoc :loop-lines-end (conj (@*state :loop-lines-end) (@*state :loop-end))))))

(defn start-animation
  []
  (do
    (swap! *state assoc :anim-duration (* (last (@*state :loop-lines-end)) (double (/ 60 (:bpm m)))))
    (swap! *state assoc :to-x (* 160 (last (@*state :loop-lines-end))))
    (swap! *state assoc :anim-status :running)))

(defn clean-and-play
  [instr]
  (clean-loop-line)
  (start-animation)
  (case (count (@*state :loop-lines))
    1 (play-looper (m) first instr)
    2 (play-looper (m) second instr)
    3 (play-looper (m) last instr)))

(defn play-from-keyboard
  [event instr]
  (let [cd (.getCode ^KeyEvent (:fx/event event))
        octave (@*state :current-octave)
        option (@*state :option)
        chord-name (keyword (clojure.string/lower-case (@*state :chord-name)))]
    (when (= cd KeyCode/A) (if (= option "Note") (instr (note (str "C"octave))) (play-chord instr (note (str "C"octave)) chord-name)))
    (when (= cd KeyCode/W) (if (= option "Note") (instr (note (str "C#"octave))) (play-chord instr (note (str "C#"octave)) chord-name)))
    (when (= cd KeyCode/S) (if (= option "Note") (instr (note (str "D"octave))) (play-chord instr (note (str "D"octave)) chord-name)))
    (when (= cd KeyCode/E) (if (= option "Note") (instr (note (str "D#"octave))) (play-chord instr (note (str "D#"octave)) chord-name)))
    (when (= cd KeyCode/D) (if (= option "Note") (instr (note (str "E"octave))) (play-chord instr (note (str "E"octave)) chord-name)))
    (when (= cd KeyCode/F) (if (= option "Note") (instr (note (str "F"octave))) (play-chord instr (note (str "F"octave)) chord-name)))
    (when (= cd KeyCode/R) (if (= option "Note") (instr (note (str "F#"octave))) (play-chord instr (note (str "F#"octave)) chord-name)))
    (when (= cd KeyCode/G) (if (= option "Note") (instr (note (str "G"octave))) (play-chord instr (note (str "G"octave)) chord-name)))
    (when (= cd KeyCode/T) (if (= option "Note") (instr (note (str "G#"octave))) (play-chord instr (note (str "G#"octave)) chord-name)))
    (when (= cd KeyCode/H) (if (= option "Note") (instr (note (str "A"octave))) (play-chord instr (note (str "A"octave)) chord-name)))
    (when (= cd KeyCode/Y) (if (= option "Note") (instr (note (str "A#"octave))) (play-chord instr (note (str "A#"octave)) chord-name)))
    (when (= cd KeyCode/J) (if (= option "Note") (instr (note (str "B"octave))) (play-chord instr (note (str "B"octave)) chord-name)))))

(defn reset-data-arrays
  [scope]
  (let [x-array   (scope :x-array)
        [y-a y-b] @(scope :y-arrays)]

    (dotimes [i 1100]
      (aset ^doubles x-array i (double i)))

    (dotimes [i 1100]
      (aset ^doubles y-a i (double (/ 300 2)))
      (aset ^doubles y-b i (double (/ 300 2))))))

(defn start-bus-synth
  [bus buf control-rate?]
  (if control-rate?
    (control-bus->buf bus buf)
    (bus->buf bus buf)))

(defn scope-bus
  [s control-rate?]
  (let [buf       (buffer SCOPE-BUF-SIZE)
        bus-synth (start-bus-synth (:thing s) buf control-rate?)]
    (assoc s
           :size SCOPE-BUF-SIZE
           :bus-synth bus-synth
           :buf buf)))

(defn create-scope
  [thing kind keep-on-top width height]
  (let [thing-id (to-sc-id thing)
        x-array  (double-array width)
        y-a      (double-array width)
        y-b      (double-array width)
        scope    {:id     thing-id
                  :size       0
                  :thing      thing
                  :kind       kind
                  :x-array    x-array
                  :y-arrays   (atom [y-a y-b])}

        _        (reset-data-arrays scope)]
    
    (case kind
      :control-bus (scope-bus scope true)
      :bus (scope-bus scope false)
      :audio-bus (scope-bus scope false))))

(defn draw-canvas
  [context]
  (let [[y-a y-b] @(:y-arrays (:scope @scope))
        x-array (:x-array (:scope @scope))]
    (.clearRect context 0 -150 1100 300)
    (.clearRect context 0 0 1100 300)
    (.setFill context javafx.scene.paint.Color/BLUE)
    (.fillPolygon context x-array y-a 1100)
    (when (every? #(> 2.0 %) y-a)
      (dotimes [x 1100]
        (.fillOval context (int (nth x-array x)) (int (nth y-a x)) 1 1)))))

(defn loop-timer
  [event]
  (let [scene (.getScene ^Node (.getTarget event))
        canvas (.lookup scene ".canvas")
        context (.getGraphicsContext2D canvas)
        timer (proxy [javafx.animation.AnimationTimer] []
                (handle [now]
                  (update-scope-data (:scope @scope))
                  (draw-canvas context)))]
    (swap! scope assoc :timer timer)
    (.start timer)))

(defn start-scope
  [event]
  (let [s  (create-scope 1 :audio-bus false 1100 300)]
    (swap! scope assoc :scope s)
    (swap! scope assoc :playing true)
    (loop-timer event)))

(defn stop-timer
  [event]
  (let [timer (:timer @scope)
        scene (.getScene ^Node (.getTarget event))
        canvas (.lookup scene ".canvas")
        context (.getGraphicsContext2D canvas)
        x-array (:x-array (:scope @scope))
        y-array (double-array 1100)]
    (.stop timer)
    (swap! scope assoc :playing false)
    (.clearRect context 0 -150 1100 300)
    (.clearRect context 0 0 1100 300)
    (dotimes [x 1100]
      (.setFill context javafx.scene.paint.Color/BLUE)
      (.fillOval context (int (nth x-array x)) (int (nth y-array x)) 1 1))))

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
                 nil (swap! *state assoc :poruka "You must select instrument.")
                 (play-sample selected-item 0))
               (if (not= selected-item nil)
                 (swap! *state assoc :poruka ""))
               (when (= false (:playing @scope))
                 (start-scope (:fx/event e))))
    ::stop (do (stop)
               (swap! *state assoc :anim-status :stopped)
               (swap! *state assoc :set-speed-disabled false)
               (stop-timer (:fx/event e)))
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
    ::select (def selected-item (:fx/event e))
    ::rec (swap! *state assoc :prikazi true)
    ::rec-stop (do (recording-stop)
                   (swap! *state assoc :stop-rec-disabled true)
                   (swap! *state assoc :rec-disabled false))
    ::ucitaj-sample (ucitaj-file (:fx/event e))
    ::press (do
              (case (@*state :instrument)
                "Piano" (play-from-keyboard e piano)
                "Guitar" (play-from-keyboard e string)
                "Synthesizer" (play-from-keyboard e overpad))
              (when (= false (:playing @scope))
                (start-scope (:fx/event e))))
    ::loop (if (= selected-item nil)
             (swap! *state assoc :poruka "You must select sample.")
             (case (re-find #".wav" selected-item)
               nil (swap! *state assoc :poruka "You must select sample.")
               (do (play-sample selected-item 1)
                   (swap! *state assoc :poruka "")
                   (when (= false (:playing @scope))
                     (start-scope (:fx/event e))))))
    ::play-note (do
                  (case (@*state :instrument)
                    "Piano" (play-note e piano)
                    "Guitar" (play-note e string)
                    "Synthesizer" (play-note e overpad))
                  (when (= false (:playing @scope))
                    (start-scope (:fx/event e))))
    ::looper (if (= 0.0 (@*state :loop-end))
               (swap! *state assoc :poruka "You must select end of the loop.")
               (do
                 (swap! *state assoc :poruka "")
                 (swap! *state assoc :set-speed-disabled true)
                 (case (@*state :instrument)
                   "Piano" (clean-and-play piano)
                   "Guitar" (clean-and-play string)
                   "Synthesizer" (clean-and-play overpad))))
    ::loop-end (swap! *state assoc :loop-end (:i e))
    ::new-looper (if (>= (count (@*state :loop-lines)) 3)
                   (swap! *state assoc :poruka "Maximum number of loops.")
                   ((swap! *state assoc :loop-line '())
                    (swap! *state assoc :loop-end 0.0)))
    ::set-octave (swap! *state assoc :current-octave (:fx/event e))
    ::set-option (do
                   (swap! *state assoc :option (:option e))
                   (if (= (:option e) "Chord")
                     (swap! *state assoc :chord-name-disabled false)
                     (swap! *state assoc :chord-name-disabled true)))
    ::set-chord-name (swap! *state assoc :chord-name (:option e))
    ::set-instrument (swap! *state assoc :instrument (:option e))
    ::exit (do
             (when (= true (:playing @scope))
               (.stop (:timer @scope)))
             (stop))))

(fx/mount-renderer
  *state
  (fx/create-renderer
   :middleware (fx/wrap-map-desc assoc :fx/type root)
    :opts {:fx.opt/map-event-handler map-event-handler}))
