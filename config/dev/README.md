# Example Configuration

All keys can be overridden with environment variables, such as `DATABASE_URL=http://...`

```clojure
{
 :server-port 5000
 :logging-level :info
 ; Keep this secret! It's used to digitally sign JSON Web Tokens
 :jwt-secret "If two witches would watch two watches, which witch would watch which watch?"
 ; Database
 :database-url "postgres://username:password@localhost:5432/bluegenes-dev"
 ; If URL isn't present then use:
 :db-host "localhost"
 :db-name "bluegenes-dev"
 :db-username "username"
 :db-password "password"

 ;Pass a Google Analytics tracking ID here (or not)
 :googleAnalytics "yourTrackingIdHere"

 ;if BlueGenes is being initialised by InterMine, the IM instance will
 ;need to tell BlueGenes which URL to default to. Set below,
 ;or on the command line set "bluegenes_default_service_root"
 :bluegenes-default-service-root "http://beta.flymine.org/beta"
 ;Optional - set a mine name so it's not blank in the few seconds before the
 ;properties are loaded from intermine's web-properties endpoint.
 :bluegenes-default-mine-name "FlyMine Beta"
 }
```
