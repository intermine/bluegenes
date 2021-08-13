// ***********************************************
// This example commands.js shows you how to
// create various custom commands and overwrite
// existing commands.
//
// For more comprehensive examples of custom
// commands please read more here:
// https://on.cypress.io/custom-commands
// ***********************************************
//
//
// -- This is a parent command --
// Cypress.Commands.add("login", (email, password) => { ... })
//
//
// -- This is a child command --
// Cypress.Commands.add("drag", { prevSubject: 'element'}, (subject, options) => { ... })
//
//
// -- This is a dual command --
// Cypress.Commands.add("dismiss", { prevSubject: 'optional'}, (subject, options) => { ... })
//
//
// -- This is will overwrite an existing command --
// Cypress.Commands.overwrite("visit", (originalFn, url, options) => { ... })
import 'cypress-file-upload';
import '@4tw/cypress-drag-drop';

Cypress.Commands.add("openLoginDialogue", () => {
    cy.visit('/biotestmine');
    cy.get('.dropdown-toggle').should("exist").contains('LOGIN').click();
})

Cypress.Commands.add("openRegisterDialogue", () => {
    cy.visit('/biotestmine');
    cy.get('.dropdown-toggle').contains('LOGIN').click();
    cy.get('a.btn-block').contains('Create new account').click();
})

Cypress.Commands.add("openListsTab", () => {
    cy.get("#bluegenes-main-nav").within(() => {
        cy.contains("Lists").click();
    });
})

Cypress.Commands.add("searchKeyword", (keyword) => {
    cy.visit("/biotestmine/search");
    cy.get(".searchform > input").type(keyword + '{enter}',{delay:200});
})

Cypress.Commands.add("selectFromDropdown", (keyword) => {
    cy.get('select').then($option => {$option.val(keyword)}).parent().trigger('change');
})

Cypress.Commands.add("createGeneList", (geneList) => {
    cy.contains("Upload").click();
    cy.get(".wizard").find("textarea").type(geneList,{delay:100});
    cy.contains("Continue").click();
    //Assertion
    cy.get('.title').should("exist");
    cy.url().should("include","/save");
    cy.contains("Save List").click();
    cy.url().should("include","/results")
    cy.contains("Lists").click();
})

Cypress.Commands.add("createProteinList", (proteinList) => {
    cy.contains("Upload").click();
    cy.contains("List type").parent().find("select").select("Protein");
    cy.get(".wizard").find("textarea").type(proteinList,{delay:100});
    cy.contains("Continue").click();
    cy.contains("Save List").click();
    cy.url().should("include","/results")
    cy.contains("Lists").click();
})

//The commands isInViewport and isNotInViewport are taken directly from
//https://github.com/cypress-io/cypress/issues/877#issuecomment-490504922.

Cypress.Commands.add('isInViewport', element => {
    cy.get(element).then($el => {
      const bottom = Cypress.$(cy.state('window')).height()
      const rect = $el[0].getBoundingClientRect()
  
      expect(rect.top).not.to.be.greaterThan(bottom)
      expect(rect.bottom).not.to.be.greaterThan(bottom)
      expect(rect.top).not.to.be.greaterThan(bottom)
      expect(rect.bottom).not.to.be.greaterThan(bottom)
    })
  })

  Cypress.Commands.add('isNotInViewport', element => {
    cy.get(element).then($el => {
      const bottom = Cypress.$(cy.state('window')).height()
      const rect = $el[0].getBoundingClientRect()
  
      expect(rect.top).to.be.greaterThan(bottom)
      expect(rect.bottom).to.be.greaterThan(bottom)
      expect(rect.top).to.be.greaterThan(bottom)
      expect(rect.bottom).to.be.greaterThan(bottom)
    })
  })

  Cypress.Commands.add('loginToAdminAccount', element => {
    cy.openLoginDialogue();
		cy.get(".login-form").should("contain", "Login to BioTestMine");
		cy.get("input#email").type("test_user@mail_account");
		cy.get("input[type='password']").type("secret");
    cy.intercept('POST', '/api/auth/login').as('login');
		cy.get(".login-form")
			.find("button")
			.contains('Login')
		 	.click();
        cy.wait('@login');
        cy.get(".logon.dropdown.success").should("exist").click();
        cy.get(".logon.dropdown.success").should("contain", "test_user@mail_account"); //flaky
  })

  Cypress.Commands.add('loginToUserAccount', (email,password) => {
    cy.openLoginDialogue();
		cy.get(".login-form").should("contain", "Login to BioTestMine");
		cy.get("input#email").type(email);
		cy.get("input[type='password']").type(password);
    cy.intercept('POST', '/api/auth/login').as('login');
		cy.get(".login-form")
			.find("button")
			.contains('Login')
		 	.click();
        cy.wait('@login');
    cy.get(".logon.dropdown.success").should("exist").click();
    cy.get(".logon.dropdown.success").should("contain", email); //flaky
  })

  Cypress.Commands.add('createListFromFile', (filePath,listName) => {
    cy.contains("Upload").click();
    cy.contains("File Upload").click();
    cy.contains("Browse").click();
    cy.get('input[type="file"]').attachFile(filePath);
    cy.intercept("POST","/api/ids/parse").as("listParse");
    cy.contains("Continue").click();
    cy.wait("@listParse");
    cy.get('.title').should("exist");
    cy.url().should("include","/save");
    cy.get(".save-list > label > input").clear().type(listName,{delay:100});
    cy.contains("Save List").click();
    cy.intercept("POST","/biotestmine/service/query/results").as("resultsLoad");
    cy.url().should("include","/results");
    cy.wait("@resultsLoad");
  })

// Cypress.Commands.add("openTemplatesTab", () => {
//     cy.get("#bluegenes-main-nav").within(() => {
//         cy.contains("Templates").click();
//     });
// })

// Cypress.Commands.add("openRegionsTab", () => {
//     cy.get("#bluegenes-main-nav").within(() => {
//         cy.contains("Regions").click();
//     });
// })

// Cypress.Commands.add("openQueryBuilderTab", () => {
//     cy.get("#bluegenes-main-nav").within(() => {
//         cy.contains("Query Builder").click();
//     });
// })