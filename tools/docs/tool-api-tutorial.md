# Bluegenes Tool API Tutorial

You don't need to know Clojure/Clojurescript in order to create a data vis / analysis tool for BlueGenes. This document will walk you through converting your favourite javascript tool into a BlueGenes compatible tool. You may wish to keep the [Tool API documentation](tool-api.md) open in another tab.

For this tutorial, we'll be creating a tool that fetches GO terms related to a single gene, broken down by namespace. Using the query builder and im-tables, we can generate the imjs query required, and we'll save it for later:

```javascript
var query    = {
  "from": "Gene",
  "select": [
    "goAnnotation.ontologyTerm.name",
    "goAnnotation.evidence.code.code",
    "goAnnotation.evidence.code.name",
    "goAnnotation.ontologyTerm.namespace"
  ],
  "where": [
    {
      "path": "symbol",
      "op": "=",
      "value": "zen"
    }
  ]
};
```

## Getting ready:

What you'll need to build the tool:

- npm and node 7+, preferably installed via [nvm](https://github.com/creationix/nvm)
- A JavaScript tool that you would like to implement in a BlueGenes report page. See our [Tool Board](https://github.com/intermine/bluegenes/projects/2#column-943519) for some options.
- [yeoman](http://yeoman.io/) (You can install via `npm install -g yo`).
- A text editor and modern browser.

Optionally, to test your tool:

- [A local BlueGenes install](https://github.com/intermine/bluegenes/blob/dev/docs/getting-started.md)

We'll expect a basic familiarity with programming - specifically [JavaScript](https://developer.mozilla.org/en-US/docs/Glossary/JavaScript), [LESS](http://lesscss.org/)/[CSS](https://developer.mozilla.org/en-US/docs/Web/CSS), and to a lesser degree the [DOM](https://developer.mozilla.org/en-US/docs/Web/API/Document_Object_Model/Introduction).

## Generate your tool scaffold

If you look at the [Tool API documentation](tool-api.md), you many note that BlueGenes tools have a specific folder structure. The good news is that you don't have to create all these files yourself! We have a yeoman generator which makes things easier for you.

### Install the Yeoman Generator

Assuming you've installed yeoman already, run the following command to install the BlueGenes tool generator:

```bash
 npm install -g @intermine/generator-bluegenes-tool
```

Brilliant! You'll only need to install the generator once.

### Generate a new project: The wizard

In the parent folder where you'd like your project to live, we need to make a new folder with your preferred name (perhaps something like `~/projects/myBluegenesTool`), and then generate a new tool scaffold.

```
mkdir myBluegenesTool
cd myBluegenesTool
yo @intermine/bluegenes-tool
```

This will walk you through a few questions in a step-by-step wizard. We'll walk through what each one means now:

1. **? What shall we name your project? This is a computer name with no spaces or special characters.** a name for your tool. We'd recommend prefixing every tool with the word bluegenesTool, e.g. `bluegenesToolProtVista` or `bluegenesToolCytoscape`. If you're not sure what you want to call your project yet, you could name it `bluegenesToolGOTerms`.
2. **NPM package name? This is a computer name with no capital letters or special characters apart from the - dash** - something like `bluegenes-tool-go-terms` would be perfect.
2. **Thanks! Now, give me a human name for the project - e.g. "Protein Feature Viewer"** - a nice friendly name for humans to read - spaces are allowed. Let's call ours "GO Terms"
3. **Fabulous. Which report pages do you expect this tool to work for, e.g. "Gene" or "Protein"? Separate with commas and put * for all.** This needs to be an InterMine class or classes. Since GO stands for Gene Ontology, let's enter `Gene`, to show this tool on all gene report pages.
4. **Awesome. What type of InterMine data can you work with?** - Right now, you should only select `id` - the tool API as of version 1 only supports the report page. Selecting id means that the tool will be passed the id of a single InterMine entity - e.g. a protein might be represented by the ID 4815162342.
5. **What's your name?** Hopefully you know the answer to this one! ;) This is important for package.json, which we will automatically generate you.
6. **Your email** As above - it's useful for package.json.
7. **Your website**  As above - it's useful for package.json.
8. **Which license do you want to use?** we're pre-provided a few licences to choose from. Whichever you choose, remember that InterMine is LGPL 2.1, meaning it can be taken into private repositories. This is _not_ compatible with viral licences like GPL. For your tool, LGPL, MIT, or Apache might be good choices that are compatible with LGPL.

Once you select a licence, the yeoman installer should set up your repository and all the files within based on the responses you gave to the wizard. This may take a minute or two.

### Setting up your newly scaffolded tool

Take a few minutes to look through your newly generated tool files. We'll be doing most of our work in the `src/index.js` file, and previewing it via demo.html.

#### fetching and formatting our data

Open up src/index.js and take a quick look. There's a lot of dummy / example code that we can delete, but make sure not to delete the main method, `export function main (el, service, imEntity, state, config)`. Instead, the code we write should be INSIDE this method (or called from inside it). This is the method that BlueGenes will automatically call when it loads your tool.

You can read more about the arguments for this method in the [tool API docs](tool-api.md). In this tutorial, we'll use these args:

- `service` provides the URL and token for communicating with a mine,
- `imEntity` provides the details of the report page we're looking at - i.e. it might be that the tool is being passed the objectid of a gene, or the accession of a protein...
- `el` - the id of the html element we can attach any text / visualisations to.

Let's write some code. Remember the query we looked up, earlier? We want to use
imjs to load the query results and then display them on the page. Let's copy the
 query into index.js and print the results to the console to see if it works like we hoped.

 Here's my first pass at index.js, deleting all the boilerplate and logging my query to the console:

```javascript
//make sure to export main, with the signature
export function main (el, service, imEntity, state, config) {

  var query    = {
    "from": imEntity.class, //this should always be Gene, because we configured this tool to display data on Gene report pages.
    "select": [
      "goAnnotation.ontologyTerm.name",
      "goAnnotation.evidence.code.code",
      "goAnnotation.evidence.code.name",
      "goAnnotation.ontologyTerm.namespace"
    ],
    "where": [
      {
        "path": imEntity.format, //this translates to id, based on our tool config.
        "op": "=",
        "value": imEntity.value  // BlueGenes will pass this dynamically.
      }
    ]
  };
    //fetch data using imjs, which is available on the window.
    var goTerms = new imjs.Service(service)
        .records(query)
        .then(function(response) {
          //we'll add more code here, but in the meantime, print the result to the console.
          console.log(response);
    });
}
```

Note the use of `imEntity` to form the query. You might be wondering where this data is coming from.
When you're using BlueGenes, it'll be passed automatically from BlueGenes to your tool. Since we're just working in standalone mode at the minute, hop over to demo.html instead to see what's being passed over as arguments. You can tweak the `dataToInitialiseToolWith` variable to make sure it reflects sample data you want to work with. You can also change the mine URL if needed!

Set your `dataToInitialiseToolWith` variable in demo.html to look like this. You may need to tweak the id depending on which InterMine you are using. (To get an id in a JSP-based InterMine, go to a report page and grab it from the URL. The url in the code snippet below came from this report page: [http://www.humanmine.org/humanmine/report.do?id=1276963](http://www.humanmine.org/humanmine/report.do?id=1276963))

```javascript
dataToInitialiseToolWith = {
  "class": "Gene",
  "format": "id",
  // right now this id corresponds to Human GATA1
  // this is a bit fragile since IDs change with every build
  // but we don't want to make the code more complicated than
  // needed for the demo.
  "value": 1276963
},
```

Okay, now that we have our sample data set up and some code, let's test if it works!

In your console, you'll need to bundle your js file. To do so, run `npm run build`. You should notice that we now have a folder called `dist` which contains a file: `bundle.js`. How can we inspect to see if it is working or not? Well, we've included a handy little server for just this purpose! To test it out, run `npm run dev` and then navigate to
[http://localhost:3456](http://localhost:3456). If everything went well, you should see a white screen, and your query results logged to the [javascript console](https://www.digitalocean.com/community/tutorials/how-to-use-the-javascript-developer-console). Great! It should look roughly like this:

![output of console log, showing an array of GO terms associated with the Gene we searched for.](img/console-log-preview-tutorial.png)

##### Making something display on the screen...

White screens are boring. Let's output our results by iterating through them, sorting by namespace,  and printing to the screen. In the demo below, we've used a quick and easy technique of building up the HTML using strings; we wouldn't recommend this for any large applications but for a small demo tool it's sufficient - a framework would definitely be overkill!

Here's the same code as before, but expanded to output the results visually in the HTML. In particular, note how we've set `el.innerHTML = <someHTMLHere>` - this is how we get our work to display in BlueGenes.

```javascript
//make sure to export main, with the signature
export function main(el, service, imEntity, state, config) {

  //this query fetches GO terms and evidence codes associated with the given gene.
  var query = {
    "from": imEntity.class, //In this case, this should always be Gene, because
    // we configured this tool to display data on Gene report pages. It's still
    // better practice to use imEntity.class rather than hardcoding to gene -
    // consider a tool that worked for genes OR proteins, for example!
    "select": [
      "goAnnotation.ontologyTerm.name",
      "goAnnotation.evidence.code.code",
      "goAnnotation.evidence.code.name",
      "goAnnotation.ontologyTerm.namespace"
    ],
    "orderBy": [{
      "path": "goAnnotation.ontologyTerm.namespace",
      "direction": "ASC"
    }],
    "where": [{
      "path": imEntity.format, //this translates to id, based on our tool config.
      "op": "=",
      "value": imEntity.value // BlueGenes will pass this dynamically.
    }]
  };

  //fetch data using imjs, which is available on the window.
  var goTerms = new imjs.Service(service)
    .records(query)
    .then(function(response) {
      //process results so they're grouped by their namespaces
      var terms = resultsToNamespaceBuckets(response);
      // output the results into HTML and add to the element provided
      // in the head of the main method.
      el.innerHTML = buildVisualOutput(terms);
    });
}

/**
Given namespace-sorted terms, return an HTML list of each term with its evidence
code(s).
**/
function buildVisualOutput(terms) {
  var namespaces = Object.keys(terms),
    termUI = "<div class='namespaces'>";
  //loop through the namespaces and create a header for each namespace
  namespaces.map(function(namespace) {
    termUI = termUI + "<div class='namespace'><h3>" + namespace + "</h3> <ul>";
    //loop through the terms in each namespace
    terms[namespace].map(function(result) {
      //create a new list entry for each GO term.
      termUI = termUI + "<li>" + result.ontologyTerm.name
      // we're also going to add the evidence codes for each GO term. Codes
      // are used to explain _why_ a given gene is annotation with a GO term.
      // Evidence codes come in an array (there could be more than one per GO
      // term), so we have to loop through them too.
      result.evidence.map(function(evidence) {
        //add a span for each evidence code.
        termUI = termUI + "<span class='evidencecode' title='" +
          evidence.code.name +
          "'>" + evidence.code.code + "</span>";
      });
      //close all the open elements so we have well-formed HTML.
      termUI = termUI + "</li>";
    });
    termUI = termUI + "</ul></div>"
  });
  return termUI;
}

/**
Given a set of InterMine results (GO terms associated with a single gene),
sort the terms by namespace and return the results sorted into named buckets.
**/
function resultsToNamespaceBuckets(response) {
  //we're going to sort our terms by namespace.
  //here's a var to store each type of term in...
  var terms = {
    molecular_function: [],
    cellular_component: [],
    biological_process: []
  };
  //iterate through the results and group them by namespace
  response[0].goAnnotation.map(function(result) {
    //we don't need to store anything except the details in ontologyterm
    //push each term into the correct box
    terms[result.ontologyTerm.namespace].push(result);
  });
  return terms;
}
```

##### Goodness, that's ugly! Let's make it look a little nicer.

_Note: This section assumes some basic familiarity with [CSS](https://developer.mozilla.org/en-US/docs/Learn/CSS/Introduction_to_CSS) and [LESS](http://lesscss.org/). You can use CSS frameworks like Bootstrap if you prefer (Bootstrap 3 is on the window), but you'll still need to follow this guide and namespace + compile your CSS._

Head over to `src/style.less`. You'll notice that there have been a few lines pre-populated on your behalf - it might look something like this:

```less
.bluegenesToolGOTerms {
  // put all your css classes inside this block. Keeping them
  // nested under the tool name prevents your styles from
  // leaking out and affecting other elements by accident.

  // If you want to learn more about LESS, visit lesscss.org.

  //to build your css, run `lessc src/style.css dist/style.css --clean-css`
  // in your terminal
}

```

Nest all your css inside the main code block, between the two curly brackets `{}` - this ensures that _your_ css won't affect bluegenes or other tools by accident.

We've added a few minimal style rules here to show the GO terms in a column layout:

```less
.bluegenesToolGOTerms {
  // put all your css classes inside this block. Keeping them
  // nested under the tool name prevents your styles from
  // leaking out and affecting other elements by accident.
  // If you want to learn more about LESS, visit lesscss.org.
  //to build your css, run `lessc src/style.css dist/style.css --clean-css`
  // in your terminal
  .namespaces {
    display: flex;
    flex-wrap:wrap;

    .namespace {
      margin: 1em;
      max-width:30%;
      h3 {text-align:center;}
    }

    .evidencecode {
      margin: 0.2em;
    }
  }
}
```

In order for the less to affect the page, we need to compile it into CSS. In your console, run

```bash
npm run less
```

Now, if you refresh your demo.html page, you should see the new css applied! You can also look at your compiled CSS if you wish - it's in `dist/style.css`.

### Releasing your tool and using it in BlueGenes.

At this point, you should have a basic functional tool that consumes InterMine data and looks nice when you visit http://localhost:3456 - well done! So you'll probably want to test it in BlueGenes now, right? You have two ways to do this:

- for tools under development, like this one, the easiest thing to do is [use npm link to install your new tool](tools.md#development-tools) in the bluegenes tool folder.  
- for tools that are release ready (perhaps because you've tested them via the npm link method above), you can [release the tool on npm](https://docs.npmjs.com/cli/publish). This will make the tool available for others. To install a tool that's released via NPM, see our [installing published tools guide](tools.md#published-tools).
