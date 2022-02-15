describe("Model browser test", function() {
    beforeEach(function() {
      cy.visit("/biotestmine/querybuilder");
    });

    // Model browser
    it("can select data type and summary attributes with model browser", function() {
      cy.get(".model-browser-root").within(() => {
        cy.selectFromDropdown("Protein");
        cy.contains("Add summary").click();
      });
    });

    it("can select specific attributes", function(){
      cy.get(".model-browser-root").within(() => {
        cy.selectFromDropdown("Gene");        
        cy.get("select").should("have.value", "Gene");
        cy.contains("Add summary").click();
      });

      cy.get(".model-browser").within(() => {
        cy.get("ul").its("length").should("be.gt", 0);
        cy.get(".qb-class").contains("UTRs").click();
        cy.get('.expanded-group > ul').find('.qb-label').filter(':contains("Name")').click();
        cy.get('.expanded-group > ul').find('.qb-label').filter(':contains("Length")').click();
        cy.get('.expanded-group > ul').find('.qb-label').filter(':contains("DB identifier")').click();
      });

      cy.get(".query-view-column").within(() => {
        cy.get('.qb-label').should('include.text','Name');
        cy.get('.qb-label').should('include.text','Length');
        cy.get('.qb-label').should('include.text','DB identifier');
      });
    });

    it("can clear selected attributes on model browser", function(){
      cy.get(".model-browser-column").within(() => {
        cy.selectFromDropdown("Protein");        
        cy.get("select").should("have.value", "Protein");
        cy.contains("Add summary").click();
        cy.get('[title="Remove all selected attributes"]').click();
      });
    });

    //Data browser
    it("allows selection of data type from data model", function(){
      cy.get(".model-browser-column > h4").should("include.text","Model Browser");
      cy.contains("Data Model").click();
      cy.contains("Gene").click();
      cy.contains("Add summary").click();

      cy.get(".query-view-column").within(() => {
        cy.get(".qb-label").should("include.text","Gene");
      })
    });

    it("can return to model browser from data browser", function(){
      cy.get(".model-browser-column > h4").should("include.text","Model Browser");
      cy.contains("Data Model").click();
      cy.get(".header-group").within(() => {
        cy.get("h4").should("include.text","Data Browser");
        cy.contains("Back to query").click();
      })
      cy.get(".model-browser-column > h4").should("include.text","Model Browser");
    });
});

