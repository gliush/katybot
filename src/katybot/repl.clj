(ns katybot.repl
  (:use katybot.utils)
  (:require [clojure.string :as str]))

(defn reload-robot []
  (doseq [module ["utils" "core" "brain" "campfire" "xmpp" "console" "atom_memory" "file_memory"]]
    (fyi "Loading " module)
    (load module)
    (use (-> (str "katybot." module) (str/replace "_" "-") symbol))))

(defn- load-dir [dir]
  (fyi "Loading " dir ":")
  (doseq [f (->> (file-seq (clojure.java.io/as-file dir))
                 (filter  #(-> (.getName %) (.endsWith ".clj")))
                 (sort-by #(.getParent %)))]
    (fyi "  " f)
    (load-file (.getCanonicalPath f))))

(defn reload-reflexes []
  (load-dir "reflexes"))

(defn reload-all []
  (fyi "Loading repl")
  (load "repl")
  (use 'katybot.repl))

(reload-robot)
(reload-reflexes)

(defn listen-console []
  (-> (new-robot)
    (+file-memory "robot.memory")
    (+console-receptor)
    (+global-brain ["/" "Katy" "Kate"])
    (listen)))

(defn listen-campfire []
  (-> (new-robot)
    (+file-memory "robot.memory")
    (+campfire-receptor (env "KATYBOT_CAMPFIRE_ACCOUNT")
                        (env "KATYBOT_CAMPFIRE_ROOM")
                        (env "KATYBOT_CAMPFIRE_TOKEN"))
    (+global-brain [(or (env "KATYBOT_CAMPFIRE_ALIASES") "/|Kat[ye]")])
    (listen)))

(defn listen-xmpp []
  (-> (new-robot)
    (+file-memory "robot.memory")
    (+xmpp-receptor {:username (env "KATYBOT_XMPP_ACCOUNT"  "katybot@jabber.ru")
                     :password (env "KATYBOT_XMPP_PASSWORD" "FJuJbmwkeU"   )
                     :host     (env "KATYBOT_XMPP_HOST"     "jabber.ru")
                     :domain   (env "KATYBOT_XMPP_DOMAIN"   "jabber.ru")})
    (+global-brain [(or (env "KATYBOT_XMPP_ALIASES") "=|/|Kat[ye]")])
    (listen)))
