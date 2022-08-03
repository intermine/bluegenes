describe("User Account Test", function() {
    beforeEach(function() {
      cy.visit("/biotestmine");
    });

    it.only("can create a new user account", function(){
        cy.openRegisterDialogue();
        cy.get(".login-form").should("exist");
        cy.get("input#email").type("test_user_account@mail.com");
		cy.get("input[type='password']").type("password");
        cy.intercept('POST', '/api/auth/register').as('register');
        cy.get("button").contains("Register").click();
        cy.wait("@register");
        cy.get(".logon.dropdown.success").should("exist").click();
        cy.get(".logon.dropdown.success").should("contain", "test_user_account@mail.com");
    })

    it("can set user preferences", function(){
        cy.loginToUserAccount("test_user_account@mail.com","password");
        cy.visit("/biotestmine/profile");
        cy.intercept('GET', '/user/preferences').as('getPreferences');
        cy.wait('@getPreferences');
        cy.get('.profile-page > ').eq(0).as("userPreferences").within(() => {
            cy.get('[type="checkbox"]').first().uncheck({force:true}); //still flaky
            cy.contains("Save changes").should("not.be.disabled").click(); 
            cy.get(".success").should("have.text","Updated preferences have been saved.");
            cy.get('[type="checkbox"]').first().should("not.be.checked");
        })
    })

    it("can generate API access key", function(){
        cy.loginToUserAccount("test_user_account@mail.com","password");
        cy.visit("/biotestmine/profile");
        cy.get('.profile-page > ').eq(1).as("generateAPI").within(() => {
            cy.contains("Generate a new API key").click();
            cy.get(".success").should("include.text","New API key successfully generated.");
        })
    })

    it("can add tags to list", function(){
        cy.loginToUserAccount("test_user_account@mail.com","password");
        cy.createGeneList("SODB, GBP, GST, CDPK1");
        cy.contains("Lists").click();
        cy.url().should("include","/lists");
        cy.get('.lists-item').its("length").should('be.gt',0);

        cy.get(".lists-item").first().within(() => {
            cy.get("button").find('svg[class="icon icon-list-more"]')
            .should("be.visible")
            .click();

            cy.get("button").find('svg[class="icon icon-list-edit"]')
            .should("be.visible")
            .first()
            .click();
        });

        cy.get(".modal-body").within(()=>{
            cy.contains("Tags").parent().find("input").type("Algae genes{enter}",{delay:100});
        })
        cy.get('.modal-footer').within(()=>{
            cy.contains("Save").click();
        })
        cy.get(".lists-table").within(()=>{
            cy.get(".tag").should("contain","Algae genes");
        })
    })

    it("can add lists to folders", function(){
        cy.loginToUserAccount("test_user_account@mail.com","password");
        cy.contains("Lists").click();
        cy.url().should("include","/lists");
        cy.get('.lists-item').its("length").should('be.gt',0);

        cy.get('.lists-headers').within(() => {
            cy.get('[type="checkbox"]').check();
        })
        cy.get('.bottom-controls').within(() => {
            cy.contains("Move all").click();
        })
        cy.get(".css-1wy0on6").click().type("List of common genes{enter}",{delay:100},{force:true});
        cy.get(".modal-footer").within(() => {
            cy.contains("Move list(s)").click();
        })
        cy.get('.list-title').should("contain","List of common genes");
    })

    it("can filter lists by tags", function(){
        cy.loginToUserAccount("test_user_account@mail.com","password");
        cy.createProteinList("Q8ID23_PLAF7, Q6LFN1");
        cy.get('.lists-item').its("length").should('be.gt',0);

        cy.contains("Tags").parent().find(".icon-selection").click();
        cy.get(".dropdown").find("li").filter(':contains("Algae genes")').click();
        cy.get('.lists-item').should('have.length',1);
        cy.get('.lists-item').find(".list-title").should("include.text","List of common genes");
    })

    it.only("can build and save a query", function(){
        cy.loginToUserAccount("test_user_account@mail.com","password");
        cy.visit("/biotestmine/querybuilder");
        cy.get(".model-browser-root").within(() => {
            cy.selectFromDropdown("Protein");        
            cy.contains("Add summary").click();
          });
    
          cy.get("div.panel-body").first().as("queryEditorTab").within(()=>{
            cy.contains("Save Query").click();
            cy.get("div.input-group").type("Protein summary", {delay:100});
            cy.contains("Save Query").click();
          });
          cy.visit("/biotestmine/querybuilder");
          cy.get("div.panel-body").last().as("savedQueriesTab").within(()=>{
            cy.contains("Saved Queries").click();
            cy.get('table').contains('td','Protein summary');
          });
    })

    

    it.only("can change user password", function(){
        cy.loginToUserAccount("test_user_account@mail.com","password")
        cy.visit("/biotestmine/profile");
        cy.get('.profile-page > ').eq(2).as("changePassword").within(() => {
            cy.contains("Old password").siblings(".form-control").type("password");
            cy.contains("New password").siblings(".form-control").type("newpassword");
            cy.intercept("POST","/biotestmine/service/user").as("waitPasswordChange");
            cy.get("button").contains("Save password").click();
            cy.wait("@waitPasswordChange");
            // cy.wait(500); //change?
            cy.get(".success").should("include.text","Password changed successfully.")
        })
        
    })

    it.only("can delete an account", function(){
        cy.loginToUserAccount("test_user_account@mail.com","newpassword")
        cy.visit("/biotestmine/profile");
        cy.get(".settings-group").eq(3).as("deleteAccountSection").within(() => {
            cy.contains("Start account deletion").click();
            cy.get("code").then(($code) => {
                const deletionCode = $code.text();
                cy.get("input").type(deletionCode);
            })
            cy.get("button").contains("Delete account").click();
        })
        cy.openLoginDialogue();
        cy.get(".login-form").should("contain", "Login to BioTestMine");
        cy.get("input#email").type("test_user_account@mail.com");
        cy.get("input[type='password']").type("newpassword");
        cy.intercept('POST', '/api/auth/login').as('login');
        cy.get(".login-form")
          .find("button")
          .contains('Login')
          .click();
        cy.wait('@login');
        cy.get(".error-box")
          .should('to.contain', 'Unknown username: test_user_account@mail.com');
    })
});
