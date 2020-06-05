# Bluegenes Tool API Spec

BlueGenes is built in Clojure and ClojureScript, but we don't want to re-write all existing browser data vis and analysis tools, so we've built a way to integrate JavaScript tools into our pages.

You can take existing JavaScript biology applications and provide a wrapper for them, allowing them to interact with BlueGenes.

This document provides reference for the API specifications, but there is also a [tutorial to walk you through creating a wrapper for your tool](tool-api-tutorial.md).

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

- **bundle.js**, contains your entire application, with all dependencies bundled in, excluding im.js which is available on the window.
- **style.css**, optional. Use if any additional styles are required. If your file isn't called style.css, make sure to specify the file name in config json.

### src

Where do the bundled files come from? Probably the src directory. This is the folder you'll be doing most of your work in.

#### index.js

This is the preferred entry point to build dist/bundle.js. May import external libraries or node modules if needed. [See bluegenesProtVista example ](https://github.com/intermine/bluegenesProtVista/tree/master/src). Make sure to export an object that matches your tool name and has a main method - e.g. for bluegenesProtVista, there is an exposed method called `bluegenesProtvista.main()`.

The signature of the main method should look have the following signature:

```javascript
var myApp.main = function(el, service, imEntity, state, config, navigate) {
  // code to initialise your app here
}
```


**el** - The id of a dom element where the tool will render

**service** - An object representing an intermine service, like the following:

```json
{
  "root": "www.flymine.org/query",
  "token": "bananacakes"
}
```

**imEntity**

An object representing the data passed to the app, e.g.:

```json
{
  "class": "Gene",
  "format": "id",
  "value": 456
}
```

To see a full example, look at how the method is called in [bluegenesProtVista's ui demo file](https://github.com/intermine/bluegenesProtVista/blob/master/demo.html) and how [index.js uses the values passed to it](https://github.com/intermine/bluegenesProtVista/blob/master/src/index.js)

**navigate**

A function you can call to make BlueGenes navigate to a specific page.

```javascript
// Navigate to a report page.
navigate("report", {type: "Gene", id: 1018204});
// Run a query and open the page showing the results.
navigate("query", myQueryObj);
```

You can optionally specify a third argument with the namespace of a mine (e.g. `"humanmine"`).

#### style.less
This is the preferred entry point to build dist/styles.css.  If your tool has a stylesheet already, make sure to import the styles and wrap them in a parent class to ensure the styles are sandboxed and don't leak into another file. See [bluegenesProtVista's less file for an example](https://github.com/intermine/bluegenesProtVista/blob/master/src/style.less) and [the css it compiled to](https://github.com/intermine/bluegenesProtVista/blob/master/dist/style.css).

### config.json

This file provides bluegenes-specific config info. Some further config info is drawn from package.json as well.

```json
{
  "accepts": ["id", "ids", "records", "rows"],
  "classes": ["Gene", "Protein", "*"],
  "columnMapping": {"Protein": {"id" : "primaryAccession"}},
  "depends": ["AtlasExpression", "ProteinAtlasExpression"],
  "files": {
    "css": "dist/style.css",
    "js": "dist/bundle.js"
  },
  "toolName": {
    "human": "Protein Features",
    "cljs": "proteinFeatures"
  }
}
```
#### Accepts:

##### Currently supported in BlueGenes:
* id: a single database id  
* ids: multiple database ids

##### Planned for the future:

* records: a raw result from POSTing to /query/results with format "jsonobjects"  
* rows: a raw result from POSTing to /query/results with format "json"  

Plurality (i.e. id vs ids) will help to determine which context a tool can appear (report page, list analysis page)

**classes** default to `*` if this tool isn't class / objectType specific. otherwise your tool might be specific to a certain class, e.g. a gene displayer.

**columnMapping** is an important way to specify (or override) which columns should be passed to the tool by BlueGenes. As an example, for a gene tool, you might want to pass a symbol, OR a primaryIdentifier, or even secondaryIdentifier - and this might change depending in the InterMine that is fuelling the BlueGenes. Set a default likely value here, and in the future individual bluegenes administrators can override it if needed.

**depends** lets you specify any class names in the InterMine instance's model that your tool depends on. This is useful if you're querying for a non-standard path that is only present in a specific InterMine instance. Any instances which don't have the class name in their model, will not attempt to run your tool, and will instead list it as unsupported.

**files** - one file each for css and js, please. This should be the file bundled/built with all dependencies except/ imjs if needed. CSS is optional if the tool has no styles.

**toolName** is an object with a human-readable name, as well as an internal name. The human name would be what you want to see as a header for this tool (e.g. ProtVista might be called "Protein Features"). The internal `cljs` name needs to be unique among tools and identical to the global JS variable which your tool's bundle initialises.


#### preview.png

Optional preview image for the "app store" dashboard (when admins are selecting tools, this is the way to impress them!)

## Other notes
* [imjs](https://www.npmjs.com/package/imjs) will be available on the window automatically.

**Credits:** Thanks to [Josh](https://gist.github.com/joshkh/76091f1182d425934c1c5dbe2644d23a) and [Vivek](https://gist.github.com/vivekkrish/2e5e4128efbbf2014c194aae6b83d245) for early work on the Tool API proposal.
