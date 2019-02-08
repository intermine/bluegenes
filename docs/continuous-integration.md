# Continuous Integration and Continuous Deployment

We currently use [TravisCI](https://travis-ci.org/intermine/bluegenes) to build BlueGenes.

## Testing

Right now it tests for:

 - a completed minified build
 - correctly indented and formatted code. If your build fails this step, run `lein cljsfmt` and recommit your code.
 - STILL NEEDED: comprehensive unit tests, UI tests, integration tests.

## Deployment

 - BlueGenes [API Docs](api-docs.md) automatically build and are deployed to [intermine.github.io/bluegenes](https://intermine.github.io/bluegenes). 
