describe("Single List Test", function() {
    beforeEach(function() {
      cy.visit("/biotestmine/upload");
      cy.createGeneList("SODB, GBP, GST, CDPK1");
      cy.contains("Lists").click();
      cy.url().should("include","/lists");
      cy.get('.lists-item').should('have.length',1);
    });

    it("can copy a list", function(){
        cy.get(".lists-item").within(() => {
            cy.get("button").find('svg[class="icon icon-list-more"]')
            .should("be.visible")
            .click();

            cy.get("button").find('svg[class="icon icon-list-copy"]')
            .should("be.visible")
            .first()
            .click();
        })

        cy.get('.modal-footer').within(() => {
            cy.contains("Copy list(s)").click({delay:100});
        })
        cy.get('.lists-item').should('have.length',2);

    });

    it("can rename and add description to a list", function(){
        cy.get(".lists-item").within(() => {
            cy.get("button").find('svg[class="icon icon-list-more"]')
            .should("be.visible")
            .click();

            cy.get("button").find('svg[class="icon icon-list-edit"]')
            .should("be.visible")
            .first()
            .click();
        })

        cy.get('.modal-body').find("textarea").type("List of algae genes",{delay:100});
        cy.contains('Save').click();

        cy.get(".lists-item").within(() => {
            cy.get(".list-description").should("have.text","List of algae genes");
        })       
    });
    it("can delete a list", function(){
        cy.get(".lists-item").within(() => {
            cy.get("button").find('svg[class="icon icon-list-more"]')
            .should("be.visible")
            .click();

            cy.get("button").find('svg[class="icon icon-list-delete"]')
            .should("be.visible")
            .first()
            .click();
        })

        cy.get('.modal-footer').within(() => {
            cy.contains("Delete list(s)").click({delay:100});
        })
        cy.get('.no-lists').should('exist');
    });

    it("can control the number of lists shown", function(){
        cy.get(".pagination-controls").within(() => {
            cy.contains("Rows per page").siblings(".dropdown").as("rowSelect");
            cy.get("@rowSelect").find("li").filter(".active").should("have.text","20");
            cy.get("@rowSelect").find("button").click();
            cy.contains("100").click();
            cy.get("@rowSelect").find("li").filter(".active").should("have.text","100");
        })
    });

    it("has an associated list analysis page and can be analyzed with the enrichment widget",function(){
        cy.get('.list-title').first().click();
        cy.url().should("contain","/results");
        cy.get(".results-table").should("exist");
        cy.get(".enrichment").within(() => {
            cy.contains("Max p-value").parent().find("select").first().select("0.10");
            cy.get('.correction').find("select").select("Benjamini Hochberg");
            cy.get(".dropdown").click();
            cy.get(".list-selection").first().click();
            cy.get('.text-filter').find("input").type("ABRA{enter}",{delay:100});
        })
    })
});