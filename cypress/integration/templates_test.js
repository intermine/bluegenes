describe("Templates Test", function() {
    beforeEach(function() {
      cy.visit("/biotestmine/templates");
    });

    it("can filter templates by search", function() {
        cy.contains('Filter by description').next().type("Organism{enter}",{delay:100});
        cy.get('.template').should("have.id","Organism_Protein");
    });

    it("can search templates by categories", function() {
        cy.get(".template").should("have.length","3");
        cy.contains("Genomics").click();
        cy.get(".template").should("have.length","1");
    });

    it("can load predefined template and show results", function() {
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