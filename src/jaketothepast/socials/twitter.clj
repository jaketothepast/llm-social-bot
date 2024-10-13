(ns jaketothepast.socials.twitter
  (:require [clojure.core.async :as a :refer [go chan <!! >!! <! >!]]
            [clojure.java.shell :as sh]
            [ring.adapter.jetty :as jetty]
            [ring.middleware.params :refer [wrap-params]]
            [clojure.edn :as edn]
            [clojure.java.io :as io])
  (:import
   (com.twitter.clientlib.api TwitterApi)
   (com.twitter.clientlib.auth TwitterOAuth20Service)
   (com.github.scribejava.core.pkce PKCE PKCECodeChallengeMethod)
   (com.github.scribejava.core.model OAuth2AccessToken)
   (com.twitter.clientlib.model TweetCreateRequest TweetCreateResponse)
   (com.twitter.clientlib TwitterCredentialsOAuth2)
   (java.net URLEncoder)))

  ;;   TwitterCredentialsOAuth2 credentials = new TwitterCredentialsOAuth2(System.getenv("TWITTER_OAUTH2_CLIENT_ID"),
  ;;       System.getenv("TWITTER_OAUTH2_CLIENT_SECRET"),
  ;;       System.getenv("TWITTER_OAUTH2_ACCESS_TOKEN"),
  ;;       System.getenv("TWITTER_OAUTH2_REFRESH_TOKEN"));

  ;;   OAuth2AccessToken accessToken = getAccessToken(credentials);
  ;;   if (accessToken == null) {
  ;;     return;
  ;;   }

  ;;   // Setting the access & refresh tokens into TwitterCredentialsOAuth2
  ;;   credentials.setTwitterOauth2AccessToken(accessToken.getAccessToken());
  ;;   credentials.setTwitterOauth2RefreshToken(accessToken.getRefreshToken());
  ;;   callApi(credentials);
  ;; }

  ;; public static OAuth2AccessToken getAccessToken(TwitterCredentialsOAuth2 credentials) {
  ;;   TwitterOAuth20Service service = new TwitterOAuth20Service(
  ;;       credentials.getTwitterOauth2ClientId(),
  ;;       credentials.getTwitterOAuth2ClientSecret(),
  ;;       "http://twitter.com",
  ;;       "offline.access tweet.read users.read");

  ;;   OAuth2AccessToken accessToken = null;
  ;;   try {
  ;;     final Scanner in = new Scanner(System.in, "UTF-8");
  ;;     System.out.println("Fetching the Authorization URL...");

  ;;     final String secretState = "state";
  ;;     PKCE pkce = new PKCE();
  ;;     pkce.setCodeChallenge("challenge");
  ;;     pkce.setCodeChallengeMethod(PKCECodeChallengeMethod.PLAIN);
  ;;     pkce.setCodeVerifier("challenge");
  ;;     String authorizationUrl = service.getAuthorizationUrl(pkce, secretState);

  ;;     System.out.println("Go to the Authorization URL and authorize your App:\n" +
  ;;         authorizationUrl + "\nAfter that paste the authorization code here\n>>");
  ;;     final String code = in.nextLine();
  ;;     System.out.println("\nTrading the Authorization Code for an Access Token...");
  ;;     accessToken = service.getAccessToken(pkce, code);

  ;;     System.out.println("Access token: " + accessToken.getAccessToken());
  ;;     System.out.println("Refresh token: " + accessToken.getRefreshToken());
  ;;   } catch (Exception e) {
  ;;     System.err.println("Error while getting the access token:\n " + e);
  ;;     e.printStackTrace();
  ;;   }
  ;;   return accessToken;
  ;; }

(defonce twitter-env (:twitter (edn/read (java.io.PushbackReader. (io/reader "environment.edn")))))

(def creds
  (let [{:keys [client-id client-secret]} twitter-env]
    (doto (TwitterCredentialsOAuth2.
           client-id
           client-secret
           (System/getenv "TWITTER_ACCESS_TOKEN")
           (System/getenv "TWITTER_REFRESH_TOKEN")
           true)))) ;; Is autorefresh token == true

(def service (TwitterOAuth20Service.
              (.getTwitterOauth2ClientId creds)
              (.getTwitterOAuth2ClientSecret creds)
              "http://localhost:3000/login/success"
              "offline.access tweet.read tweet.write users.read"))

(def twitter-state (atom {:server nil
                          :chan nil
                          :creds nil
                          :api nil
                          :api-keys {:api-key nil :api-key-secret nil :access-token nil :refresh-token nil}
                          :ds nil}))

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
        pkce (doto (PKCE.)
               (.setCodeChallenge "challenge")
               (.setCodeChallengeMethod PKCECodeChallengeMethod/PLAIN)
               (.setCodeVerifier "challenge"))
        url (.getAuthorizationUrl service pkce state)]
    (swap! twitter-state assoc :chan (chan)) ;; Add our channel to the twitter-state
    (sh/sh "open" url)
    (let [server (start-embedded-auth-server handler-twitter-code)
          code (<!! (:chan @twitter-state))]
      (.stop server)
      (.getAccessToken service pkce code))))

(defn authorize-app []
  (let [oauth2-access-token (get-oauth2-credentials)
        access-token (.getAccessToken oauth2-access-token)
        refresh-token (.getRefreshToken oauth2-access-token)]
    (println oauth2-access-token)
    (swap! twitter-state assoc :api
           (TwitterApi. (doto creds
                          (.setTwitterOauth2AccessToken access-token)
                          (.setTwitterOauth2RefreshToken refresh-token))))))

(defn make-tweet-request
  "Create and format a tweet request given the option map"
  [{:keys [text]}]
  (let [req (TweetCreateRequest.)]
    (cond-> req
      (not (nil? text)) (.setText text))
    req))

(defn tweet [tweet-options]
  (let [tweet-request (make-tweet-request tweet-options)
        api (or (:api @twitter-state) (:api (authorize-app)))]
    (.. api tweets (createTweet tweet-request) execute)))

(comment
  (twitter-creds-oauth2)

  ;; TODO: post the access/refresh token to file, so that we can read them on startup
  (get-oauth2-credentials)

  (authorize-app)

  (tweet {:text "hello, world!"})

  twitter-state)
