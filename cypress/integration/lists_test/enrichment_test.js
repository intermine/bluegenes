Cypress.on('uncaught:exception', (err, runnable) => {
    if (err.message.includes('Can not create edge')) {
        // Cytoscape.js tools are throwing this - ignore for now.
        return false
    }
})

describe("Enrichment Test", function(){
    beforeEach(function(){
        cy.visit("/");
        cy.createListFromFile("enrichment_gene.csv","Enrichment analysis genes");
    });

    // Viewing and downloading enrichment results
    it("can view single enrichment result", function(){
        cy.get(".enrichment").should("exist").within(() => {
            cy.get(".enrichment-filter").within(() => {
                cy.get("select").select("molecular_function");
            })
            cy.contains("cell adhesion molecule binding").click();
        })
        cy.get(".pagination-label").should("include.text","4 rows");
    })

    it("can view and download all enrichment results", function(){
        cy.get(".enrichment").should("exist").within(() => {
            cy.get(".enrichment-category").eq(0).within(() => { 
                cy.get(".icon-download").click();
            })
            cy.get(".enrichment-header").should("exist").within(() => {
                cy.get('[type="checkbox"]').check();
                cy.wait(500);
                cy.get('[type="checkbox"]').should("be.checked");
            })
            cy.contains("View").click();
        })
        cy.get('.pagination-label').should("include.text","172 rows");
        cy.readFile("cypress/downloads/Enrichment analysis genes Gene Ontology Enrichment.tsv").then(newResult => {
            cy.readFile("cypress/fixtures/all_enrichment_result.tsv").should("eq",newResult);
        })
    })

    it("can view and download checked enrichment results", function(){
        cy.get(".enrichment").should("exist").within(() => {
            cy.get('[type="checkbox"]').eq(1).check();
            cy.get('[type="checkbox"]').eq(2).check();
            cy.get('[type="checkbox"]').eq(1).should("be.checked");
            cy.get('[type="checkbox"]').eq(2).should("be.checked");
            cy.get(".enrichment-category").eq(0).within(() => {
                cy.get(".icon-download").click();
            })
            cy.contains("View").click();
        })
        cy.get('.pagination-label').should("include.text","22 rows");
        cy.readFile("cypress/downloads/Enrichment analysis genes Gene Ontology Enrichment.tsv").then(newResult => {
            cy.readFile("cypress/fixtures/checked_enrichment_result.tsv").should("eq",newResult);
        })
    })

    // Applying enrichment filters
    it("can select the test correction method", function(){
        cy.get(".enrichment").should("exist").within(() => {
            cy.intercept("POST","/biotestmine/service/list/enrichment").as("enrichmentLoad");
            cy.get(".correction").find("select").select("Benjamini Hochberg");
            cy.wait("@enrichmentLoad");
            cy.get(".enrichment-p-value").eq(0).should("have.text","1.78e-7");
        })
    })

    it("can select the p-value", function(){
        cy.get(".enrichment").should("exist").within(() => {
            cy.intercept("POST","/biotestmine/service/list/enrichment").as("enrichmentLoad");
            cy.contains("Max p-value").parent().find("select").eq(0).select("1.00");
            cy.wait("@enrichmentLoad");
            cy.contains("Gene Ontology Enrichment").should("include.text","(21)");
        })
    })

    it("can filter enrichment results by ontology", function(){
        cy.get(".enrichment").should("exist").within(() => {
            cy.intercept("POST","/biotestmine/service/list/enrichment").as("enrichmentLoad");
            cy.get(".enrichment-filter > div > select").select("molecular_function");
            cy.wait("@enrichmentLoad");
            cy.contains("Gene Ontology Enrichment").should("include.text","(1)");
        })
    })

    it("can filter enrichment results by text", function(){
        cy.get(".enrichment").should("exist").within(() => {
            cy.intercept("POST","/biotestmine/service/list/enrichment").as("enrichmentLoad");
            // The typing in command has many workarounds here because it's super flaky.
            cy.get(".text-filter > .form-control").type("x");
            cy.get(".text-filter > .form-control").click().wait(500).clear().type("symbiosis{enter}",{delay:200}); //very flaky
            cy.wait("@enrichmentLoad");
            cy.get(".enrichment-item").eq(0).should("include.text","symbiosis");
        })
    })

    it("can select background population to analyze against", function(){
        cy.createListFromFile("background_genes.csv","Background genes");
        cy.contains("List").click();
        cy.url().should("include","/lists");
        cy.get(".lists-item").find(".list-title").eq(1).click();
        cy.intercept("POST","/biotestmine/service/query/results").as("resultsLoad");
        cy.url().should("include","/results");
        cy.wait("@resultsLoad");
        cy.get(".enrichment").should("exist").within(() => {
            cy.get(".dropdown").click();
            cy.get(".list-selection").eq(1).click();
        })
        cy.intercept("POST","/biotestmine/service/list/enrichment").as("enrichmentReq");
        cy.wait('@enrichmentReq').its('response.statusCode').should('eq', 200);
    })

    it("returns an error if the background population doesn't contain a gene in the list", function(){
        cy.createListFromFile("background_genes.csv","Background genes");
        cy.contains("List").click();
        cy.url().should("include","/lists");
        cy.get(".lists-item").find(".list-title").eq(0).click();
        cy.intercept("POST","/biotestmine/service/query/results").as("resultsLoad");
        cy.url().should("include","/results");
        cy.wait("@resultsLoad");
        cy.get(".enrichment").should("exist").within(() => {
            cy.get(".dropdown").click();
            cy.get(".list-selection").eq(1).click();
        })
        cy.get('.enrichment-settings > .alert > p').should("include.text","One or more of the Genes in this list are missing from your background population. The background population should include all Genes that were tested as part of your experiment.")
    })

    // Other elements on the page
    it("shows widgets and tools for list analysis", function(){
        cy.get(".widget-heading").should("have.text","Widgets");
        cy.get(".widget").its("length").should("be.gt",0);
        cy.get(".results-heading").last().should("have.text","Tools");
        cy.get(".report-item").its("length").should("be.gt",0);
    })
})
