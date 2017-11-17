There is a minimal server setup to allow the data browser to work.
It is set up to expect a localhost:6379 redis already running, perhaps via

```docker run -it --link bootcut --rm redis -p 6379:6379```

This command should create a redis install with the correct port open, assuming you have docker.

The following endpoints exist:

### Caching model counts
#### Why?
Because it can take several seconds to return results for a count depending on the mine and query depth.

#### How?
There is a script to automatically grab the correct url for known mines from `bluegenes/src/cljc/mines.cljc`, go to the mine(s), and grab top level whitelisted model counts.  The whitelist at `bluegenes/src/cljc/whitelist.cljc` exists for two reasons:
1. There is a need for a simplified model for the data browser
2. The model is recursive. Simplifying things makes it harder to create endless loops and make the world explode.

That said, it would be easy to expand the methods to cache counts for non-whitelisted properties.

One day this will be done via batch job, but right now we have to generate the caches manually. Here's how:

#### Caching a single mine

```GET http://www.whateverserver.com:3449/api/model/count/cache?mine=mine-name ```
 where mine-name might be fly, e.g.  
```GET http://www.whateverserver.com:3449/api/model/count/cache?mine=fly```

There must be a valid `:fly` mine configuration under `bluegenes/src/cljc/mines.cljc` for this to work.

#### Caching all known mines in the mines.cljc file:

```GET http://www.whateverserver.com:3449/api/model/count/cacheall```

This'll grab all the ones you need. Want another? add it to the file. simples.

### Accessing model counts
Awesome, so they're cached! How do I get the details?

#### Give me the counts for all top level whitelisted model entities, please!

Sure! here you go.

```GET http://www.whateverserver.com:3449/api/model/count?mine=zebrafish&paths=top```

Output:

```{
"Homologue": "1834894",
"DataSet": "53",
"Protein": "53932",
"Gene": "133238",
"GOAnnotation": "193393",
"Organism": "7",
"Publication": "25391",
"Author": "0",
"GOTerm": "43531"
}```

#### How about just the specific paths I need?

```GET http://www.whateverserver.com:3449/api/model/count?mine=rat&paths=Gene,Protein,Gene.interactions,Author```

Output:

```
{
"Gene": "205319",
"Protein": "81949",
"Gene.interactions": "117269",
"Author": "938688"
}
```

#### Neat! Can I get all the children of my selected path?

Yep!

```GET http://www.whateverserver.com:3449/api/model/count/children?mine=yeast&path=Gene```

Output:

```{
"Gene.goAnnotation": "93036",
"Gene.homologues": "981242",
"Gene.dataSets": "25",
"Gene": "239245",
"Gene.proteins": "6804",
"Gene.publications": "70349",
"Gene.interactions": "680235"
}```
