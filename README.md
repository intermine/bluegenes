# BlueGenes
## About
BlueGenes is designed to make searching and analysing genomic data easy. It's powered by [InterMine](http://intermine.org/) web services, meaning that the data from nearly 30 InterMines worldwide can be accessed from the same familiar interface.

[Try BlueGenes now](http://bluegenes.apps.intermine.org/)


![BlueGene screenshots](http://i.imgur.com/zwp0uxM.jpg)

**Feedback:** Please create an issue in this repo or email `info - at - intermine - dot - org`

## Getting Started

#### System Requirements
* Java 1.6+
* PostgreSQL 9.3+
* [Leiningen 2.5+](https://leiningen.org/)

#### Initial Setup

BlueGenes has two main components: a web application and a server that hosts it. The server requires a connection to a PostgreSQL server to persist user data. We recommend creating two databases, one for development/testing and one for production:

```bash
$ createdb bluegenes-dev
$ createdb bluegenes-prod
```

You then need to configure BlueGenes to use those databases. Copy and edit the example in [config/dev/README.md](config/dev/README.md) and save it to `config/dev/config.edn` and `config/prod/config.edn`.

#### Compile and Run (Production)

To compile and package BlueGenes into an executable jar, run the following command in the project folder:
```bash
$ lein uberjar
```
Then, to start the application, execute the jar and pass in one of the `config.edn` files from above:

```bash
$ java -jar -Dconfig="config/prod/config.edn" target/bluegenes.jar
```

(When executing the jar the `config.edn` file can be located anywhere, including your home directory for security.)

## Configuration

#### Adding a new mine
Open [src/cljc/bluegenes/mines.cljc](https://github.com/intermine/bluegenes/blob/dev/src/cljc/bluegenes/mines.cljc#L7-L51) and copy the value of the `sample-mine` variable into the `mines` hashmap. Change the key to something unique (`:yourmine`) and edit the default values appropriately. Be sure to edit the {:id ...} value to reflect the key you used for `:yourmine`

```clj
(def mines {:humanmine {...}
            :yourmine  {:id :yourmine
	                ...})
```



#### Changing the default mine
Open `src/cljs/bluegenes/db.cljs` and edit the `:current-mine` hashmap value to the keyword of a mine in `mines.cljc` (see above).

```clj
(def default-db
  {...
   :mine-name :yourmine
   ...}
```
Please note that you will have to recompile the application for the changes to take effect (see below). Also, may need to clear your local storage for the `:default-mine` to take effect. You can do this by visiting the web application, clicking the cog on the top right, selecting Debug, and then clicking the button to delete local storage.

## Developers

### Prerequisites and Dependencies

You will need [Leiningen][lein] 2.0 or above installed (2.4+ to use the web-repl). This handles all
java/clojure dependencies. As clojure is a JVM language, this requires a JDK (1.6+) be installed;
please see your friendly java vendor for details.

A [node-js][nodejs] environment is also required, which handles the
installation of the javascript dependencies using [npm][npm] and
[Bower][bower].

**Required:** The InterMine you point BlueGenes at *must* be running InterMine 1.7.2 or above.

### Download dependencies.

```
bower install
```

### Compile css file once.

```
lein less once
```

### Automatically recompile css file on change.

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

## Production Build
<!---
```
lein clean
lein uberjar
```

That should compile the clojurescript code first, and then create the standalone jar.

When you run the jar you can set the port the ring server will use by setting the environment variable PORT.
If it's not set, it will run on port 3000 by default.
-->

If you only want to compile the clojurescript code (this is a quick and easy way to deploy but less automated):

```
lein clean
lein cljsbuild once min
```

Then you can copy the `/public/resources` folder to a static server such as apache or nginx.

### Minified deployment using dokku
One of the easiest ways to deploy the prod minified version is to set up [Dokku](http://dokku.viewdocs.io/dokku/) on your intended server. Once dokku is configured on your remote host, all you need to do to deploy a minified build is add the server as a remote and push to it:

	git remote add my-awesome-server bluegenes@my-awesome-server.git
    git push my-awesome-server master


[lein]: https://github.com/technomancy/leiningen
[bower]: http://bower.io/
[npm]: https://www.npmjs.com/
[nodejs]: https://nodejs.org/
