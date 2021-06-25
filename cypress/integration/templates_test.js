describe("Templates Test", function() {
    beforeEach(function() {
      cy.visit("/");
    });

    it("can be accessed from the navigation tab", function() {
        cy.openTemplatesTab();
        cy.url().should("include", "/templates");
    });

    it("can be accessed from the main page", function() {
        cy.contains("More queries here").click();
        cy.url().should("include", "/templates");
    });

    it("can filter templates by search", function() {
        cy.openTemplatesTab();
        cy.url().should("include", "/templates");
        cy.get('input[class="form-control input-lg"]').type("Organism{enter}",{delay:100});
        cy.get('#Organism_Protein').should("have.id","Organism_Protein");
    });

    it("can search templates by categories", function() {
        cy.openTemplatesTab();
        cy.url().should("include","/templates");
        cy.get("div[class=grid-1]").should("have.length","3");
        cy.contains("Genomics").click();
        cy.get("div[class=grid-1]").should("have.length","1");
    });

    it("can load predefined template and show results", function() {
        cy.openTemplatesTab();
        cy.url().should("include","/templates");

        cy.contains("View").first().click();
        cy.url().should("include","/All_Proteins_In_Organism_To_Publications");
        cy.get('#All_Proteins_In_Organism_To_Publications').within(() => {
            cy.get('select').first().select('!=');
            cy.get('td').should('include.text','Query returned no results');

            cy.contains('Open in results page').click();
            cy.url().should("include","/results");
        });
    });
});