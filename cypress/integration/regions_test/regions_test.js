describe("Regions Test", function(){
    beforeEach(function(){
        cy.visit("/biotestmine/regions");
    });

    it("can search a gene region given by example", function(){
        cy.get(".input-section").within(() => {
            cy.contains("Show Example").click();
            cy.get("textarea").should("include.text","MAL");
            cy.get("button").filter(':contains("Search")').click();
        })
        cy.get(".results").within(() => {
            cy.get(".single-feature").its("length").should("be.gt",0);
            cy.contains("exon.46111").click();
        })
        cy.url().should("include","/report");
    });

    it("can type in chromosome coordinates and select features", function(){

        cy.get(".input-section").within(() => {
            cy.get("textarea").type("MAL1:0..100000",{delay:100});
            cy.get('button[class="btn dropdown-toggle"]').click();
            cy.contains("P. falciparum 3D7").click();
            cy.contains("Features to include").children().click();
            cy.contains("Gene").children().click();
            cy.get("button").filter(':contains("Search")').click();
        })
        cy.get(".results").within(() => {
            cy.get(".single-feature").its("length").should("be.gt",0);
            cy.contains("Next").click();
            cy.get(".single-feature").its("length").should("be.gt",0);
            cy.get(".single-feature").first().click();
        })
        cy.url().should("include","/report");
    });

    it("can select the coordinate system", function(){
        cy.get(".radio-group").within(() => {
            cy.contains("interbase").within(() => {
                cy.get(".check").should("not.be.visible");
                cy.get(".circle").should("be.visible").click();
                cy.wait(500);
                cy.get(".check").should("be.visible");
            })
        })
    })

    it("can select to perform strand-specific region search", function(){
        cy.get(".togglebutton").within(() => {
            cy.get('.toggle').click();
            cy.wait(500);
        })
        // Write assertion
    })

    it("can extend gene search region by clicking", function(){
        cy.get(".extend-region").within(() => {
            cy.get(".slider-ticks").eq(0).within(() => {
                cy.contains("100k").click();
                cy.wait(500);
            })
            cy.get(".form-control").eq(1).should("have.value","100k");
        })
    })

    it("can extend gene search region by inputing in the text box", function(){
        cy.get(".extend-region").within(() => {
            cy.get("input.form-control").eq(1).clear().type("138k{enter}",{delay:100});
            cy.get("input.form-control").eq(1).should("have.value","138k");
        })
    })

    it("can lock gene search region extension", function(){
        cy.get(".extend-region").within(() => {
            cy.get(".icon-lock").click();
            cy.get(".slider-ticks").eq(0).within(() => {
                cy.contains("100k").click();
                cy.wait(500);
            })
            cy.get("input.form-control").eq(1).should("have.value","100k");
            cy.get("input.form-control").eq(2).should("have.value","0");
        })
    })

    it("can select the organism in which to perform region search", function(){
        cy.get(".organism-selection").within(() => {
            cy.get(".dropdown-toggle").click();
            cy.get(".dropdown-menu").within(() => {
                cy.contains("Clear").click();
            })
            cy.get("span").should("include.text","All Organisms");
        })
    })

    it("can create a list by feature type", function(){
        cy.get(".input-section").within(() => {
            cy.contains("Show Example").click();
            cy.get("textarea").should("include.text","MAL");
            cy.get("button").filter(':contains("Search")').click();
        })
        cy.get(".results").within(() => {
            cy.get(".single-feature").its("length").should("be.gt",0);
        })
        cy.get(".results-actions").within(() => {
            cy.contains("Create list by feature type").click();
            cy.get(".dropdown-menu").within(() => {
                cy.contains("Gene").click();
            })
        })
        cy.intercept("POST","/biotestmine/service/query/results").as("queryResults");
        cy.wait("@queryResults");
        cy.url().should("include","/results");
        cy.get(".query-title").should("include.text","Gene");
    })

    it.only("can view all features in search region in a results table", function(){
        cy.get(".input-section").within(() => {
            cy.contains("Show Example").click();
            cy.get("textarea").should("include.text","MAL");
            cy.get("button").filter(':contains("Search")').click();
        })
        cy.get(".results").within(() => {
            cy.get(".single-feature").its("length").should("be.gt",0);
        })
        cy.get(".results-actions").within(() => {
            cy.contains("View all in results table").click();
        })
        cy.intercept("POST","/biotestmine/service/query/results").as("queryResults");
        cy.wait("@queryResults");
        cy.contains("Query Results").should("exist");
        cy.url().should("include","/results");
    })

    it("can skip to selected region of search results", function(){
        cy.get(".input-section").within(() => {
            cy.contains("Show Example").click();
            cy.get("textarea").should("include.text","MAL");
            cy.get("button").filter(':contains("Search")').click();
        })
        cy.get(".results").within(() => {
            cy.get(".single-feature").its("length").should("be.gt",0);
        })
        cy.get("#region-skip-to-bar").within(() => {
            cy.get(".results-count").contains("MAL9").click();
            cy.wait(500);
        })
        cy.get(".results").eq(2).as("MAL9results");
        cy.isInViewport("@MAL9results");
    })
})
