(ns redgenes.components.search.views
  (:require [reagent.core :as reagent]
            [clojure.string :as str]
            [redgenes.components.search.resultrow :as resulthandler]
            [redgenes.components.search.filters :as filters]
            [re-frame.core :as re-frame :refer [subscribe dispatch]]
            [oops.core :refer [ocall oget]]
))

;;;;TODO: Cleanse the API/state arguments being passed around from the functions here. This is legacy of an older bluegenes history and module based structure.
;;;;TODO ALSO: abstract away from IMJS.
;;;;TODO: probably abstract events to the events... :D this file is a mixture of views and handlers, but really we just want views in the view file.


(defn is-active-result? [result]
 "returns true is the result should be considered 'active' - e.g. if there is no filter at all, or if the result matches the active filter type."
   (or
     (= (:active-filter @(subscribe [:search/full-results])) (.-type result))
     (nil? (:active-filter @(subscribe [:search/full-results])))))

(defn count-total-results [state]
 "returns total number of results by summing the number of results per category. This includes any results on the server beyond the number that were returned"
 (reduce + (vals (:category (:facets state))))
 )

(defn count-current-results []
 "returns number of results currently shown, taking into account result limits nd filters"
 (count
   (remove
     (fn [result]
       (not (is-active-result? result))) (:results @(subscribe [:search/full-results])))))

(defn results-count []
 "Visual component: outputs the number of results shown."
   [:small " Displaying " (count-current-results) " of " (count-total-results @(subscribe [:search/full-results])) " results"])

(defn results-display [search-term]
 "Iterate through results and output one row per result using result-row to format. Filtered results aren't output. "
 (let [state (subscribe [:search/full-results])]
  [:div.results
   [:h4 "Results for '" @(subscribe [:search-term]) "'"  [results-count]]
   [:form
       (doall (let [active-results (filter (fn [result] (is-active-result? result)) (:results @state))]
         (for [result active-results]
           ^{:key (.-id result)}
           [resulthandler/result-row {:result result :search-term @(subscribe [:search-term])}])))
    ]
  ]))

(defn input-new-term []
  [:div
   (let [new-term (reagent/atom nil)]
    [:form.searchform {:on-submit
        (fn [e]
          (.preventDefault js/e) ;;don't submit the form, that just makes a redirect
;          (.log js/console "%c@new-term" "color:hotpink;font-weight:bold;" (clj->js @new-term) )
          (cond (some? @new-term)
            (do
;              (.log js/console "%cLet's go" "color:green;font-weight:bold;")
              (re-frame/dispatch [:search/set-search-term @new-term])
              (dispatch [:search/full-search])
              )))}
      [:input {:placeholder "Type a new search term here"
               :on-change
                (fn [e]
                  (let [input-val (oget e "target" "value")]
;                    (.log js/console "%cinput-val" "color:blue;font-weight:bold;" (clj->js input-val))
                    (cond (not (clojure.string/blank? input-val))
                      (reset! new-term input-val))))}]
      [:button "Search"]])])


 (defn search-form [search-term]
   "Visual form component which handles submit and change"
   [:div.search-fullscreen
    [:div.response
       [filters/facet-display (subscribe [:search/full-results]) nil @search-term]
       [results-display nil search-term]]])

 (defn main []
   (let [global-search-term (re-frame/subscribe [:search-term])]
   (reagent/create-class
     {:reagent-render
       (fn render []
         [search-form global-search-term]
         )
       :component-will-mount (fn [this]
           (cond (some? @global-search-term)
               (dispatch [:search/full-search])))
 })))
