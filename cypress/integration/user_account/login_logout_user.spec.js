describe(__filename, function() {

	it("it should login user successfully", function() {
		cy.openLoginDialogue()
		cy.get('.login-form').should('to.contain', 'Login to BioTestMine')
		cy.get("input#email").type("test_user@mail_account");
		cy.get("input[type='password']").type("secret");
		cy.get(".login-form")
			.find("button")
			.contains('Login')
		 	.click();
    cy.get(".logon.dropdown").click();
    cy.get(".logon.dropdown").should('to.contain', 'test_user@mail_account');
	});

	it("it should logout user successfully", function() {
		cy.openLoginDialogue()
		cy.get('.login-form').should('to.contain', 'Login to BioTestMine')
		cy.get("input#email").type("test_user@mail_account");
		cy.get("input[type='password']").type("secret");
		cy.get(".login-form")
			.find("button")
			.contains('Login')
		 	.click();
    cy.get(".logon.dropdown").click();
    cy.get(".logon.dropdown").contains('Logout').click({ force: true });
	cy.get('.main-nav').should('to.contain', 'Login');
	});

	it("it ensures password is not empty", function() {
		cy.openLoginDialogue()
		cy.get('.login-form').should('to.contain', 'Login to BioTestMine')
		cy.get("input#email").type("test_user@mail_account");
		cy.get(".login-form")
			.find("button")
			.contains('Login')
		 	.click();
		cy.get(".error-box")
			 .should('to.contain', "Missing parameter: 'password'"); 
	});
	it("it ensures username is not empty", function() {
		cy.openLoginDialogue()
		cy.get('.login-form').should('to.contain', 'Login to BioTestMine')
		cy.get("input[type='password']").type("test_password");
		cy.get(".login-form")
			.find("button")
			.contains('Login')
		 	.click();
		cy.get(".error-box")
			 .should('to.contain', "Missing parameter: 'username'"); 
	});

	it("it errors out on invalid user credentials", () => {
		cy.openLoginDialogue()
		cy.get('.login-form').should('to.contain', 'Login to BioTestMine')
		cy.get("input#email").type("test_user@gmail.com");
		cy.get("input[type='password']").type("secret");
		cy.get(".login-form")
			.find("button")
			.contains('Login')
		 	.click();

		cy.get(".error-box")
			.should('to.contain', 'Unknown username: test_user@gmail.com');
	});

	it("it errors out on invalid password", () => {
		cy.openLoginDialogue()
		cy.get('.login-form').should('to.contain', 'Login to BioTestMine')
		cy.get("input#email").type("test_user@mail_account");
		cy.get("input[type='password']").type("test");
		cy.get(".login-form")
			.find("button")
			.contains('Login')
		 	.click();

		cy.get(".error-box")
			.should('to.contain', 'Invalid password supplied');
	});

});
