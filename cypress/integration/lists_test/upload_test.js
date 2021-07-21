describe("Upload Test", function(){
    beforeEach(function(){
        cy.visit("/biotestmine/upload");
    });

    it("can load free text input", function(){
        cy.get(".wizard").find("textarea").type("ABRA, CRK2",{delay:100});
        cy.contains("Continue").click();
        cy.url().should("include","/save");
    });

    it("can select list type and organism", function(){

        cy.get('.select-organism-and-list-type').within(() => {
            cy.contains("List type").siblings(".form-group").find("select").select("Exon");
            // cy.contains("Organism").siblings(".organism-selector").find("select").select("Any");
        })
        cy.get('.select-organism-and-list-type > :nth-child(2) > .form-group > .form-control').select("Any"); //to rewrite
        cy.get('#caseSensitiveCheckbox').check();

        cy.get(".wizard").find("textarea").type("exon.46309,exon.46313",{delay:100});
        cy.contains("Continue").click();
        cy.url().should("include","/save");
    })

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

    it("can reset to cancel upload", function(){
        cy.get(".wizard").find("textarea").type("ABRA, CRK2, CDPK1, CDPK4",{delay:100});
        cy.contains("Reset").click();
        cy.get(".wizard").find("textarea").should("have.value","");
    })
})