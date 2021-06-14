describe("UI Test", function() {
    beforeEach(function() {
      cy.visit("/");
    });

    it("Query builder allows you to select data type and summary attributes with model browser", function() {
        cy.get("#bluegenes-main-nav").within(() => {
          cy.contains("Query Builder").click();
        });
        cy.url().should("include", "/querybuilder");

        cy.get('.model-browser-root').within(() => {
          cy.get('select').then($option => {$option.val("Protein")}).parent().trigger('change');
          cy.contains("Summary").click();
        });
    });

    it("Query can be saved and loaded", function() {
        cy.get("#bluegenes-main-nav").within(() => {
            cy.contains("Query Builder").click();
        });
        cy.url().should("include", "/querybuilder");

        cy.get('.model-browser-root').within(() => {
          cy.get('select').then($option => {$option.val("Protein")}).parent().trigger('change');
          cy.contains("Summary").click();
        });
 
        cy.get('div.panel-body').within(()=>{
          cy.contains("Save Query").click();
          cy.get("div.input-group").type("Protein summary");
          cy.contains("Save Query").click();
          cy.contains("Clear Query").click();
        });
        
        cy.contains("Saved Queries").click();
        cy.get('table').contains('td','Protein summary'); //Assertion
        cy.contains('Load').click();
    });

    it("Query builder allows you to choose specific attributes", function(){
        cy.get("#bluegenes-main-nav").within(() => {
            cy.contains("Query Builder").click();
          });
        cy.url().should("include", "/querybuilder");

        cy.get('.model-browser-root').within(() => {
          cy.get('select').then($option => {$option.val("Gene")}).parent().trigger('change');
          cy.contains("Summary").click();
        })

        cy.get('.model-browser').within(() => {
          cy.get("ul").its('length').should("be.gt", 0);
          cy.get(".qb-class").contains('UTRs').click();
          cy.get('.expanded-group > ul').find('.qb-label').filter(':contains("Name")').click();
          cy.get('.expanded-group > ul').find('.qb-label').filter(':contains("Length")').click();
          cy.get('.expanded-group > ul').find('.qb-label').filter(':contains("DB identifier")').click();
        })

        cy.get('.query-view-column').within(() => {
          cy.get('.qb-label').should('include.text','Name');
          cy.get('.qb-label').should('include.text','Length');
          cy.get('.qb-label').should('include.text','DB identifier');
        })
    });

    it("Query builder allows selection of data type from data model", function(){
      cy.get("#bluegenes-main-nav").within(() => {
        cy.contains("Query Builder").click();
      });
      cy.url().should("include", "/querybuilder");

      cy.contains("Data Model").click();
      cy.contains("Gene").click();
      cy.contains("Summary").click();

      // cy.get('select').should('have.value', 'Gene');
      cy.get('.query-view-column').within(() => {
        cy.get('.qb-label').should('include.text','Gene');
      })

    })

});
  