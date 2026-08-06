(ns bluegenes.ws.feedback
  "Sends homepage feedback form submissions by email, using authenticated SMTP.
  InterMine's own built-in feedback mailer only supports unauthenticated SMTP,
  which isn't available to us, so we handle this ourselves instead."
  (:require [compojure.core :refer [defroutes POST]]
            [ring.util.http-response :as response]
            [postal.core :as postal]
            [clojure.string :as str]
            [config.core :refer [env]]))

;; Overridable via FEEDBACK_DESTINATION env var, e.g. for testing with a
;; personal address instead of the real one.
(def default-feedback-destination "crr@ecrin.org")

(defn smtp-config []
  {:host (:smtp-host env)
   :port (let [port (:smtp-port env)]
           (cond
             (number? port) (int port)
             (not (str/blank? port)) (Integer/parseInt port)))
   :user (:smtp-user env)
   :pass (:smtp-password env)
   :tls true})

(defn send-feedback [{:keys [email feedback]}]
  (let [{:keys [host port user pass] :as smtp} (smtp-config)]
    (cond
      (str/blank? feedback)
      (response/bad-request {:error "Feedback text can't be blank."})

      (or (str/blank? host) (nil? port) (str/blank? user) (str/blank? pass))
      (do (println "WARNING: SMTP_HOST/SMTP_PORT/SMTP_USER/SMTP_PASSWORD are not fully configured; feedback email not sent.")
          (response/internal-server-error {:error "Email service is not configured."}))

      :else
      (let [feedback-destination (or (not-empty (:feedback-destination env)) default-feedback-destination)
            body (cond-> feedback
                   (not (str/blank? email))
                   (str "\n\n---\nSubmitted by: " email))
            message (cond-> {:from user
                             :to feedback-destination
                             :subject "Feedback crMDR"
                             :body body}
                     (not (str/blank? email)) (assoc :reply-to email))
            result (postal/send-message smtp message)]
        (if (= :SUCCESS (:error result))
          (response/ok {:status "Success"})
          (do (println "SMTP error:" result)
              (response/internal-server-error {:error "Failed to send feedback email."})))))))

(defroutes routes
  (POST "/send" {:keys [params]} (send-feedback params)))
