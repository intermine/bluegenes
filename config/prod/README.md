# Example Configuration

```clojure
{:server-port   5000
 :logging-level :info
 :signature     "This is used to sign JSON Web Tokens"
 :database      {:dbtype   "postgresql"
                 :dbname   "bluegenes-prod"
                 :host     "localhost"
                 :user     "postgres"
                 :port     5432
                 :password "something-other-than-postgres"}
 :migrations    {:store                :database
                 :migration-dir        "migrations/"
                 :migration-table-name "migrations"
                 :db                   {:classname "org.postgresql.Driver"}}
 :hikari        {:auto-commit        true
                 :read-only          false
                 :connection-timeout 30000
                 :validation-timeout 5000
                 :idle-timeout       600000
                 :max-lifetime       1800000
                 :minimum-idle       10
                 :maximum-pool-size  10
                 :pool-name          "db-pool"
                 :database-name      "bluegenes-dev"
                 :register-mbeans    false}}
```