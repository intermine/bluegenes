describe("Login Tests", function() {
    it("Login and logout works", function() {
        cy.server();
        cy.route("POST", "/api/auth/login").as("auth");
        cy.contains("Log In").click();
        cy.get("#email").type("test_user@mail_account");
        cy.get("input[type='password']").type("secret");
        cy.get("form")
            .find("button")
            .click();
        cy.wait("@auth");
        cy.get("@auth").should(xhr => {
            expect(xhr.status).to.equal(200);
        });

        cy.contains("test_user@mail_account").should('be.visible');

        cy.get('#bluegenes-main-nav .logon').click();
        cy.get('#bluegenes-main-nav .logon').contains('Log Out').click();
    });

    // invalid login
    // invalid logout?
});