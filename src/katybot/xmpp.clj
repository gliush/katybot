(ns katybot.xmpp
  (:require [clojure.string :as str]
            [katybot.xmpp_clj :as xmpp-api])
  (:use katybot.core
        katybot.utils))

(defonce state (atom {}))

(defn add-state-field [key value]
  (swap! state conj {key value}))

(defn rm-state-field [key]
  (swap! state dissoc key))

;; Important stuff
(defn handle-message [message]
  (let [body (:body message)
        from-user (:from-name message)]
    (case body
        nil nil
        (str "Hi " from-user ", you sent me '" message "'"))))

(defn stop-bot []
  (let [conn (:conn @state)]
    (xmpp-api/stop-bot conn)
    (rm-state-field :conn)))


;(defn reload [connect-info]
;  (xmpp-api/stop-bot my-bot)
;  (def my-bot (xmpp-api/start-bot connect-info reload-helper)))

(defn type-from-xmpp [type]
  (case type
    :chat  :text
    "EnterMessage" :join
    "LeaveMessage" :leave
    type))

(defn- item-from-xmpp [item]
  (-> item
    (change-keys
      :body       :text
      :from-name  :user-id)
    (update-in [:type] type-from-xmpp)))

(defn- msg-callback [robot me-id item]
  (let [robot2 (assoc robot :recepient (:user-id item))]
    (when (not= me-id (:user-id item))
        (consider robot2 item))))


(defn- msg-callback-raw [robot me-id msg]
  (let [body (:body msg)]
    (case body
        nil nil
        (msg-callback robot me-id (item-from-xmpp msg)))))

(defn +xmpp-receptor [robot conn-info]
  (assoc robot
    :receptor ::xmpp-receptor
    ::conn-info conn-info))

(defn start-bot [connect-info handler]
  (add-state-field :conn
    (xmpp-api/start-bot connect-info handler)))

(defmethod listen ::xmpp-receptor [{conn-info ::conn-info  :as  robot}]
  (let [me (:username conn-info)]
     ;(xmpp-api/start-bot conn-info (var handle-message))
     ;(xmpp-api/start-bot conn-info (partial msg-callback-raw robot me))
     (start-bot conn-info (partial msg-callback-raw robot me))
  )
  :listening)

(defn reply [to body]
  (xmpp-api/reply to body (:conn @state)))

(defmethod say ::xmpp-receptor [robot msg]
  (reply (:recepient robot) (apply str msg)))

(defmethod say-img ::xmpp-receptor [robot url]
  (print [url "#.png"]))

(defmethod user ::xmpp-receptor [{conn ::conn} user-id]
  (xmpp-api/user-info conn user-id))

(defmethod users ::xmpp-receptor [{conn ::conn, room ::room}]
  (let [room (xmpp-api/room-info conn room)]
    (into {}
      (for [u (:users room)]
        [(:id u) u]))))

(defmethod shutdown ::xmpp-receptor [{conn ::conn, room ::room}]
  (stop-bot)
  )

