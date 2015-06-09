(ns examples.hackernews.core
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [cljs.core.async :refer [chan sub unsub unsub-all <!]]
            [sablono.core :as html :refer-macros [html]]
            [twigs.core :as tw]
            [om.core :as om]))

(enable-console-print!)

(def hn-url "https://hacker-news.firebaseio.com/v0")

(defn story [[_ ss] owner]
  (reify
    om/IWillMount
    (will-mount [_]
      (let [q (-> hn-url tw/url->ref (conj :item (str @ss)) tw/query)
            ch (chan)]
        (om/set-state! owner :q q)
        (sub q :value ch)
        (go-loop []
          (let [[_ ss] (<! ch)]
            (om/set-state! owner :story ss)
            (recur)))))
    om/IWillUnmount
    (will-unmount [_]
      (let [q (om/get-state owner :q)]
        (unsub-all q)))
    om/IRenderState
    (render-state [_ {{:keys [title url score] :as story} :story}]
      (if story
        (html [:p.h4 @title])))))

(def separator
  [:span.gray.h2
   {:style {:position "relative"
            :vertical-align "middle"}} "/"])

(defn page->top-stories-q [p n]
  (-> hn-url tw/url->ref (conj :topstories)
      (tw/query {:order-by-key true
                 :start-at (str (* p n))
                 :limit-to-first n})))

(defn app [data owner]
  (reify
    om/IInitState
    (init-state [_]
      {:page 0
       :q (page->top-stories-q 0 30)
       :ch (chan)})
    om/IWillMount
    (will-mount [_]
      (let [q (om/get-state owner :q)
            ch (om/get-state owner :ch)]
        (go-loop []
          (let [[_ ss] (<! ch)]
            (om/set-state! owner :stories ss)
            (recur)))
        (sub q :value ch)))
    om/IWillUnmount
    (will-unmount [_]
      (let [q (om/get-state owner :q)]
        (unsub-all q)))
    om/IDidUpdate
    (did-update [_ _ {prev-page :page prev-q :q ch :ch}]
      (let [page (om/get-state owner :page)]
        (when-not (= page prev-page)
          (unsub-all prev-q)
          (let [q (page->top-stories-q page 30)]
            (om/set-state! owner :q q)
            (sub q :value ch)))))
    om/IRenderState
    (render-state [_ {:keys [page stories]}]
      (html
        [:.app-wrapper.container
         [:p.caps.center.m2 "HACKERNEWS EXAMPLE"]
         [:.p2.bg-darken-1.border.rounded
          [:.mxn1
           [:a.button.button-narrow.button-transparent {:href "#!"} "HN"]
           ;separator [:a.button.button-narrow.button-transparent {:href "#!"} "Hot Dogs"]
           ]]
         [:.p2 (om/build-all story stories {:key-fn first})]
         [:.p2.bg-darken-1.border.rounded
          [:.clearfix
           [:a.left.button.button-narrow.button-transparent
            {:href "#!"
             :onClick (fn [e]
                        (om/update-state! owner :page
                          (fn [p] (if (zero? p) 0 (dec p)))))}
            "Previous"]
           [:a.right.button.button-narrow.button-transparent
            {:href "#!"
             :onClick (fn [e]
                        (om/update-state! owner :page
                          (fn [p] (inc p))))}
             "Next"]
           [:.overflow-hidden.sm-show.center
            [:a.regular.button.button-narrow.button-transparent {:href "#!"} (str "Page " (inc page))]]
           ]]]))))

(def app-state
  (atom {}))

(om/root app app-state
  {:target (.getElementById js/document "app")})
