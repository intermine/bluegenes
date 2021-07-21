describe("Advanced query builder test", function() {
    beforeEach(function() {
      cy.visit("/biotestmine/querybuilder");
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