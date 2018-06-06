# Building BlueGenes for production 

The steps details in [getting-started](getting-started.md) are designed to get you up and running for developing BlueGenes. 
But now that you've completed your snazzy updates and you want to deploy the BlueGenes to production, how do you do it?
There are actually a few ways, depending in your needs. 

## Standalone BlueGenes

One of the easiest ways to deploy the prod minified version is to set up [Dokku](http://dokku.viewdocs.io/dokku/) on your intended server. You can also use BlueGenes with [heroku](https://www.heroku.com/).

### Minified deployment using dokku
Once dokku is configured on your remote host, all you need to do to deploy a minified build is add the server as a remote and push to it:

```bash
	git remote add my-awesome-server bluegenes@my-awesome-server.git
        git push my-awesome-server master
```

### Uberjar

It's also possible to compile BlueGenes to a jar that will automatically launch a server when executed. 

To compile and package BlueGenes into an executable jar, run the following command in the project folder:
```bash
$ lein uberjar
```
Then, to start the application, execute the jar and pass in a [`config.edn` file](../config/dev/README.md):

```bash
$ java -jar -Dconfig="config/prod/config.edn" target/bluegenes.jar
```

(When executing the jar the `config.edn` file can be located anywhere, including your home directory for security.)

## BlueGenes as a Jar on Clojars

InterMine 2.0 includes a Gradle target to launch BlueGenes. If you want to update the version of the JAR being launched, you'll need to create an uberjar as above. If it's an official InterMine release, then it can be deployed to clojars, an online artifact repository. Here's how, assuming you have access to deploy to the [org.intermine organisation on clojars](https://clojars.org/groups/org.intermine). Speak to Yo for access if you need it.

### Deploying to Clojars

When deploying BlueGenes to Clojars, the JAR file should include all compiled assets include javascript, less, and the vendor libraries. This allows other projects to include BlueGenes as a dependency and deploy the client and server without needing to compile BlueGenes. To deploy a compiled JAR to clojars, include the `uberjar` profile when running the `lein deploy clojars` command:
```bash
$ lein with-profile +uberjar deploy clojars
```
