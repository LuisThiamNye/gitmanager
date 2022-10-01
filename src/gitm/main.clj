(ns gitm.main
  (:require
   [babashka.fs :as fs]
   [clojure.java.shell :refer [sh]]
   [clj-jgit.querying :as gitq]
   [clj-jgit.porcelain :as gitp]))

(defn -main [])

(defn remote-status [g]
  (first (gitq/rev-list g)))

(defn branch-synced? [dir branch-name remote-name]
  (let [lid (:out (sh "git" "rev-parse" branch-name :dir dir))
        rid (:out (sh "git" "rev-parse" remote-name :dir dir))]
    (= lid rid)))

(defn git-dir [g]
  (str (.getDirectory (.getRepository g))))

(defn owned-remote [g]
  (some (fn [[n uri :as r]]
          (when (or (= "origin" n)
                    (#{"LuisThiamNye" "CrypticButter"}
                     (second (re-matches #"/(\w+)/.+" (.getPath (first uri))))))
            r))
        (gitp/git-remote-list g)))
;; (owned-remote (gitp/load-repo "/Volumes/House/Programming Projects/parser"))
(defn branch-data [g]
  (let [rem (first (owned-remote g))]
    (into []
          (map (fn [branch-name]
                 (let [rb (str rem "/" branch-name)]
                   {:synced? (branch-synced? (git-dir g) branch-name rb)
                    :remote-branch rb
                    :branch branch-name})))
          (gitp/git-branch-list g))))

(comment
  (def repo-group-dirs
    ["/Volumes/House/prg"
     "/Volumes/House/Programming Projects"
     "/Volumes/House/lib"])

  (defn repo-dirs []
    (into ["/Volumes/House/Programming Projects/fin-man/dev/src/luisthiamnye/personal"
           "/Users/luis/.config/clj-kondo"
           "/Users/luis/.clojure"
           "/Users/luis/.lsp"
           "/Users/luis/.spacemacs.d"
           "/Users/luis/.spacemacs.d/packages/zprint-mode"]
          (comp (mapcat fs/list-dir)
                (filter fs/directory?)
                (map str))
          repo-group-dirs))

  (def repo-data
    (reduce (fn [acc x]
              (cond
                (not (:status x))
                (update acc :nogit conj x)
                (empty? (:remotes x))
                (update acc :noremote conj x)
                (some seq (vals (:status x)))
                (update acc :pending conj x)
                (some false? (map :synced? (:branches x)))
                (update acc :unsync conj x)
                :else
                (update acc :synced conj x)))
            {:nogit []
             :noremote []
             :pending []
             :unsync []
             :synced []}
            (eduction
             (map (fn [s]
                    (if-let [_r (gitp/discover-repo s)]
                      (let [g (gitp/load-repo s)]
                        {:path s
                         :current-branch (gitp/git-branch-current g)
                         :status (gitp/git-status g)
                         :remotes (gitp/git-remote-list g)
                         :branches (branch-data g)})
                      {:path s})))
             (repo-dirs))))
  #!
  )

(comment

  #'repo-data

  #!
  )
