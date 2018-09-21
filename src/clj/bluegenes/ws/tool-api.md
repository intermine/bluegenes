# Bluegenes Tool API Spec

BlueGenes is built in Clojure and ClojureScript, but we don't want to re-write all existing browser data vis and analysis tools, so we've built a way to integrate JavaScript tools into report pages.

You can take existing JavaScript biology applications and provide a wrapper for them, allowing them to interact with BlueGenes.

This document provides reference for the API specifications, but there is also a [tutorial to walk you through creating a wrapper for your tool](tool-api-tutorial).

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

You may optionally also have additional folders, including node_modules, if needed. They won't interfere.

### dist

Put all of your prod-ready bundled files in here. Ideally this should be no more than two things:

- **bundle.js**, contains your entire application, bundled with its dependencies
- **style.css**, optional. Use if any additional styles are required.

### src

Where do the bundled files come from? Probably the src directory. Ensure that:

- **index.js** This is the preferred entry point to build dist/bundle.js. May import external libraries or node modules if needed. [See bluegenesProtVista example ](https://github.com/intermine/bluegenesProtVista/tree/master/src)
- **style.less** This is the preferred entry point to build dist/styles.css.  If your tool has a stylesheet already, make sure to import the styles and wrap them in a parent class to ensure the styles are sandboxed and don't leak into another file. See [bluegenesProtVista's less file for an example](https://github.com/intermine/bluegenesProtVista/blob/master/src/style.less) and [the css it compiled to](https://github.com/intermine/bluegenesProtVista/blob/master/dist/style.css).

### config.json

This file provides bluegenes-specific config info. Some further config info is drawn from package.json as well.

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
##### Accepts: possible values

###### Currently supported in BlueGenes:
* id: a single database id  

###### Planned for the future:

* ids: multiple database ids  
* list: a single list name  
* lists: multiple list names  
* records: a raw result from POSTing to /query/results with format "jsonobjects"  
* rows: a raw result from POSTing to /query/results with format "json"  

Plurality (i.e. id vs ids) will help to determine which context a tool can appear (report page, list analysis page)

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
