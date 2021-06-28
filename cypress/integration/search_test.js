describe("Search Test", function(){
    beforeEach(function(){
        cy.visit("/");
    });

    it("can be accessed from navigation bar", function(){
        cy.get(".search").first().type('Ma*{enter}',{delay:100});
        cy.url().should("include","/search");

        cy.get(".results > form").children().first().click();
        cy.url().should("include","/report");
    });

    it("can search a single term", function(){
        cy.get(".search").first().type('ABRA{enter}',{delay:100});
        cy.url().should("include","/search");

        cy.get(".results > form").children().first().click();
        cy.url().should("include","/report");
        cy.contains("Symbol").next().should("have.text","ABRA");
    });

    it("can search two terms with OR", function(){
        cy.get(".search").first().type('CDPK1 OR CDPK4{enter}',{delay:100});
        cy.url().should("include","/search");

        cy.get(".results > form").children().first().within(()=>{
            cy.contains("Symbol").next().should("have.text","CDPK1");
        });

        cy.get(".results > form").children().last().within(()=>{
            cy.contains("Symbol").next().should("have.text","CDPK4");
        });
    });

    it("can search a phrase with quotation marks", function(){
        cy.get(".search").first().type('"DNA binding"{enter}',{delay:100});
        cy.url().should("include","/search");

        cy.get(".results > form").children().first().click();
        cy.url().should("include","/report");
        cy.contains("Name").next().should("have.text","DNA binding");
    });

    it("can search for partial matches", function(){
        cy.get(".search").first().type('MAL*{enter}',{delay:100});
        cy.url().should("include","/search");

        cy.get(".results > form").children().first().click();
        cy.url().should("include","/report");
        cy.contains("DB identifier").next().should("include.text","MAL");
    });

    it("can search with boolean search syntax", function(){
        cy.get(".search").first().type('protein AND PLA*{enter}',{delay:100});
        cy.url().should("include","/search");

        cy.get(".results > form").children().first().click();
        cy.url().should("include","/report");
        cy.contains("Name").next().should("include.text","protein");
        cy.contains("UniProt Name").next().should("include.text","PLA");
    });

    it("can filter search results by category and organism", function(){
        cy.get(".search").first().type('MAL*{enter}',{delay:100});
        cy.url().should("include","/search");

        cy.get("td").filter(':contains("Gene")').click();
        cy.get("td").filter(':contains("P. falciparum 3D7")').click();
        
        cy.get(".results > form").children().first().click();
        cy.url().should("include","/report");
        cy.contains("Organism").next().should("include.text","P. falciparum 3D7");
        cy.get('.report-page-heading > .start').should("include.text","Gene");
    });
})