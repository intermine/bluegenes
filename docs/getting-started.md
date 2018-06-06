You can run bluegenes locally for development purposes. Here's what the local setup should look like. 

## System Requirements
* Java 1.6+
* [Leiningen 2.5+](https://leiningen.org/)


### Prerequisites and Dependencies

You will need [Leiningen][lein] 2.0 or above installed (2.4+ to use the web-repl). This handles all
java/clojure dependencies. As clojure is a JVM language, this requires a JDK (1.6+) be installed;
please see your friendly java vendor for details.

A [node-js][nodejs] environment is also required, which handles the
installation of the javascript dependencies using [npm][npm] and
[Bower][bower].

**Required:** The InterMine you point BlueGenes at *must* be running InterMine

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

