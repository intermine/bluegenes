describe("Single Lists Test", function() {
    beforeEach(function() {
      cy.visit("/");
      cy.createGeneList("SODB, GBP, GST, CDPK1");
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

    it("can edit a list", function(){
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
});