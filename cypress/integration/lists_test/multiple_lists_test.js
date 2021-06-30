describe("Multiple Lists Test", function() {
    beforeEach(function() {
      cy.visit("/");
    });

    it("can combine two lists", function(){
        cy.createGeneList("SODB, GBP, GST, CDPK1");
        cy.url().should("include","/lists");
        cy.get('.lists-item').should('have.length',1);

        cy.createGeneList("CDPK1, CDPK4, ERD2, PFF1575w");
        cy.url().should("include","/lists");
        cy.get('.lists-item').should('have.length',2);

        cy.get('.lists-headers').within(() => {
            cy.get('[type="checkbox"]').check();
        })
        cy.get('.top-controls').within(() => {
            cy.contains("Combine lists").click();
        })
        cy.get('.modal-footer').within(() => {
            cy.contains("Save new list").click();
        })

        cy.contains("Combined Gene List").click();
        cy.url().should("include","/results");
        cy.get('.query-title').should("include.text","Combined Gene List");
        cy.get('.list-size-auth > span').should("include.text","7");
    })   

    it("can intersect two lists", function(){
        cy.createGeneList("SODB, GBP, GST, CDPK1");
        cy.url().should("include","/lists");
        cy.get('.lists-item').should('have.length',1);

        cy.createGeneList("CDPK1, CDPK4, ERD2, PFF1575w");
        cy.url().should("include","/lists");
        cy.get('.lists-item').should('have.length',2);

        cy.get('.lists-headers').within(() => {
            cy.get('[type="checkbox"]').check();
        })
        cy.get('.top-controls').within(() => {
            cy.contains("Intersect lists").click();
        })
        cy.get('.modal-footer').within(() => {
            cy.contains("Save new list").click();
        })

        cy.contains("Intersected Gene List").click();
        cy.url().should("include","/results");
        cy.get('.query-title').should("include.text","Intersected Gene List");
        cy.get('.list-size-auth > span').should("include.text","1");
    })   

    it("can difference two lists", function(){
        cy.createGeneList("SODB, GBP, GST, CDPK1");
        cy.url().should("include","/lists");
        cy.get('.lists-item').should('have.length',1);

        cy.createGeneList("CDPK1, CDPK4, ERD2, PFF1575w");
        cy.url().should("include","/lists");
        cy.get('.lists-item').should('have.length',2);

        cy.get('.lists-headers').within(() => {
            cy.get('[type="checkbox"]').check();
        })
        cy.get('.top-controls').within(() => {
            cy.contains("Difference lists").click();
        })
        cy.get('.modal-footer').within(() => {
            cy.contains("Save new list").click();
        })

        cy.contains("Differenced Gene List").click();
        cy.url().should("include","/results");
        cy.get('.query-title').should("include.text","Differenced Gene List");
        cy.get('.list-size-auth > span').should("include.text","6");
    })   

    it("can subtract two lists", function(){
        cy.createGeneList("SODB, GBP, GST, CDPK1");
        cy.url().should("include","/lists");
        cy.get('.lists-item').should('have.length',1);

        cy.createGeneList("CDPK1, CDPK4, ERD2, PFF1575w");
        cy.url().should("include","/lists");
        cy.get('.lists-item').should('have.length',2);

        cy.get('.lists-headers').within(() => {
            cy.get('[type="checkbox"]').check();
        })
        cy.get('.top-controls').within(() => {
            cy.contains("Subtract lists").click();
        })
        cy.get('.modal-footer').within(() => {
            cy.contains("Save new list").click();
        })

        cy.contains("Subtracted Gene List").click();
        cy.url().should("include","/results");
        cy.get('.query-title').should("include.text","Subtracted Gene List");
        cy.get('.list-size-auth > span').should("include.text","3");
    })   
});