(ns examples.hackernews.core
  (:require [sablono.core :as html :refer-macros [html]]
            [om.core :as om]
            [twigs.core :as tw]))

;; WIP hackernews clone!!!

(enable-console-print!)

(def app-state
  (atom {:page 0}))

(def hn-ref (tw/ref "https://hacker-news.firebaseio.com/v0"))

(def per-page 10)

(def top-stories-q
  (tw/query (conj hn-ref :topstories)
            {:order-by-key true
             :start-at "0"
             :limit-to-first per-page}))

(tw/on! top-stories-q "value"
  (fn [[_ ss]] (swap! app-state assoc :stories ss)))

;; state should probably go in global app-state
(defn story [[k item] owner]
  (reify
    om/IInitState
    (init-state [_]
      {:q (tw/query (conj hn-ref :item (str @item)))})
    om/IWillMount
    (will-mount [_]
      (tw/on! (om/get-state owner :q) "value"
        (fn [[_ ss]] (om/set-state! owner :ss ss))))
    om/IWillUnmount
    (will-unmount [_]
      (tw/off! (om/get-state owner :q)))
    om/IRenderState
    (render-state [_ {{:keys [score title by]} :ss}]
      (if title
        (html [:.m1.p1.border.rounded
               [:span.h5.px1.orange @score]
               [:span.h5.px1 @title]
               [:span.h6.px1.italic.gray (str "(" @by ")")]])))))

(defn handle-page [data f]
  (fn [e]
    (-> e .-target .blur)
    (om/transact! data :page
      #(let [np (f %)] (if (> np 0) np 0)))))

(defn app [{:keys [page stories] :as data} owner]
  (reify
    om/IWillReceiveProps
    (will-receive-props [_ {np :page}]
      (let [p (om/get-props owner :page)]
        (when (not= p np)
          (reset! top-stories-q
                  {:order-by-key true
                   :start-at (str (* np per-page))
                   :limit-to-first per-page}))))
    om/IRender
    (render [_]
      (html
        [:.p2.container
         [:p.caps.center.m2 "Hackernews Example"]
         [:.p1.border-top.border-black
          (om/build-all story stories {:key-fn first})]
         [:.bg-orange.rounded
          [:.clearfix
           [:button.left.button-narrow.button-transparent
            {:onClick (handle-page data dec)}
            "Previous"]
           [:button.right.button-narrow.button-transparent
            {:onClick (handle-page data inc)}
            "Next"]
           [:.overflow-hidden.sm-show.center
            [:a.regular.button.button-narrow.button-transparent
             {:href "#!"}
             (str "Page " (inc page))]]]]]))))

(om/root app app-state
  {:target (.getElementById js/document "app")})
