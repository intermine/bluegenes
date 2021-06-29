describe("Regions Test", function(){
    beforeEach(function(){
        cy.visit("/biotestmine/regions");
    });

    it("can search a gene region given by example", function(){
        cy.contains("Show Example").click();
        cy.get(".form-control").should("include.text","MAL");

        cy.get('button[class="btn dropdown-toggle"]').click();
        cy.contains("P. falciparum 3D7").click();
        
        cy.contains("Features to include").children().click();
        cy.contains("Exon").children().click();

        cy.get("button").filter(':contains("Search")').click();
        cy.get('.allresults').children().should("have.length",3);

        cy.contains("exon.46111").click();
        cy.url().should("include","/report");
    });
})