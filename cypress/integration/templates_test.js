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

    it("can run a template search with a list", function() {
        cy.createGeneList("ABRA,PFB0815w,PFF1275c,gene.60087");
        cy.contains("Templates").click();
        cy.url().should("contain","/templates");
        cy.get("#Gene_Protein").click().within(() => {
            cy.get("select").select("IN");
            cy.contains("Choose a list").click();

            cy.get(".dropdown-menu").should("exist");
            cy.get(".list-selection").first().click();
            cy.get(".view-results").should("contain","4").click();
        });
        cy.url().should("contain","/results");
    });

    it("can view and close a template", function(){
        cy.contains("View").first().click();
        cy.get(".template").filter(".selected").within(() => {
            cy.contains("Close").click();
        })
        cy.get(".template").filter(".selected").should("not.exist");
    })
    
    it("can edit a templated query", function() {
        cy.get('#All_Proteins_In_Organism_To_Publications').click().within(()=> {
            cy.contains("Edit query").click();
        })
        cy.url().should("contain","/querybuilder");
        cy.get('.query-browser').within(()=>{
            cy.get(".qb-label").should("contain","Protein");
            cy.get(".qb-label").should("contain","Publications");
        })
    })

    it("can lookup a gene with a template search", function() {
        cy.get("#Gene_Protein").click().within(() => {
            cy.get("input").clear().type("MA*{enter}",{delay:100});
            cy.get(".view-results").click();
        })
        cy.url().should("include","/results");
    });
});