describe("Registration Tests", function () {
  beforeEach(() => {
    cy.visit("/humanmine");
    cy.contains("Log In").click();
    cy.get("form").contains("I don't have an account").click();

    cy.server();
    cy.route("POST", "/api/auth/register").as("auth");
  });

  it("displays error message; requires email input", () => {
    cy.get("#email").type("test_user@mail_account.com{enter}");
    cy.wait("@auth");
    cy.get("@auth").should(xhr => {
      expect(xhr.status).to.eq(400);
    })
    cy.get(".error-box").should('contain', 'missing parameters. name and password required');
  });

  it("displays error message; requires password input", () => {
    cy.get("input[type='password']").type("nay{enter}");
    cy.wait("@auth");
    cy.get("@auth").should(xhr => {
      expect(xhr.status).to.eq(400);
    })

    cy.get(".error-box")
      .should('be.visible')
      .and('contain', 'missing parameters. name and password required');
  });

  it("registers new user successfully", () => {
    cy.get("#email").type("dummy_act@dummy.com");
    cy.get("input[type='password']").type("yay{enter}");
    cy.wait("@auth");
    cy.get("@auth").should(xhr => {
      expect(xhr.status).to.eq(200);
    });

    cy.getCookie("ring-session").should('exist');
    cy.get('.long-name').should("contain", "dummy_act@dummy.com")
  })

  it("displays error message; requires unique email", () => {
    cy.get("#email").type("dummy_act@dummy.com");
    cy.get("input[type='password']").type("naw{enter}");
    cy.wait("@auth");
    cy.get("@auth").should(xhr => {
      expect(xhr.status).to.eq(400);
    });

    cy.get(".error-box")
      .should('be.visible')
      .and('contain', 'There is already a user with that name');

    cy.getCookie("ring-session").should('not.exist');
  })
});
