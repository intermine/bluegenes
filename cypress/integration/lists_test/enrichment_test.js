Cypress.on('uncaught:exception', (err, runnable) => {
    if (err.message.includes('Can not create edge')) {
        // Cytoscape.js tools are throwing this - ignore for now.
        return false
    }
})

describe("Enrichment Test", function(){
    beforeEach(function(){
        cy.visit("/");
        cy.createListFromFile("all_genes.csv","All genes"); //This file is downloaded from querybuilder.
        cy.createListFromFile("enrichment_gene.csv","Enrichment analysis genes");
        cy.get(".enrichment").should("exist").within(() => {
            cy.get(".dropdown").click();
            cy.get(".list-selection").eq(0).click();
        })
    });

    it("can view and download all enrichment results", function(){
        cy.get(".enrichment").should("exist").within(() => {
            cy.get('.enrichment-header').within(() => { 
                cy.get('[type="checkbox"]').check();
                cy.wait(500);
                cy.get('[type="checkbox"]').should("be.checked"); 
            })
            cy.intercept("POST","/biotestmine/service/query/results").as("tableLoad");
            cy.contains("View").click();
            cy.wait("@tableLoad");
        })
        cy.get('.pagination-label').should("include.text","172 rows");
        cy.get(".enrichment-category").eq(0).within(() => { 
            cy.get(".icon-download").click();
        })
        cy.readFile("cypress/downloads/Enrichment Results Gene Ontology Enrichment.tsv").then(newResult => {
            cy.readFile("cypress/fixtures/all_enrichment_result.tsv").should("eq",newResult);
        })
    })

    it("can view and download single enrichment result", function(){
        cy.get(".enrichment").should("exist").within(() => {
            cy.get('.enrichment-filter').within(() => {
                cy.get("select").select("molecular_function");
            })
            cy.contains("cell adhesion molecule binding").click();
        })
        cy.get('.pagination-label').should("include.text","4 rows");
        cy.get(".enrichment-category").eq(0).within(() => {
            cy.get(".icon-download").click();
        })
        cy.readFile("cypress/downloads/GO_0050839 Gene Ontology Enrichment.tsv").then(newResult => {
            cy.readFile("cypress/fixtures/single_enrichment_result.tsv").should("eq",newResult);
        })
    })
    it("can view and download checked enrichment results", function(){
        cy.get(".enrichment").should("exist").within(() => {
            cy.get('[type="checkbox"]').eq(1).check();
            cy.get('[type="checkbox"]').eq(2).check();
            cy.get('[type="checkbox"]').eq(1).should("be.checked");
            cy.get('[type="checkbox"]').eq(2).should("be.checked");
            cy.contains("View").click();
        })
        cy.get('.pagination-label').should("include.text","22 rows");
        cy.get(".enrichment-category").eq(0).within(() => {
            cy.get(".icon-download").click();
        })
        cy.readFile("cypress/downloads/Enrichment Results Gene Ontology Enrichment.tsv").then(newResult => {
            cy.readFile("cypress/fixtures/checked_enrichment_result.tsv").should("eq",newResult);
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

    it("can select test correction method and filter enrichment results by text", function(){
        cy.get(".enrichment").should("exist").within(() => {
            cy.intercept("POST","/biotestmine/service/list/enrichment").as("enrichmentLoad");
            cy.contains("Max p-value").parent().find("select").eq(0).select("0.10");
            cy.get(".correction").find("select").select("Benjamini Hochberg");
            cy.get(".text-filter > .form-control").type("x");
            cy.get(".text-filter > .form-control").click().wait(500).clear().type("symbiosis{enter}",{delay:100}); //very flaky
            cy.wait("@enrichmentLoad");
            cy.get(".enrichment-item").eq(0).should("include.text","symbiosis");
            cy.get(".enrichment-p-value").eq(0).should("have.text","1.783148e-7");
        })
    })

    it("shows widgets and tools for list analysis", function(){
        cy.get(".widget-heading").should("have.text","Widgets");
        cy.get(".widget").its("length").should("be.gt",0);
        cy.get(".results-heading").last().should("have.text","Tools");
        cy.get(".report-item").its("length").should("be.gt",0);
    })
})