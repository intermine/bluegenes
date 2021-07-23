describe("Report Page Layout Test", function(){
    beforeEach(function(){
        cy.loginToAdminAccount();
        cy.visit("/biotestmine/admin");
        cy.contains("Report page layout").parent().should("have.class","well").as("reportPageLayout");
    });

    it("can add a category containing classes",function(){
        cy.get("@reportPageLayout").within(() => {
            cy.get("Select").select("MRNA");
            cy.get("select").should("have.value","MRNA");
            cy.get("input").should("exist").type("Interactions{enter}",{delay:100});
            cy.get("li").contains("Interactions").should("exist").click(); //Flaky
            cy.get('.css-1hwfws3').click().type("Exons{enter}",{delay:100},{force:true});
            cy.get("ul").filter(".classes").find(".btn-fab").first().click();  //Flaky
            cy.contains("Save changes").click();
            cy.get(".success").should("include.text","Successfully saved changes to report page layout.");  
        })
    })

    it("can add description to classes", function(){
        cy.get("@reportPageLayout").within(() => {
            cy.get("Select").select("MRNA");
            cy.get("select").should("have.value","MRNA");
            cy.get("li").contains("Interactions").should("exist").click();
            cy.get("li").contains("Exons").should("exist").click();
            cy.get(".btn-group-sm").children().first().click();

            cy.get(".classes").within(() => {
                cy.get("textarea").clear().type("Coding regions within this gene{enter}",{delay:100});
                cy.contains("Save").click();
            })

            cy.contains("Save changes").click();
            cy.get(".success").should("include.text","Successfully saved changes to report page layout.");  
        })
    })

    it("can edit the visibility of the description of a class", function(){
        cy.get("@reportPageLayout").within(() => {
            cy.get("Select").select("MRNA");
            cy.get("select").should("have.value","MRNA");
            cy.get("li").contains("Interactions").should("exist").click()
            cy.get(".btn-group-sm").children().eq(1).click({delay:100});

            cy.contains("Save changes").click();
            cy.get(".success").should("include.text","Successfully saved changes to report page layout.");  
        })
    })

    it("can remove a class from a category", function(){
        cy.get("@reportPageLayout").within(() => {
            cy.get("Select").select("MRNA");
            cy.get("select").should("have.value","MRNA");
            cy.get("li").contains("Interactions").should("exist").click();
            cy.get('.css-1hwfws3').click().type("Introns{enter}",{delay:100},{force:true});
            cy.get("ul").filter(".classes").find(".btn-fab").last().click();
            cy.get(".btn-group-sm").eq(1).children().eq(4).click({delay:100});

            cy.contains("Save changes").click();
            cy.get(".success").should("include.text","Successfully saved changes to report page layout.");  
        })
    })

    after(function(){
        cy.visit("/biotestmine/report/MRNA/2000009");
        cy.get(".report-page-heading").should("include.text","mRNA.46312");
        cy.get('.report-table-heading').contains("Interactions")
        .parent(".report-table")
        .as('interactionsSection');

        cy.get("@interactionsSection").within(() => {
            cy.get(".report-item-title").should("include.text","Exons");
            cy.get('[class="icon icon-info"]').should("exist").click();
            cy.get(".report-item-title > .dropdown > .dropdown-menu").should("exist")
            .find("p").should("include.text","Coding regions within this gene");
        });

        cy.visit("/biotestmine/admin");
        cy.get("@reportPageLayout").within(() => {
            cy.get("Select").select("MRNA");
            cy.get("select").should("have.value","MRNA");
            cy.get("li").contains("Interactions").should("exist").click();
            cy.contains("Delete").click({multiple:true});
            cy.contains("Save changes").click();
            cy.get(".success").should("include.text","Successfully saved changes to report page layout.");  
        })
    })
})