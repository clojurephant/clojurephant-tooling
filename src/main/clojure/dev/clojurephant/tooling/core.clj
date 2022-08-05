(ns dev.clojurephant.tooling.core
  (:require [clojure.string :as string]
            [clojure.java.io :as io]
            [dev.clojurephant.tooling.impl.event :as event])
  (:import [org.gradle.tooling GradleConnector ResultHandler]
           [org.gradle.tooling.events ProgressListener]))

(defn connect [dir]
  (-> (GradleConnector/newConnector)
      (.forProjectDirectory (io/file dir))
      (.connect)))

(defn tap-listener []
  (let [cache (atom {})]
    (reify ProgressListener
      (statusChanged [this event]
        (println (event/event event cache))))))

(defn collecting-listener [db]
  (let [cache (atom {})]
    (reify ProgressListener
      (statusChanged [this event]
        (swap! db conj (event/event event cache))))))

(defn handler [done db]
  (reify ResultHandler
    (onComplete [this result]
      (deliver done {:result :success
                     :value result
                     :events @db}))
    (onFailure [this failure]
      (deliver done {:result :failed
                     :error failure
                     :events @db}))))

(defn build [con & tasks]
  (let [cancel-source (GradleConnector/newCancellationTokenSource)
        done (promise)
        db (atom [])]
    (-> (.newBuild con)
        (.forTasks (into-array String tasks))
        (.setJavaHome (io/file "/usr/lib/jvm/java-18-openjdk-amd64"))
        (.setStandardOutput System/out)
        (.setStandardError System/err)
        #_(.addProgressListener (tap-listener))
        (.addProgressListener (collecting-listener db))
        (.withCancellationToken (.token cancel-source))
        (.run (handler done db)))
    {:result done
     :cancel cancel-source}))

(defn wait [run]
  @(:result run))

(defn cancel [run]
  (.cancel (:cancel run))
  run)

(defn task-results
  ([run]
   (task-results run any?))
  ([run state-filter]
   (let [result (wait run)
         task-finish? (fn [e]
                        (and (= :task (get-in e [:operation :descriptor]))
                             (= :finish (:state e))))
         simplify (fn [t]
                    (merge 
                     {:task (-> t :operation :name)}
                     (:result t)))]
     (->> (:events result)
          (filter task-finish?)
          (map simplify)
          (filter (fn [t]
                    (state-filter (:result t))))))))
