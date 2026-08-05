(ns bluegenes.ws.feedback
  "Sends homepage feedback form submissions by email, using the Resend API.
  InterMine's own built-in feedback mailer only supports unauthenticated SMTP,
  which isn't available to us, so we handle this ourselves instead."
  (:require [compojure.core :refer [defroutes POST]]
            [ring.util.http-response :as response]
            [clj-http.client :as client]
            [clojure.string :as str]
            [config.core :refer [env]]))

;; Overridable via FEEDBACK_DESTINATION env var, e.g. for testing with a
;; personal address before a sending domain is verified with Resend (which
;; restricts sending to only the account owner's own address until then).
(def default-feedback-destination "crr@ecrin.org")
;; Resend's shared sending address; usable without verifying our own domain.
(def sender-address "onboarding@resend.dev")

(defn send-feedback [{:keys [email feedback]}]
  (cond
    (str/blank? feedback)
    (response/bad-request {:error "Feedback text can't be blank."})

    (str/blank? (:resend-api-key env))
    (do (println "WARNING: RESEND_API_KEY is not configured; feedback email not sent.")
        (response/internal-server-error {:error "Email service is not configured."}))

    :else
    (let [feedback-destination (or (not-empty (:feedback-destination env)) default-feedback-destination)
          params (cond-> {:from sender-address
                          :to [feedback-destination]
                          :subject "Feedback crMDR"
                          :text (cond-> feedback
                                  (not (str/blank? email))
                                  (str "\n\n---\nSubmitted by: " email))}
                  (not (str/blank? email)) (assoc :reply_to email))
          res (client/post
               "https://api.resend.com/emails"
               {:headers {"Authorization" (str "Bearer " (:resend-api-key env))}
                :content-type :json
                :as :json
                :throw-exceptions false
                :form-params params})]
      (if (client/success? res)
        (response/ok {:status "Success"})
        (do (println "Resend API error:" (:status res) (:body res))
            (response/internal-server-error {:error "Failed to send feedback email."}))))))

(defroutes routes
  (POST "/send" {:keys [params]} (send-feedback params)))
