(ns jaketothepast.socials.twitter
  (:require [clojure.core.async :as a :refer [go chan <!! >!! <! >!]]
            [clojure.java.shell :as sh]
            [ring.adapter.jetty :as jetty]
            [jaketothepast.commit-to-social :as cts]
            [ring.middleware.params :refer [wrap-params]])
  (:import
   (com.twitter.clientlib.api TwitterApi)
   (com.twitter.clientlib.auth TwitterOAuth20Service)
   (com.github.scribejava.core.pkce PKCE PKCECodeChallengeMethod)
   (com.github.scribejava.core.model OAuth2AccessToken)
   (java.net URLEncoder)))

(def twitter-state (atom {:server nil
                          :chan nil
                          :creds nil
                          :api nil
                          :api-keys {:api-key nil :api-key-secret nil :access-token nil :refresh-token nil}
                          :ds nil}))

(def service (TwitterOAuth20Service.
              (.getTwitterOauth2ClientId (:socials/twitter cts/system))
              (.getTwitterOAuth2ClientSecret (:socials/twitter cts/system))
              "http://localhost:3000/login/success"
              "offline.access tweet.read tweet.write users.read"))

(defn handler-twitter-code [req]
  (let [server-chan (:chan @twitter-state)
        {:strs [code]} (:params req)]
    (>!! server-chan code)
    {:status 200
     :headers {"Content-Type" "text/html"}
     :body "Hello world"}))

(defn start-embedded-auth-server [handler]
  (let [app (wrap-params handler)]
    (jetty/run-jetty app {:port 3000 :join? false})))

(defn get-oauth2-credentials []
  (let [state "state"
        pkce (PKCE.)
        url (do
              (.setCodeChallenge pkce "challenge")
              (.setCodeChallengeMethod pkce PKCECodeChallengeMethod/PLAIN)
              (.setCodeVerifier pkce "challenge")
              (.getAuthorizationUrl service pkce state))]
    (swap! twitter-state assoc :chan (chan)) ;; Add our channel to the twitter-state
    (sh/sh "open" url)
    (let [server (start-embedded-auth-server handler-twitter-code)
          code (<!! (:chan @twitter-state))]
      (.stop server)
      (.getAccessToken service pkce code))))

(defn authorize-app []
  (let [creds (get-oauth2-credentials)]
    (swap! twitter-state assoc :api
           (TwitterApi. (doto
                         (:socials/twitter cts/system)
                          (.getAccessToken creds)
                          (.getRefreshToken creds))))))

(comment
  (twitter-creds-oauth2)

  (authorize-app)

  (:socials/twitter cts/system))
