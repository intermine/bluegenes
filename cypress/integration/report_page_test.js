describe("Report Page Test", function(){
    beforeEach(function(){
        cy.searchKeyword("GST");
        cy.get(".result").click();
        cy.url().should("include","/report");
        cy.get('.report-page-heading').should("exist");
    });

    it("can filter the topic of a search result", function(){
        cy.get(".report-page-filter").find("input").type("Publications{enter}",{delay:100});
        cy.get('.text-highlight').should("include.text","Publications").click();
        cy.get('.report-item-title').should("include.text","Publications");
        cy.get('.im-table').should("exist");
        cy.isInViewport('.im-table');
    });

    it("can access a topic from the left sidebar", function(){
        cy.get(".toc").within(() => {
            cy.contains("Data").click();
            cy.contains("Exons").click();
        })

        cy.get('.report-item-title').contains("Exons")
        .parent().parent(".report-item")
        .as('exonsTable');
        cy.wait(100); 
        cy.isInViewport("@exonsTable");
    });

    it("can collapse and expand a section on report page", function(){
        cy.get('.report-table-heading').contains("Data")
        .parent(".report-table")
        .as('dataSection');

        cy.get("@dataSection").within(() => {
            cy.get(".report-item-title").should("exist");
            cy.get(".icon-chevron-up").click();
            cy.get(".report-item").should("not.exist");
        })
    });

    it("can perform a region search with its sequence", function(){
        cy.get(".report-table").should("have.id","summary").within(() => {
            cy.get('.report-table-link > span').click();
        })
        cy.url().should("include","/regions");
    });

    it("can view the sequence of a gene", function(){
        cy.get(".report-table").should("have.id","summary").within(() => {
            cy.get('.fasta-value > .dropdown').click();
        })
        cy.isInViewport(".fasta-sequence");
    });

    it("can download the sequence of a gene in FASTA format", function(){
        cy.get(".report-table").should("have.id","summary").within(() => {
            cy.get('.fasta-value > .fasta-download').click();
        })
        cy.readFile('cypress/downloads/PF14_0187.fasta').should('contain', 'PF14_0187');
    });

    it("can expand a section to view the data displayer", function(){
        cy.get('.report-table-heading').contains("Data")
        .parent(".report-table").find(".report-item").filter(':contains("Exons")')
        .as('exonsSection');

        cy.get("@exonsSection").within(() => {
            cy.get('.im-table').should("exist");
            cy.get('.report-item-toggle').click();
            cy.get('.im-table').should("not.exist");
        })
    });

    
    it("can copy the permanent URL of the report page of the object", function(){
        cy.get(".visible-md-block").within(() => {
            cy.contains("Copy permanent URL").click();
            cy.get(".permanent-url-container").should("be.visible").within(() => {
                cy.get("input.form-control").should("be.visible").invoke('val').should("include","http://localhost:9999/biotestmine/gene:PF14_0187");
                cy.get("button").contains("Copy").should("be.visible").click({multiple:true});
            });
        })
        // cy.wait(500);
        // cy.task("getClipboard").should("contain","/biotestmine/gene:PF14_0187");
    });

    it("can check if the gene is present in any list", function(){
        cy.get(".sidebar-entry").filter(':contains("Lists")').filter(':visible').as('listsSection');
        cy.get("@listsSection").within(() => {
            cy.get("p").should("contain","This Gene isn't in any lists.");
        })
        cy.createGeneList('CK1,ABRA,GST');
        cy.get(".typeahead-search").clear().type("GST{enter}",{delay:100});    
        cy.get(".result").click();
        cy.url().should("include","/report");

        cy.get("@listsSection").within(() => {
            cy.get("a").first().click();
        })   
        cy.wait(500);
        cy.url().should("include","/results");
        cy.get('.query-title').should("include.text","Gene list");
    });
})