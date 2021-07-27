describe("User Account Test", function() {
    beforeEach(function() {
      cy.visit("/biotestmine");
    });

    it("can create a new user account", function(){
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
        cy.loginToUserAccount("test_user_account@mail.com","password")
        cy.visit("/biotestmine/profile");
        cy.get('.profile-page > ').eq(1).as("generateAPI").within(() => {
            cy.contains("Generate a new API key").click();
            cy.get(".success").should("include.text","New API key successfully generated.");
        })
    })

    it("can change user password", function(){
        cy.loginToUserAccount("test_user_account@mail.com","password")
        cy.visit("/biotestmine/profile");
        cy.get('.profile-page > ').eq(2).as("changePassword").within(() => {
            cy.contains("Old password").siblings(".form-control").type("password");
            cy.contains("New password").siblings(".form-control").type("newpassword");
            cy.get("button").contains("Save password").click();
            cy.wait(500); //change?
            cy.get(".success").should("include.text","Password changed successfully.")
        })
        
    })

    it("can delete an account", function(){
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
        cy.intercept("POST","/api/auth/logout").as("logout");
        cy.wait("@logout");
        cy.get(".mine-intro").should("exist");
    })
});