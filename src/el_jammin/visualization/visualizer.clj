(ns el-jammin.visualization.visualizer
	(:import [javafx.scene Node]
			 [javafx.scene.canvas Canvas])
	(:require [el-jammin.state.state :refer :all])
    (:use [overtone.core]
	      [overtone.studio util]))

(defonce SCOPE-BUF-SIZE 2048)

(defn x-axis
  []
  (let [x-array (double-array 1100)]
    (dotimes [i 1100]
      (aset ^doubles x-array i (double i)))
    x-array))

(defn translate-canvas
  [context]
  (when (= (:first-time @scope) true)
    (do
      (.translate context 0.0 150.0)
      (swap! scope assoc :first-time false))))

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
    (.setFill context javafx.scene.paint.Color/STEELBLUE)
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

(defn fill-canvas
  [context x-array y-array]
  (dotimes [x 1100]
    (.setFill context javafx.scene.paint.Color/STEELBLUE)
    (.fillOval context (int (nth x-array x)) (int (nth y-array x)) 1 1)))

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
    (fill-canvas context x-array y-array)))
