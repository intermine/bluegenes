describe(__filename, function () {
  beforeEach(() => {
    cy.visit("/");
    cy.contains("LOGIN").click();
    cy.get("form").contains("Create new account").click();

    // Without this wait, cypress could get the element *before* the form
    // changes, then complain that it no longer exists when trying to type.
     cy.wait(200);

    cy.server();
    cy.route("POST", "/api/auth/register").as("auth");
   })

  it("requires email input; expect error", () => {
    cy.get("#email").type("test_user@mail_account.com{enter}");
    cy.wait("@auth");
    cy.get("@auth").should(xhr => {
      expect(xhr.status).to.eq(400);
    })
    cy.get(".error-box").should('contain', 'missing parameters. name and password required');
  });

  it("requires password input; expect error; ", () => {
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
    cy.get(".logon.dropdown").click();
    cy.get(".logon.dropdown").should("contain", "dummy_act@dummy.com");
  });

  it("requires unique email; expect error", () => {
    cy.get("#email").type("dummy_act@dummy.com");
    cy.get("input[type='password']").type("naw{enter}");
    cy.wait("@auth");
    cy.get("@auth").should(xhr => {
      expect(xhr.status).to.eq(400);
    });

    cy.get(".error-box")
      .should('be.visible')
      .and('contain', 'There is already a user with that name');

    // cy.getCookie("ring-session").should('not.exist');
  });
});
