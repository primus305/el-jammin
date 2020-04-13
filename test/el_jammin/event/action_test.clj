(ns el-jammin.event.action-test
  (:require [clojure.test :refer :all]
            [el-jammin.event.action :refer :all]
            [cljfx.api :as fx])
  (:use [overtone.core]
        [overtone.inst.piano]))

(defn teardown
  []
  (set-default-state))

(defn action-fixture
  [action-test]
  ;(setup)
  (action-test)
  (teardown))

(use-fixtures :once action-fixture)

(deftest on-rec-stop-test
  (testing "Testing when on-rec-stop function works expected if event 
::rec-stop occur."
    (on-rec-stop)
    (let [state (get-state)]
      (is (= true (:stop-rec-disabled @state)))
      (is (= false (:rec-disabled @state)))
      (is (= "You stopped recording." (:poruka @state))))))

(deftest loop-sample-test-false
  (testing "Testing if message is expected when argument with wrong 
extension passed to loop-sample function."
    (loop-sample "testing-sample.jpg" {:event/type ::loop})
    (let [state (get-state)]
      (is (= "You must select sample." (:poruka @state))))))

(deftest on-looper-test
  (testing "Testing if message is expected when did not passed end 
of the loop to on-looper function."
    (on-looper {:event/type ::looper})
    (let [state (get-state)]
      (is (= "You must select end of the loop." (:poruka @state))))))

(deftest on-new-looper-test
  (testing "Testing when allowed to add new loop line in looper."
    (on-new-looper-true)
    (let [state (get-state)]
      (is (= false (:add-new-loop @state)))
      (is (= 0.0 (:loop-end @state)))
      (is (= '() (:loop-line @state))))))

(deftest on-new-looper-test-false
  (testing "Testing if message is expected when not allowed to add 
new loop line in looper because there is already one line which is 
not looping in it."
    (on-new-looper-capacity-true)
    (let [state (get-state)]
      (is (= "You already have line which is not looped." (:poruka @state))))))

(deftest check-message-restart-test
  (testing "Testing if message is expected when looper can not empty 
because already it is."
    (check-if-restart-message)
    (let [state (get-state)]
      (is (= "Your looper is already empty." (:poruka @state))))))

(deftest on-set-instrument-test
  (testing "Testing if changed live instrument on selected as expected, 
not loop instrument."
    (on-set-instrument {:event/type ::set-instrument :option "Guitar"})
    (let [state (get-state)]
      (is (= "Guitar" (:instrument @state))))))

(deftest enable-chord-name-test-true
  (testing "Testing if enable-chord-name function when ::set-option event
occur change option as expected."
    (enable-chord-name {:event/type ::set-option :option "Note"})
    (let [state (get-state)]
      (is (= "Note" (:option @state))))))

(deftest play-selected-item-test
  (testing "Testing if message is expected when no one instrument is 
selected."
    (play-selected-item)
    (let [state (get-state)]
      (is (= "You must select instrument." (:poruka @state))))))
