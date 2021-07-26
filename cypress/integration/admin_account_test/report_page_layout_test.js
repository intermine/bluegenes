describe("Report Page Layout Test", function(){
    beforeEach(function(){
        cy.loginToAdminAccount();
        cy.visit("/biotestmine/admin");
        cy.contains("Report page layout").parent().should("have.class","well").as("reportPageLayout");
        cy.get("@reportPageLayout").within(() => {
            cy.get("Select").select("MRNA");
            cy.get("select").should("have.value","MRNA");
        })
    });

    it("can add a category",function(){
        cy.get("@reportPageLayout").within(() => {
            cy.get("input").should("exist").type("Interactions{enter}",{delay:100});
            cy.get("li").contains("Interactions").click().parent().should("have.class","active");
            cy.get('.css-1hwfws3').click().type("Exons{enter}",{delay:100},{force:true});
            cy.get(".classes").within(() => {
                cy.get(".icon-plus").as("addClassButton").click();
                cy.get(".class-entry").should("exist");
            })
            cy.contains("Save changes").click();
            cy.get(".success").should("include.text","Successfully saved changes to report page layout.");  
        })
    })

    it("can add description to classes", function(){
        cy.get("@reportPageLayout").within(() => {
            cy.get("li").contains("Interactions").click().parent().should("have.class","active");
            cy.get("li").contains("Exons").should("exist").click();                
            cy.get(".classes").within(() => {
                cy.get(".icon-info").as("classInfoButton").click();
                cy.get("textarea").clear().type("Coding regions within this gene{enter}",{delay:100});
                cy.contains("Save").click();
            })
            cy.contains("Save changes").click();
            cy.get(".success").should("include.text","Successfully saved changes to report page layout.");  
        })
    })

    it("can make a class show as collapsed", function(){
        cy.get("@reportPageLayout").within(() => {
            cy.get("li").contains("Interactions").click().parent().should("have.class","active");
            cy.get(".classes").within(() => {
                cy.get(".icon-eye").as("collapseClassButton").click();
                cy.get(".icon-eye-blocked").should("exist");
            })
            cy.contains("Save changes").click();
            cy.get(".success").should("include.text","Successfully saved changes to report page layout.");  
        })
    })
    it("can move a class up", function(){
        cy.get("@reportPageLayout").within(() => {
            cy.get("li").contains("Interactions").click().parent().should("have.class","active");
            cy.get('.css-1hwfws3').click().type("Introns{enter}",{delay:100},{force:true});
            cy.get(".classes").within(() => {
                cy.get(".icon-plus").as("addClassButton").click();
                cy.get(".class-entry").its("length").should("eq",2);
            })
            cy.get(".class-entry").eq(1).within(() => {
                cy.get(".icon-move-up-list").as("moveClassUpButton").click();
            })
            cy.get(".class-entry").eq(0).find("span").should("include.text","Introns");
            cy.get(".class-entry").eq(1).find("span").should("include.text","Exons");
            cy.contains("Save changes").click();
            cy.get(".success").should("include.text","Successfully saved changes to report page layout.");  
        })
    })

    it("can move a class down", function(){
        cy.get("@reportPageLayout").within(() => {
            cy.get("li").contains("Interactions").click().parent().should("have.class","active");
            cy.get(".class-entry").its("length").should("eq",2);
            cy.get(".class-entry").eq(0).within(() => {
                cy.get(".icon-move-down-list").as("moveClassDownButton").click();
            })
            cy.get(".class-entry").eq(0).find("span").should("include.text","Exons");
            cy.get(".class-entry").eq(1).find("span").should("include.text","Introns");
            cy.contains("Save changes").click();
            cy.get(".success").should("include.text","Successfully saved changes to report page layout.");  
        })
    })

    it("can remove a class from a category", function(){
        cy.get("@reportPageLayout").within(() => {
            cy.get("li").contains("Interactions").click().parent().should("have.class","active");
            cy.get(".class-entry").its("length").should("eq",2);
            cy.get(".class-entry").eq(1).within(() => {
                cy.get(".icon-remove-list").as("removeClassButton").click();
            })
            cy.get(".class-entry").its("length").should("eq",1);
            cy.get(".class-entry").find("span").should("include.text","Exons");
            cy.contains("Save changes").click();
            cy.get(".success").should("include.text","Successfully saved changes to report page layout.");  
        })
    })
    
    it("can move a category up", function(){
        cy.get("@reportPageLayout").within(() => {
            cy.get("input").should("exist").type("Regions{enter}",{delay:100});
            cy.get("li").contains("Interactions").should("exist");
            cy.get("li").contains("Regions").should("exist");
            cy.get(".nav").within(() => {
                cy.get("a").its("length").should("eq",2);
                cy.get("a").eq(1).should("include.text","Regions").within(() => {
                    cy.contains("Move up").click();
                })
                cy.get("a").eq(0).should("include.text","Regions");
                cy.get("a").eq(1).should("include.text","Interactions");
            })

            cy.contains("Save changes").click();
            cy.get(".success").should("include.text","Successfully saved changes to report page layout.");  
        })
    })

    it("can move a category down", function(){
        cy.get("@reportPageLayout").within(() => {
            cy.get("li").contains("Interactions").should("exist");
            cy.get("li").contains("Regions").should("exist");
            cy.get(".nav").within(() => {
                cy.get("a").its("length").should("eq",2);
                cy.get("a").eq(0).should("include.text","Regions").within(() => {
                    cy.contains("Move down").click();
                })
                cy.get("a").eq(0).should("include.text","Interactions");
                cy.get("a").eq(1).should("include.text","Regions");
            })

            cy.contains("Save changes").click();
            cy.get(".success").should("include.text","Successfully saved changes to report page layout.");  
        })
    })

    it("can update the layout in a report page", function(){
        cy.searchKeyword("mRNA.46312");
        cy.url().should("include","/search?keyword"); // Flaky
        cy.get(".results > form").children().first().click();
        cy.url().should("include","/report");

        cy.get(".report-page-heading").should("include.text","mRNA.46312");
        cy.get(".report-table-heading").contains("Interactions")
        .parent(".report-table")
        .as("interactionsSection");

        cy.get("@interactionsSection").within(() => {
            cy.get(".im-table").should("not.exist");
            cy.get(".report-item-title").should("include.text","Exons");
            cy.get('[class="icon icon-info"]').should("exist").click();
            cy.get(".report-item-title > .dropdown > .dropdown-menu").should("exist")
            .find("p").should("include.text","Coding regions within this gene");
            cy.get(".report-item-toggle").click();
            cy.get(".im-table").should("exist");
        });
    })

    it("can delete a category", function(){
        cy.get("@reportPageLayout").within(() => {
            cy.get("li").contains("Interactions").should("exist");
            cy.get("li").contains("Regions").should("exist");
            cy.get("a").its("length").should("eq",2);
            cy.get("a").eq(1).should("include.text","Regions").within(() => {
                cy.contains("Delete").click();
            })
            cy.get("a").eq(0).should("include.text","Interactions").within(() => {
                cy.contains("Delete").click();
            })
            // cy.contains("Delete").click({delay:100},{multiple:true}); //Flaky
            cy.get("li").contains("Interactions").should("not.exist");
            cy.get("li").contains("Regions").should("not.exist");
            cy.contains("Save changes").click();
            cy.get(".success").should("include.text","Successfully saved changes to report page layout.");  
        })
    })
})