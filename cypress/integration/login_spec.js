describe("Login Tests", function() {
	beforeEach(function() {
		cy.visit("/");

		cy.server();
    cy.route("POST", "/api/auth/login").as("auth");
    cy.contains("Log In").click();
	});
	
  it("login and logout user successfully", function() {
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
	
	// **The response for the two tests below give me inconsistent response; sometimes 500, other times, 401.
	// Uncomment when resolved.
	// it("requires an email input; expect error", () => {
	// 	cy.get("input[type='password']").type("secret{enter}");
	// 	cy.wait("@auth")
	// 	cy.get("@auth").should(xhr => {
	// 		expect(xhr.status).to.equal(401);
	// 	});

	// 	cy.get(".error-box")
	// 		.should('contain', 'Empty user name.');
	// });

	// it("requires a password input; expect error", () => {
	// 	cy.get("#email").type("test_user@mail_account{enter}");
	// 	cy.wait("@auth");
	// 	cy.get("@auth").should(xhr => {
	// 		expect(xhr.status).to.equal(401);
	// 	});

	// 	cy.get(".error-box")
	// 		.should('contain', 'Invalid password supplied');
	// });

	it("requires valid username; expect error", () => {
		cy.get("#email").type("dummy@no_bueno.com");
		cy.get("input[type='password']").type("secret{enter}");
		cy.wait("@auth")
		cy.get("@auth").should(xhr => {
			expect(xhr.status).to.equal(401);
		});

		cy.get(".error-box")
			.should('contain', 'Unknown username: dummy@no_bueno.com');
	});

	it("requires valid password; expect error", () => {
		cy.get("#email").type("test_user@mail_account");
		cy.get("input[type='password']").type("nay{enter}");
		cy.wait("@auth")
		cy.get("@auth").should(xhr => {
			expect(xhr.status).to.equal(401);
		});

		cy.get(".error-box")
			.should('contain', 'Invalid password supplied');
	});

});