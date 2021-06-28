describe("Upload Test", function(){
    beforeEach(function(){
        cy.visit("/");
    });

    it("can be accessed from navigation bar", function(){
        cy.openUploadTab();
        cy.url().should("include","/upload");
    });
    it("can be accessed from the main page", function(){
        cy.contains("Analyse data").click();
        cy.url().should("include","/upload");
    });

    it("can load free text input", function(){
        cy.openUploadTab();
        cy.url().should("include","/upload");

        cy.get('.identifier-input > .form-group > .form-control').type("ABRA, CRK2",{delay:100});
        cy.contains("Continue").click();
        cy.url().should("include","/save");
    });

    it("can upload and save list", function(){
        cy.openUploadTab();
        cy.url().should("include","/upload");

        cy.get('.identifier-input > .form-group > .form-control').type("ABRA, CRK2, CDPK1, CDPK4",{delay:100});
        cy.contains("Continue").click();
        cy.url().should("include","/save");
        cy.contains("Save List").click();
        cy.url().should("include","/results");

        cy.openListsTab();
        cy.url().should("include","/lists");
        cy.get(".lists-item").its('length').should("be.gt", 0);
    });
})