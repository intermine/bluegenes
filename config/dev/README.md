# Example Configuration

All keys can be overridden with environment variables, such as `DATABASE_URL=http://...`
These properties are read in by [yogthos](https://github.com/yogthos/), which can read in variables in a variety of ways
See [Yogthos's config docs](https://github.com/yogthos/config#yogthosconfig) for more details.

Copy and paste the code below and save it as `config.edn`. Don't forget to tweak it to match your preferred properties!

```clojure
{
 :server-port 5000
 :logging-level :info
 ; Keep this secret! It's used to digitally sign JSON Web Tokens
 :jwt-secret "If two witches would watch two watches, which witch would watch which watch?"

 ;Pass a Google Analytics tracking ID here (or not)
 :googleAnalytics "yourTrackingIdHere"

 ;if BlueGenes is being initialised by InterMine, the IM instance will
 ;need to tell BlueGenes which URL to default to. Set below,
 ;or on the command line set "bluegenes_default_service_root"
 ;NOTE: This only applies if you are running InterMine 2.0 or later. 
 :bluegenes-default-service-root "http://beta.flymine.org/beta"

;Optional - set a mine name so it's not blank in the few seconds before the
 ;properties are loaded from intermine's web-properties endpoint.
 :bluegenes-default-mine-name "FlyMine Beta"
 }
```
