# Example Configuration

All keys can be overridden with environment variables, such as `DATABASE_URL=http://...`

```clojure
{
 :server-port 5000
 :logging-level :info
 ; The InterMine server associated with this bluegenes client
 :im-service "http://beta.flymine.org/beta"
 ; Keep this secret! It's used to digitally sign JSON Web Tokens
 :jwt-secret "If two witches would watch two watches, which witch would watch which watch?"
 ; Database
 :database-url "postgres://username:password@localhost:5432/bluegenes-dev"
 ; If URL isn't present then use:
 :db-host "localhost"
 :db-name "bluegenes-dev"
 :db-username "username"
 :db-password "password"
 :googleAnalytics "yourTrackingIdHere"
 }
```