describe("Search Test", function(){
    beforeEach(function(){
        cy.visit("/biotestmine/search");
    });

    it("can be accessed from navigation bar", function(){
        cy.searchKeyword("Ma*");
        cy.url().should("include","/search?keyword");

        cy.get(".results > form").children().first().click();
        cy.url().should("include","/report");
    });

    it("can search a single term", function(){
        cy.searchKeyword("ABRA");
        cy.url().should("include","/search?keyword");

        cy.get(".results > form").children().first().click();
        cy.url().should("include","/report");
        cy.contains("Symbol").next().should("have.text","ABRA");
    });

    it("can search two terms with OR", function(){
        cy.searchKeyword("CDPK1 OR CDPK4{enter}");
        cy.url().should("include","/search?keyword");

        cy.get(".results > form").children().first().within(()=>{
            cy.contains("Symbol").next().should("have.text","CDPK1");
        });

        cy.get(".results > form").children().last().within(()=>{
            cy.contains("Symbol").next().should("have.text","CDPK4");
        });
    });

    it("can search a phrase with quotation marks", function(){
        cy.searchKeyword('"DNA binding"');        
        cy.url().should("include","/search?keyword");

        cy.get(".results > form").children().first().click();
        cy.url().should("include","/report");
        cy.contains("Name").next().should("have.text","DNA binding");
    });

    it("can search for partial matches", function(){
        cy.searchKeyword('MAL*');        
        cy.url().should("include","/search?keyword");

        cy.get(".results > form").children().first().click();
        cy.url().should("include","/report");
        cy.get('.report-table-body').within(() => {
            cy.get(".report-table-cell").filter(':contains("MAL")').its('length').should("be.gt", 0);;
        })
    });

    it("can search with boolean search syntax", function(){
        cy.searchKeyword('protein AND PLA*');        
        cy.url().should("include","/search?keyword");

        cy.get(".results > form").children().first().click();
        cy.url().should("include","/report");
        cy.get('.report-table-body').within(() => {
            cy.get(".report-table-cell").filter(':contains("protein")').its('length').should("be.gt", 0);;
            cy.get(".report-table-cell").filter(':contains("PLA")').its('length').should("be.gt", 0);;
        })
    });

    it("can filter search results by category and organism", function(){
        cy.searchKeyword('MAL*');        
        cy.url().should("include","/search?keyword");

        cy.get("td").filter(':contains("Gene")').click();
        cy.get("td").filter(':contains("P. falciparum 3D7")').click();
        
        cy.get(".results > form").children().first().click();
        cy.url().should("include","/report");
        cy.contains("Organism").next().should("include.text","P. falciparum 3D7");
        cy.get('.report-page-heading > .start').should("include.text","Gene");
    });
})