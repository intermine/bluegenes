describe("Regions Test", function(){
    beforeEach(function(){
        cy.visit("/biotestmine/regions");
    });

    it("can search a gene region given by example", function(){

        cy.get(".input-section").within(() => {
            cy.contains("Show Example").click();
            cy.get("textarea").should("include.text","MAL");
            cy.get("button").filter(':contains("Search")').click();
        })

        cy.get(".results").within(() => {
            cy.get(".single-feature").its("length").should("be.gt",0);
            cy.contains("exon.46111").click();
        })
        
        cy.url().should("include","/report");
    });

    it("can type in chromosome coordinates and select features", function(){

        cy.get(".input-section").within(() => {
            cy.get("textarea").type("MAL1:0..100000",{delay:100});
            cy.get('button[class="btn dropdown-toggle"]').click();
            cy.contains("P. falciparum 3D7").click();
            cy.contains("Features to include").children().click();
            cy.contains("Gene").children().click();
            cy.get("button").filter(':contains("Search")').click();
        })

        cy.get(".results").within(() => {
            cy.get(".single-feature").its("length").should("be.gt",0);
            cy.contains("Next").click();
            cy.get(".single-feature").its("length").should("be.gt",0);
            cy.get(".single-feature").first().click();
        })

        cy.url().should("include","/report");
    });
})
