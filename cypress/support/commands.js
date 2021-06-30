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

Cypress.Commands.add("openLoginDialogue", () => {
    cy.visit('/biotestmine');
    cy.get('.dropdown-toggle').contains('LOGIN').click();
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
    cy.get(".searchform > input").type(keyword + '{enter}',{delay:100});
})

Cypress.Commands.add("selectFromDropdown", (keyword) => {
    cy.get('select').then($option => {$option.val(keyword)}).parent().trigger('change');
})

Cypress.Commands.add("createGeneList", (geneList) => {
    cy.contains("Upload").click();
    cy.get(".wizard").find("textarea").type(geneList,{delay:100});
    cy.contains("Continue").click();
    cy.contains("Save List").click();
    cy.wait(100);
    cy.contains("Lists").click();
})

Cypress.Commands.add("createProteinList", (proteinList) => {
    cy.contains("Upload").click();
    cy.contains("List type").parent().find("select").select("Protein");
    cy.get(".wizard").find("textarea").type(proteinList,{delay:100});
    cy.contains("Continue").click();
    cy.contains("Save List").click();
    cy.wait(100);
    cy.contains("Lists").click();
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