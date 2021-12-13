describe("Admin Account Test", function(){
    beforeEach(function(){
        cy.loginToAdminAccount();
    });

    it("can set user preferences in profile page",function(){
        cy.visit("/biotestmine/profile");
        cy.intercept('GET', '/user/preferences').as('getPreferences');
        cy.wait('@getPreferences');
        cy.get('.profile-page > ').first().should("exist").within(() => {
            cy.get('[type="checkbox"]').first().uncheck({force:true}); //flaky
            // cy.get('[type="checkbox"]').first().should("not.be.checked"); //flaky
            cy.contains("Public name").type("{selectall}Admin{enter}",{delay:100});
            cy.contains("Save changes").should("not.be.disabled").click(); //flaky {force:true} needed?
            cy.get(".success").should("have.text","Updated preferences have been saved.");
        })
    })

    after(function(){
        cy.get('.profile-page > ').first().should("exist").within(() => {
            cy.get('[type="checkbox"]').first().check({force:true}); //flaky
            cy.get('[type="checkbox"]').first().should("be.checked"); 
            cy.contains("Public name").type("{selectall}{backspace}",{delay:100});
            cy.contains("Save changes").should("not.be.disabled").click(); //flaky {force:true}?
            cy.get(".success").should("have.text","Updated preferences have been saved.");
        })
    })

    it("can set and clear homepage notice",function(){
        cy.visit("/biotestmine/admin"); //direct use of cy.visit can be flaky sometimes
        cy.get(".admin-page .nav").contains('Home').click();
        cy.contains("Set homepage notice").parent().as("setHomepageNotice");

        cy.get("@setHomepageNotice").within(() => {
            cy.get("textarea").type("A new list has been added!{enter}",{delay:100});
            cy.contains("Save changes").click();
            cy.get(".success").should("include.text","Successfully saved changes to notice text.");
        })

        cy.visit("/biotestmine");
        cy.get(".text-center").first().should("have.text","BioTestMine");
        cy.contains("A new list has been added!");

        cy.visit("/biotestmine/admin");
        cy.get(".admin-page .nav").contains('Home').click();
        cy.get("@setHomepageNotice").within(() => {
            cy.contains("Clear notice").click();
            cy.get(".success").should("include.text","Successfully saved changes to notice text.");
        })
    })

    it("can update installed tools",function(){
        cy.visit("/biotestmine/tools");
        cy.contains("Update installed tools").click();
        cy.get(".alert").should("include.text","A tool operation is in progress...");
    })

    it("can generate API access key",function(){
        cy.visit("/biotestmine/profile");
        cy.get('.profile-page > ').eq(1).within(() => {
            cy.contains("Generate a new API key").click();
            cy.get(".success").should("include.text","New API key successfully generated.");
        })
    })
})
