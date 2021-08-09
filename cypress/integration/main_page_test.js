describe("Main page test", function() {
    beforeEach(function() {
      cy.visit("/");
    });

    it("can browse the data sources from the main page", function() {
        cy.contains("Browse sources").click();
        cy.url().should("include","/results");
        cy.get(".im-table").should("exist");
    })

    it("can access upload tab from the navigation bar", function() {
        cy.get("#bluegenes-main-nav").within(() => {
            cy.contains("Upload").click();
        });
        cy.url().should("include", "/upload");
    });

    it("can access lists tab from the navigation bar", function() {
        cy.get("#bluegenes-main-nav").within(() => {
            cy.contains("Lists").click();
        });
        cy.url().should("include", "/lists");
    });

    it("can access templates tab from the navigation bar", function() {
        cy.get("#bluegenes-main-nav").within(() => {
            cy.contains("Templates").click();
        });
        cy.url().should("include", "/templates");
    });

    it("can access regions tab from the navigation bar", function() {
        cy.get("#bluegenes-main-nav").within(() => {
            cy.contains("Regions").click();
        });
        cy.url().should("include", "/regions");
    });

    it("can access query builder tab from the navigation bar", function() {
        cy.get("#bluegenes-main-nav").within(() => {
            cy.contains("Query Builder").click();
        });
        cy.url().should("include", "/querybuilder");
    });

    it("can access templates from the main page", function() {
        cy.contains("More queries here").click();
        cy.url().should("include", "/templates");
    });

    it("can access upload from the main page", function() {
        cy.contains("Analyse data").click();
        cy.url().should("include", "/upload");
    });

    it("can access query builder from the main page", function() {
        cy.contains("Build your own query").click();
        cy.url().should("include", "/querybuilder");
    });
    
    it("can access other intermine instances", function() {
        cy.get(".minename > .dropdown-toggle").click();
        cy.contains("FlyMine").click();
        cy.url().should("contain","/flymine");
    });
});