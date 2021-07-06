describe("Upload Test", function(){
    beforeEach(function(){
        cy.visit("/biotestmine/upload");
    });

    it("can load free text input", function(){
        cy.get(".wizard").find("textarea").type("ABRA, CRK2",{delay:100});
        cy.contains("Continue").click();
        cy.url().should("include","/save");
    });

    it("can upload and save list", function(){
        cy.get(".wizard").find("textarea").type("ABRA, CRK2, CDPK1, CDPK4",{delay:100});
        cy.contains("Continue").click();
        cy.url().should("include","/save");
        cy.contains("Save List").click();
        cy.url().should("include","/results");

        cy.openListsTab();
        cy.url().should("include","/lists");
        cy.get(".lists-item").its('length').should("be.gt", 0);
    });
    
    it("can load a file and save list", function(){
        const filePath = 'gene.csv';
        cy.contains("File Upload").click();
        cy.contains("Browse").click();
        cy.get('input[type="file"]').attachFile(filePath);
        cy.contains("Continue").click();

        cy.url().should("include","/save");
        cy.contains("Save List").click();
        cy.url().should("include","/results");
    });
})