describe("Upload Test", function(){
    beforeEach(function(){
        cy.visit("/");
    });

    it("can be accessed from navigation bar", function(){
        cy.get("#bluegenes-main-nav").within(() => {
            cy.contains("Upload").click();
        });
        cy.url().should("include","/upload");
    });

    it("can be accessed from the main page", function(){
        cy.contains("Analyse data").click();
        cy.url().should("include","/upload");
    });

    it("can load free text input", function(){
        cy.visit("/biotestmine/upload");

        cy.get(".wizard").find("textarea").type("ABRA, CRK2",{delay:100});
        cy.contains("Continue").click();
        cy.url().should("include","/save");
    });

    it("can upload and save list", function(){
        cy.visit("/biotestmine/upload");

        cy.get(".wizard").find("textarea").type("ABRA, CRK2, CDPK1, CDPK4",{delay:100});
        cy.contains("Continue").click();
        cy.url().should("include","/save");
        cy.contains("Save List").click();
        cy.url().should("include","/results");

        cy.openListsTab();
        cy.url().should("include","/lists");
        cy.get(".lists-item").its('length').should("be.gt", 0);
    });
})