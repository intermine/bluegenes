# Bluegenes Tool API Spec


#### Assumptions:

* js Dependencies are bundled into the app (requirejs, browserify, webpack)

Our justification is that sharing dependencies between third party tools introduces levels of complexity that are expensive (time consuming) and can be challenging to developers. If we find that third party apps are truly to large then we'll reconsider a more complicated approach.

## App structure:

```
.
+-- myapp/
|   +-- dist/
|      +-- style.css (optional)
|      +-- bundle.js (required)
|   +-- src/
|      +-- style.less (optional, could be some other preprocessor too.)
|      +-- index.js (optional but recommended)
|   +-- config.json (required)
|   +-- package.json (required)
|   +-- demo.html (optional)
|   +-- preview.png (optional)
```

#### config.json
```json
{
  "accepts": ["id", "ids", "list", "lists", "records", "rows"],
  "classes": ["Gene", "Protein", "*"],
  "columnMapping" : {"Protein" : {"id" : "primaryAccession"}},
  "files" : {
    "css" : "dist/style.css",
    "js" : "dist/bundle.js"
  },
  "toolName" : "Protein Features"
}
```
**Accepts: possible values **

* id: a single database id  
* ids: multiple database ids  
* list: a single list name  
* lists: multiple list names  
* records: a raw result from POSTing to /query/results with format "jsonobjects"  
* rows: a raw result from POSTing to /query/results with format "json"  

Plurality (i.e. id vs ids) will help to determine which context a tool can appear (report page, list analysis page)

Records and Rows can also be accepted. The advantage here is that BlueGenes has likely already run and stored the query results in memory, so the tool can skip a redundant call to the server.

**columnMapping** is an important way to specify (or override) which columns should be passed to the tool by BlueGenes. As an example, for a gene tool, you might want to pass a symbol, OR a primaryIdentifier, or even secondaryIdentifier - and this might change depending in the InterMine that is fuelling the BlueGenes. Set a default likely value here, and in the future individual bluegenes administrators can override it if needed.

**classes** default to `*` if this tool isn't class / objectType specific. otherwise your tool might be specific to a certain class, e.g. a gene displayer.

**files** - one file each for css and js, please. This should be the file bundled/built with all dependencies except/ imjs if needed. CSS is optional if the tool has no styles.

**toolName** what would you want to see as a header for this tool? e.g. ProtVista might be called "Protein Features".

##### el

The id of a dom element where the tool will render

##### service

An object representing an intermine service  

```json
{
  "root": "www.flymine.org/query",
  "token": "bananacakes"
}
```

##### package

An object representing the data passed to the app, e.g.:

```json
{
  "class": "Gene",
  "format": "ids",
  "value": [123, 456]
}
```

##### state

The last state of the tool (more on this later)

#### dist
##### style.css
Optional, but must be a built / processed file if present. If your file isn't called style.css, make sure to specify the file name in config json.
##### bundle.js
Required - this should be the built file that makes your code go, with all dependencies bundled in, excluding im.js which is available on the window. Make sure to export an object that matches your tool name and has  a main method - e.g. for bluegenesProvista, there is an exposed method called `bluegenesProtvista.main()`.

```javascript
var MyApp.main = function(el, service, package, state, config) {
  // ...
}
```

#### preview.png

Optional preview image for the "app store" dashboard (when admins are selecting  

## Integration

* Apps will be installed to bluegenes/resources/public/apps/[someapp]
* If an app.css it must be namespaced such that all css properties are subclassed to the tool name and can't leak to external tools - e.g. in less, thois could be written `.bluegenesProtvista { // your styles in here}`
* [imjs](https://www.npmjs.com/package/imjs) will be available on the window automatically.

**Credits:** Thanks to [Josh](https://gist.github.com/joshkh/76091f1182d425934c1c5dbe2644d23a) and [Vivek](https://gist.github.com/vivekkrish/2e5e4128efbbf2014c194aae6b83d245) for early work on the Tool API proposal.
