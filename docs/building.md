# Local build

## System Requirements

* OpenJDK, version 8 (only until we make our software compatible with OpenJDK 11)
* Latest [Leiningen](https://leiningen.org/)
* Latest supported [nodejs](https://nodejs.org/).  You can check your version using `node -v`). We recommend installing node using [nvm](https://github.com/creationix/nvm)
* Latest supported [npm](https://www.npmjs.com/)
* InterMine version 1.8+ (version 2.0 recommended)


## Download NPM dependencies

    npm install


## Compile the CSS

We use [less](http://lesscss.org/) to write our styles.

If you only want the CSS to be uploaded once, run:

    lein less once

Note: in this case you will have to manually recompile the CSS files after each change.

If you will be changing the CSS files continuously, you can automatically recompile the CSS files after each change using:

    lein less auto

Note: even that you will not see a prompt telling you when it's complete, the browser page will automatically refresh.


## Make Leiningen reload code changes in the browser

    lein figwheel dev


## Start the web server

If you have OpenJDK 8:

    lein with-profile +dev run

If you have OpenJDK 9:

    lein with-profile +java9 figwheel


By default, the web server will be started on http://localhost:5000/. To change this value, edit the corresponding `config.edn`.


## Running tests

    lein clean
    lein doo phantom test once

The above command assumes you will use [phantomjs](https://www.npmjs.com/package/phantomjs) (already declared as a Node.js development dependency). However, please note that [doo](https://github.com/bensu/doo) can be configured to run cljs.test in many other JS environments (chrome, ie, safari, opera, slimer, node, rhino, or nashorn).



# Production Builds

## Testing a minified instance before deploying:

Most of the time, we develop with uncompressed files - it's faster for hot reloads. But for production, we want things to be extra fast on load and we don't hot-reload changes, so it's better to present minified files that have had all un-necessary code stripped. Clojure uses Google Closure tools (yes, Clojure uses Closure) to minify things.

Sometimes the Closure compiler is overzealous and removes something we actually wanted to keep. To check what your work looks like in a minified build, run this in the terminal (I'd recommend closing any existing lein run / lein figwheel sessions first).

    lein cljsbuild once min + lein run

There is also a shortcut:

    lein prod


## Deploying your build

One of the easiest ways to deploy the prod minified version is to set up [Dokku](http://dokku.viewdocs.io/dokku/) on your intended server. You can also use BlueGenes with [heroku](https://www.heroku.com/).


### Minified deployment using dokku
Once dokku is configured on your remote host, all you need to do to deploy a minified build is add the server as a remote and push to it:

    git remote add my-awesome-server bluegenes@my-awesome-server.git
    git push my-awesome-server master


### Uberjar

It's also possible to compile BlueGenes to a jar that will automatically launch a server when executed.

To compile and package BlueGenes into an executable jar, run the following command in the project folder:

    lein uberjar

Then, to start the application, execute the jar and pass in a [`config.edn` file](../config/dev/README.md):

    java -jar -Dconfig="config/prod/config.edn" target/bluegenes.jar

(For security reasons, the `config.edn` file used to execute the jar can be located anywhere, including your home directory.)


### Launching your uberjar with InterMine

InterMine 2.0 includes a [Gradle target to launch a BlueGenes instance](https://intermine.readthedocs.io/en/latest/system-requirements/software/gradle/index.html#deploy-blue-genes).

By default, it launches the latest BlueGenes release from Clojars. If you want to update the version of the JAR being launched, you'll need to create an uberjar (see above).


### Deploying your uberjar to Clojars

Official BlueGenes releases can be deployed to [Clojars](https://clojars.org/), under the [org.intermine Clojars organisation](https://clojars.org/groups/org.intermine).

When deploying BlueGenes to Clojars, the JAR file should include all compiled assets: this includes JavaScript, less, and vendor libraries. This allows other projects to include BlueGenes as a dependency and deploy the client and server without needing to compile BlueGenes.

To deploy a compiled JAR to clojars, include the `uberjar` profile when running the `lein deploy clojars` command:

    $ lein with-profile +uberjar deploy clojars



# Troubleshooting

1. When things get weird, you might consider clearing both your browser's and BlueGene's cache data. To clear BlueGenes', click on the cog in the top right, then "developer". There should be a big blue button that clears local storage.
2. Verify what branch you have checked out. `dev` is our main development branch, whereas `master` is our production branch.
3. Verify that the InterMine web services ("InterMines") you are using are running the latest InterMine release. You will find a list of InterMines and their current version under the key `intermine_version` in the [InterMine registry](http://registry.intermine.org/service/instances). The changelog for InterMine release versions is [available on GitHub](https://github.com/intermine/intermine/releases).
4. Remember that you can always change which InterMine you're using in BlueGenes by using the cog (top right).

If none of these tips help you, create an [issue](https://github.com/intermine/bluegenes/issues) or [contact us (via chat, email, mailing list, etc.)](http://intermine.readthedocs.io/en/latest/about/contact-us/)
