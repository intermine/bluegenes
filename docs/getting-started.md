You can run bluegenes locally for development purposes. Here's what the local setup should look like. 

## System Requirements
* Java 1.6+
* [Leiningen 2.5+](https://leiningen.org/)
* [node 7+][nodejs]  (you can check your version using `node -v`)
* [npm][npm]
* **Required:** The InterMine you point BlueGenes at *must* be running InterMine 1.8 or later; ideally 2.0. 

### Download dependencies.

```
npm install
```

### Compile the CSS

We use [less](http://lesscss.org/) to write our styles. In order to run BlueGenes you'll need to compile the less, using


```
lein less once
```

_Or_, if you'll be making lots of style edits and don't want to type `lein less once` every time you make a change, you can automatically recompile the css file whenever it changes, using:

```
lein less auto
```

Note that you won't see a prompt telling you when it's complete if you use `lein less auto` - but the browser page will automatically refresh so you'll know when it's done. 

### Start the process to reload code changes in the browser:

```
lein figwheel dev
```

### Start the web server:

In another terminal, run the following
```
lein with-profile +dev run
```

Then visit http://localhost:5000/ (or whichever port you specific in config.edn)

### Run tests:

```
lein clean
lein doo phantom test once
```

The above command assumes that you have [phantomjs](https://www.npmjs.com/package/phantomjs) installed. However, please note that [doo](https://github.com/bensu/doo) can be configured to run cljs.test in many other JS environments (chrome, ie, safari, opera, slimer, node, rhino, or nashorn).

## What next?

Once you're happy with any edits you've made, you probably want to check that it all works the same in a minified prod build. See [production builds](production-builds.md) for info on deploying and testing a minified build. 


[lein]: https://github.com/technomancy/leiningen
[npm]: https://www.npmjs.com/
[nodejs]: https://nodejs.org/


