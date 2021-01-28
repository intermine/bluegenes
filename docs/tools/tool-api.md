# Bluegenes Tool API Specifications

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

Put all of your prod-ready bundled files in here. Ideally, this should be no more than two things:

- **bundle.js**, contains your entire application, with all dependencies bundled in, excluding im.js which is available on the window.
- **style.css**, optional. Use if any additional styles are required. If your file isn't called style.css, make sure to specify the file name in config json.

### src

Where do the bundled files come from? Probably the src directory. This is the folder you'll be doing most of your work in.

#### index.js

This is the preferred entry point to build dist/bundle.js. May import external libraries or node modules if needed. [See bluegenesProtVista example](https://github.com/intermine/bluegenesProtVista/tree/master/src). Make sure to export an object that matches your tool name and has a main method - e.g. for bluegenesProtVista, there is an exposed method called `bluegenesProtvista.main()`.

The signature of the main method should have the following signature:

```javascript
var myApp.main = function(el, service, imEntity, state, config, navigate) {
  // code to initialise your app here
}
```


**el** - The id of a dom element where the tool will render.

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
  "Gene": {
    "class": "Gene",
    "format": "id",
    "value": 456
  }
}
```

If your tool *accepts* `ids` and takes multiple *classes*, (see [config.json](/docs/tools/tool-api.md#configjson)) it might receive more than one class if they are present on the list or query results page.

```json
{
  "Gene": {
    "class": "Gene",
    "format": "ids",
    "value": [1, 2]
  },
  "Protein": {
    "class": "Protein",
    "format": "ids",
    "value": [3, 4]
  }
}
```

Subclasses (descendant of a class in the model hierarchy) might also be passed to your tool if it's descendant of one of your tool's *classes*. When this happens, the key will still be its superclass which you specified in *classes*, while the subclass name can be accessed under `class`. If you want your tool to work with subclasses, you'll need to make sure that any queries you build based on imEntity sets the `from` key to this `class` (`imEntity.Gene.class` in this example).

```json
{
  "Gene": {
    "class": "ORF",
    "format": "id",
    "value": 5
  }
}
```

It is up to you which class you want to use in your tool, and you can even use multiple.

Currently, it is not possible to receive multiple classes on the report page with *accepts* `id`. However, the Tool API allows for this, should it be an option in the future.

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
  },
  "version": 2
}
```
#### Accepts:

##### Currently supported in BlueGenes:
* id: a single database id  
* ids: multiple database ids

##### Planned for the future:

* records: a raw result from POSTing to /query/results with format "jsonobjects"  
* rows: a raw result from POSTing to /query/results with format "json"  

Plurality (i.e. id vs ids) will help to determine which context a tool can appear (report page, list analysis page).

**classes** default to `*` if this tool isn't class / objectType specific. Otherwise, your tool might be specific to a certain class e.g. a gene displayer. Note that a subclass of a class you specify here may be passed via *imEntity* (see its section above for more details).

**columnMapping** is an important way to specify (or override) which columns should be passed to the tool by BlueGenes. As an example, for a gene tool, you might want to pass a symbol, or a primaryIdentifier, or even secondaryIdentifier - and this might change depending on the InterMine that is fuelling the BlueGenes. Set a default likely value here, and in the future, individual bluegenes administrators can override it if needed.

**depends** lets you specify any class names in the InterMine instance's model that your tool depends on. This is useful if you're querying for a non-standard path that is only present in a specific InterMine instance. Any instances which don't have the class name in their model will not attempt to run your tool, and will instead, list it as unsupported.

**files** - one file each for css and js, please. This should be the file bundled/built with all dependencies except/ imjs if needed. CSS is optional if the tool has no styles.

**toolName** is an object with a human-readable name, as well as an internal name. The human name would be what you want to see as a header for this tool (e.g. ProtVista might be called "Protein Features"). The internal `cljs` name needs to be unique among tools and identical to the global JS variable which your tool's bundle initialises.

**version** is a whole number indicating which major version of the Tool API your tool adheres to. When creating a tool, you should always specify the latest version presented here. If your tool's version does not match the Tool API version of the BlueGenes using your tool, a warning will be shown and your tool will be disabled from displaying. In this case, you will have to update your tool to support the Tool API version of the BlueGenes using your tool, update the version key in your *config.json* and publish a new version of your tool. See the [Changelog](/docs/tools/tool-api.md#changelog) for details on each version.

#### preview.png

Optional preview image for the "app store" dashboard. When admins are selecting tools, this is the way to impress them!

## Other notes
* [imjs](https://www.npmjs.com/package/imjs) will be available on the window automatically.

**Credits:** Thanks to [Josh](https://gist.github.com/joshkh/76091f1182d425934c1c5dbe2644d23a) and [Vivek](https://gist.github.com/vivekkrish/2e5e4128efbbf2014c194aae6b83d245) for early work on the Tool API proposal.

## Changelog

We aim to keep all changes to the Tool API as backwards compatible as possible, but in some cases, breaking changes are necessary. The Tool API major version number will increment on breaking changes and additional details on the rationale and upgrading process will be included.

Guidelines which should be followed for Tool API changes:
1. All maintainers of the tools in https://github.com/topics/bluegenes-tool need to be contacted.
2. A breaking change should be avoided unless deemed absolutely necessary, as agreed between developers and maintainers.
3. Developers will assist with upgrading existing tools, even so far as to creating PRs.
4. If the tool maintainer doesn't provide a way to test the updated tool, this becomes their responsibility.
5. When releasing a breaking version, send an email to the dev-intermine mailing list with a warning that things may break if they update BlueGenes to this version.

### Tool API version 1.0

- Initial release.

### Tool API version 2.0

- Changes `imEntity` from an object to a nested object with keys corresponding to each object's class.

  `{"class": "Gene", "format": "id", "value": 1}` **-->** `{"Gene": {"class": "Gene", "format": "id", "value": 1}}`

  **Rationale:** It's possible for a list or query results page to have multiple classes, depending on the columns present. This meant a tool needed to be able to receive multiple imEntity's, which the previous Tool API didn't allow.

  **Upgrading:** You will need to grab the value you wish to work on out from the nested object in `imEntity`. As an example, for a tool that works on the "Gene" class, you would change `imEntity.value` to `imEntity.Gene.value`. If your tool takes multiple classes, you can decide whether to always default to one if available, or present a different behaviour when multiple classes are present.

- Adds a `version` key to *config.json*.

  **Rationale:** To accomodate the first breaking change in the Tool API, we have added versioning of the tools. If your tool's version does not match the Tool API version of the BlueGenes using it, a warning will be displayed and your tool won't be shown on the respective pages. To make your tool work again, you will have to update it to support the changes to the Tool API, as well as update the `version` key in *config.json*. Note that a missing `version` key will be interpreted as version 1.

  **Upgrading:** Make sure your tool adheres to Tool API version 2 as described here, then add `"version": 2` to your *config.json*.
