(ns el-jammin.visualization.visualizer-test
  (:require [clojure.test :refer :all]
            [el-jammin.visualization.visualizer :refer :all]
            [cljfx.api :as fx])
  (:use [overtone.core]
        [overtone.inst.piano]))

(defn teardown
  []
  (def scope
    (atom {:first-time true
           :playing false})))

(defn visualizer-fixture
  [visualizer-test]
  ;(setup)
  (visualizer-test)
  (teardown))

(deftest create-scope-test
  (testing "Testing if scope created with excepted values of parameters."
    (let [s (create-scope 1 :audio-bus false 1100 300)]
      (is (= 2048 (:size s)))
      (is (= 1100 (count (:x-array s))))
      (is (= :audio-bus (:kind s))))))

(deftest reset-data-arrays-test
  (testing "Testing if reset-data-arrays function which called in 
create-scope function set arrays on expected values and if y-a and y-b 
are equal."
    (let [s (create-scope 1 :audio-bus false 1100 300)
          [y-a y-b] @(:y-arrays s)]
      (is (= false (every? #(= 0.0 %) (:x-array s))))
      (is (= true (every? #(= 150.0 %) y-a)))
      (is (= true (every? #(= 150.0 %) y-b))))))

(deftest update-scope-data-test
  (testing "Testing if update-scope-data function properly update arrays
in scope, and check if y-a and y-b no longer equal as expected."
    (let [s (create-scope 1 :audio-bus false 1100 300)]
      (update-scope-data s)
      (let [[y-a y-b] @(:y-arrays s)]
        (is (= false (= (first y-a) (first y-b))))))))
