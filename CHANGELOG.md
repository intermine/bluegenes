## 1.4.5

- Update results table with expanded export modal functionality to support wider range of formats, preview and modifying contents

## 1.4.4

- Update Google Analytics from Universal Tracking (will be phased out July 2023) to GA4

## 1.4.3

- Handle constraining attributes of type boolean
- Allow constraining with custom value when possible values
- Fix unable to set idresolver options when linking in
- Display tabs on templates when public or owned by user
- Make list authorized/public tabs both visible, and fix filtering
- Homepage templates: Fallback if none are ranked
- Collapse the arrows in homepage template names
- Only enable oauth login when on default mine
- Use summaryfields when displaying search result attributes
- Report page: Do not use attribute of ref/coll of class for title
- Update to result tables
    - Make column summary shorter in height to avoid buttons ending up off-screen on smaller monitors
    - Make cell an external link if it's a URL

## 1.4.2

- Make mine logo and name prominent in header, moving Bluegenes logo to the credits section at the bottom of the home page
- Ensure template constraints are removed from template queries displayed on report pages
- Reduce p-value exponential decimal places to 2 on enrichment results displayed on results page
- Improve support for HTTP clients using POST portal.do
- Fix incorrect counts when saving a list from some templates or custom query results [#797](https://github.com/intermine/bluegenes/issues/797)
    - This would also lead to additional items in the saved list, not present in the original result table
    - These additional items would also be passed onto visualisation tools

## 1.4.1

- Fix various crashes that occur when connected to a mine without any preferredbagtypes in model

## 1.4.0

- Improvements and bugfixes for query builder
    - Use displayName for classes in Data Browser
    - More clearly convey empty classes in Data Browser
    - Do not show empty classes in QB root class dropdown
    - Distinguish reverse references by showing them top of list with an arrow
    - Fix outer join not being removed when class with it is removed from view
    - Fix crash caused by adding or summarising descendent of class with type/subclass constraint
    - Rename *Summary* buttons to *Add summary*
- Open first extant tab in upload page save step when matches tab isn't present
- Make Back To Template button visible on results page after running template, return to templates page with the template still active
- Autofocus search input on search page
- Add clear button to region search page
- Re-introduce old portal.do [linking in API](https://intermine.readthedocs.io/en/latest/webapp/linking-in/#list-of-identifiers) for directly opening upload page with specified IDs resolved

## 1.3.0

- Support for creating templates from query builder page
- More features on templates page
    - Edit template button, sending you to the query builder with your template loaded
    - Reset button to set constraints back to default
    - Delete button with option to undo deletion from alert (**REQUIRES INTERMINE 5.0.4+**)
    - Web service URL when viewing a template
    - Improved fuzziness of template text filter and visual look of the filter panel
    - New filter for showing templates owned by the logged in user (**REQUIRES INTERMINE 5.0.4+**)
- Admin page has been split into 3 sections: report, home and templates (new)
    - *Manage templates* component (**REQUIRES INTERMINE 5.0.4+**) gives you an overview of the templates you own, with the option to
        - view a template on the templates page
        - export a template to XML
        - precompute a template
        - summarise a template
        - edit tags of a template
        - export multiple templates to XML (using checkboxes)
        - delete multiple templates (using checkboxes)
    - *Import templates* component allows you to paste in the XML of previously exported templates, and add them to your account
- Customise BG home page and footer using `web.properties` (see [#765](https://github.com/intermine/bluegenes/pull/765) or [documentation](http://intermine.org/im-docs/)) (**REQUIRES INTERMINE 5.0.4+**)
- Able to set custom non-root deploy path with `:bluegenes-deploy-path` in [config](https://github.com/intermine/bluegenes/blob/dev/docs/configuring.md)
- Able to use a different service root for BG backend with `:bluegenes-backend-service-root` in [config](https://github.com/intermine/bluegenes/blob/dev/docs/configuring.md)
- New 404 catch-all page replacing the Jetty error page
- Use only summary fields instead of union of summary fields and all attributes of class, for report page summary
- Much less verbose and more readable errors when BG is started with an invalid config/envvars
- Better error messages when something goes wrong when using Bluegenes tool store
- Limit length of report summary text with *show more* button
- Fix recently created column for list constraint not working properly
- Fix crash when receiving invalid bg-properties layout.report (for customised report layouts)
- Fix report page layout config unable to grow beyond a specific size (**REQUIRES INTERMINE 5.0.4+**)

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
