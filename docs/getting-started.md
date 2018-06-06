You can run bluegenes locally for development purposes. Here's what the local setup should look like. 

## System Requirements
* Java 1.6+
* [Leiningen 2.5+](https://leiningen.org/)
* [node 7+][node]  (you can check your version using `node -v`)
* [Bower][bower]
* **Required:** The InterMine you point BlueGenes at *must* be running InterMine 1.8 or later; ideally 2.0. 

### Download dependencies.

```
bower install
```

Compile css file once.

```
lein less once
```

Automatically recompile css file on change.

```
lein less auto
```

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

[lein]: https://github.com/technomancy/leiningen
[bower]: http://bower.io/
[npm]: https://www.npmjs.com/
[nodejs]: https://nodejs.org/


