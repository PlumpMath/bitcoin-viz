(ns bitcoin-viz.core
  (:require [quil.core :as q]
            [quil.middleware :as m]
            [clojure.core.async :refer [chan map> <!! sliding-buffer go-loop >!]]
            [midje.sweet :refer [fact]]
           [clojure.data.json :as json]))

(def price-chan (sliding-buffer 50))

(def coinbase-api "https://coinbase.com")

(def price-endpoint (str coinbase-api "/api/v1/prices/spot_rate"))

(defn get-bitcoin-price []
   (json/read-str (slurp price-endpoint) :key-fn keyword))


(def max-price 800)
(def min-price 400)

(defn scaler [min-value max-value min-range max-range]
  (let [offset (- 0 (- min-value min-range))
        scaler (/ max-range (- (+ max-value offset) (+ min-value offset)))]
   [scaler offset]))
 
(defn start-grabber [c]
  (go-loop []
    (>! c (:amount get-bitcoin-price))))

(defn start-x-chan [c height]
  (let [scale-factor (scaler 0 height min-price max-price)
        scaled (map (partial scale scale-factor))] 
  (start-grabber scaled)))

(fact "scale-factor should be the number that you need
      to multiply a field by so all number in it fit in a range"
      (scaler 5 15 0 5) => [(/ 1 2) -5]
      (scaler 10 20 10 5) => [(/ 1 2) 0]
      (scaler 0 10 0 5)=> [(/ 1 2) 0])

(defn scale [[factor offset] value]
  (+ offset  (* factor value)))

(defn update-fn [state]
  (update-in state [:y] #(<!! (:c state)) )
  (update-in state [:x] #(mod (inc %) (q/width))))

(defn draw [state]
  (q/background 255)
  (q/ellipse (:x state) (:y state) (:r state) (:r state)))

(defn setup []
  (let [c (chan)]
    (start-x-chan  c (q/height))
  {:x 200 :y 0 :r 20 :c c }))

(q/defsketch bitcoin-viz
  :size [400 400]
  :setup setup 
  :update update-fn
  :draw draw
  :middleware [m/fun-mode]) 
