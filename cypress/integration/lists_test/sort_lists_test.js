describe("Sort Lists Test", function() {
    beforeEach(function() {
      cy.visit("/");
      cy.createGeneList("SODB, GBP, GST, CDPK1");
      cy.createProteinList("Q8ID23_PLAF7, Q6LFN1");
      cy.url().should("include","/lists");
      cy.get(".lists-item").should('have.length',2);
    });

    it("can sort lists by name in ascending order", function(){
        cy.get(".lists-headers").children().eq(1).as("listDetailsSection").within(() => {
            cy.get(".icon-sort").click();
            cy.get(".active-asc-sort").should("exist");
        })
        cy.get(".list-title").eq(0).should("include.text","Gene list");
    })

    it("can sort lists by name in descending order", function(){
        cy.get(".lists-headers").children().eq(1).as("listDetailsSection").within(() => {
            cy.get(".icon-sort").dblclick();
            cy.get(".active-desc-sort").should("exist");
        })
        cy.get(".list-title").eq(0).should("include.text","Protein list");
    })

    it("can sort lists by date in ascending order", function(){
        cy.get(".lists-headers").children().eq(2).as("dateSection").within(() => {
            cy.get(".icon-sort").click();
            cy.get(".active-asc-sort").should("exist");
        })
        cy.get(".list-title").eq(0).should("include.text","Gene list");
    })

    it("can sort lists by date in descending order", function(){
        cy.get(".lists-headers").children().eq(2).as("dateSection").within(() => {
            cy.get(".icon-sort").dblclick();
            cy.get(".active-desc-sort").should("exist");
        })
        cy.get(".list-title").eq(0).should("include.text","Protein list");
    })

    it("can sort lists by type in ascending order", function(){
        cy.get(".lists-headers").children().eq(3).as("typeSection").within(() => {
            cy.get(".icon-sort").click();
            cy.get(".active-asc-sort").should("exist");
        })
        cy.get(".list-title").eq(0).should("include.text","Gene list");
    })

    it("can sort lists by type in descending order", function(){
        cy.get(".lists-headers").children().eq(3).as("dateSection").within(() => {
            cy.get(".icon-sort").dblclick();
            cy.get(".active-desc-sort").should("exist");
        })
        cy.get(".list-title").eq(0).should("include.text","Protein list");
    })
});