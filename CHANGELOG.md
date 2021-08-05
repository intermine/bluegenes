## 1.2.1

- Fix authentication errors with 5XX response codes not being handled

## 1.2.0

- Cache busting for Tools' dist files (browser should immediately catch up when a tool is updated)
- Improved RDF support **REQUIRES INTERMINE TBD**
    - Option to export results in N-triples and RDF
    - Support RDF representation for permanent URLs
    - Static HTML header link to RDF representation in report pages
- Support downloading of enrichment results
- Vastly improved region search page
    - Fix features missing in results when fully encompassing search region
    - Graph showing location of features relative to min and max loci of search region
    - Show symbols for features
    - Buttons to export results of one or all search regions in various formats, including opening the features in an im-table
    - Dropdown to create list by feature type, for one or all search regions
    - Interbase coordinates
    - Strand-specific search
    - Specify strand explicitly using `chormosome:start:end:strand` notation
    - Sliders to extend search regions on one or both sides
    - Restrict organism dropdown to only those that have chromosome data
    - Use configured default organism for organism dropdown initial value
- Show mine release version on home page below mine name
- Restrict Other Mines section on gene report pages to only show homologues for main organisms of mine (uses organisms saved for mine on InterMine registry)
- Show strand in summary for sequence feature report pages (includes descendents, e.g. gene)
- Show mines incompatible with BlueGenes as external links, which means
    - registry mines below API version 27
    - registry mines using unsecured HTTP when BlueGenes is hosted on HTTPS
    - *additional mines* (specified in *config.edn*) with `:external true`
- Support specifying a `description` in the *config.json* for a Tool, shown in info popover in header of tool (can be overridden by setting a description in the report page layout editor)

## 1.1.0

- Add missing enrichment features to panel on results page
    - Specifying list as background population **REQUIRES INTERMINE 5.0.2+**
    - Filters specific to an enrichment widget
- Display chart and table widgets on results page **REQUIRES INTERMINE 5.0.2+**
- Make results page layout more space efficient by moving recent queries panel into a dropdown

## 1.0.1

- Fix BlueGenes version number in footer showing as "dev" for a build from the exact commit with annotated tag
- Support specifying a favicon, either from default mine or by adding one
