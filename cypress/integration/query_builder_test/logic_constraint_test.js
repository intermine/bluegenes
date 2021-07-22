describe("Query view test", function() {
    beforeEach(function() {
      cy.visit("/biotestmine/querybuilder");
    });
    
    it("can add length constraint on query editor", function(){
      cy.get(".model-browser-root").within(() => {
       cy.selectFromDropdown("Protein");        
       cy.get("select").should("have.value", "Protein");
       cy.contains("Summary").click();
     });
   
     cy.get(".query-view-column").within(() => {
       cy.contains('Length').parentsUntil('.lab qb-class').children('.icon.icon-filter').click();
       cy.get("select").select(">=");
       cy.wait(500);
       cy.get("input.form-control").click().type("450{enter}",{delay:100});
     });
   });

    it("can add logic constraint on query editor", function(){
      cy.get(".model-browser-root").within(() => {
        cy.selectFromDropdown("Gene");        
        cy.get("select").should("have.value", "Gene");
        cy.contains("Summary").click();
      });

      cy.get(".query-view-column").within(() => {
        cy.get('.qb-label').should('include.text',"Gene");
        cy.contains('Gene').parents('.qb-label').siblings('.icon-filter').click();
        cy.get('select').select('Not in list');
        cy.get('select').should('have.value','NOT IN');

        cy.contains('DB identifier').parentsUntil('.lab qb-class').children('.icon.icon-filter').click();
        cy.get('select').last().select('Contains').should("have.value","CONTAINS");
        cy.get('input.form-control').click().type("MAL{enter}",{delay:100});
      })
    });

    it("can change the constraint to outer join", function(){
      cy.get('.model-browser-root').within(() => {
          cy.selectFromDropdown("Gene");
          cy.contains("Summary").click();
      });

      cy.get(".query-view-column").within(() => {
          cy.get('.outer-join-button').click();
          cy.get('.joins-list').should("exist");
      })
  })

  it("can edit constraint logic", function(){
      cy.get('.model-browser-root').within(() => {
          cy.selectFromDropdown("Gene");
          cy.contains("Summary").click();
      });
      cy.get('.model-browser').within(() => {
          cy.get(".qb-class").filter(':contains("Protein")').siblings(".label-button").click();
      });

      cy.get(".query-view-column").within(() => {
          cy.contains("Gene").parentsUntil('.lab qb-class').children('.icon.icon-filter').click();
          cy.get("input.form-control").eq(0).type("MA*{enter}",{delay:100});
          
          cy.contains("Protein").parentsUntil('.lab qb-class').children('.icon.icon-filter').click();
          cy.get("input.form-control").eq(1).type("MA*{enter}",{delay:100});

          cy.get(".logic-container").should("exist");
          cy.get("#logic-input").click();
          cy.get("input.form-control").eq(2).clear().type("{selectall}{backspace}A or B{enter}",{delay:100});
      })
  })

  it("can add NULL as a constraint in search results", function(){
      cy.get('.model-browser-root').within(() => {
          cy.selectFromDropdown("Gene");
          cy.contains("Summary").click();
      });

      cy.get(".query-view-column").within(() => {
          cy.contains("DB identifier").parentsUntil('.lab qb-class').children('.icon.icon-filter').click();
          cy.get('select.constraint-chooser').eq(0).select('!=');
          cy.wait(500);
          cy.get('.css-1hwfws3').click().type("PFF1360w{enter}",{delay:100});
          
          cy.contains("DB identifier").parentsUntil('.lab qb-class').children('.icon.icon-filter').click();
          cy.get('select.constraint-chooser').eq(1).select('Null');

          cy.get(".logic-container").should("exist");
          cy.get("#logic-input").click();
          cy.get("input.form-control").clear().type("{selectall}{backspace}A or B{enter}",{delay:100});
      })
  })
});
  