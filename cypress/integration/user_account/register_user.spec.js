function getDummyEmail() {
  var strValues = "abcdefghijklmnopqrs";
  var strEmail = "";
  var strTmp;
  for (var i = 0; i < 10; i++) {
      strTmp = strValues.charAt(Math.round(strValues.length * Math.random()));
      strEmail = strEmail + strTmp;
  }
  strTmp = "";
  strEmail = strEmail + "@";
  for (var j = 0; j < 8; j++) {
      strTmp = strValues.charAt(Math.round(strValues.length * Math.random()));
      strEmail = strEmail + strTmp;
  }
  strEmail = strEmail + ".com"
  return strEmail;
}

describe(__filename, function () {

  it("it ensures a new user is registered successfully", () => {
    cy.openRegisterDialogue();
    const username = getDummyEmail();
    cy.get("input#email").type(username);
    cy.get("input[type='password']").type("test_password");
    cy.get("button").contains("Register").click();

    cy.get(".logon.dropdown").click();
    cy.get(".logon.dropdown").should('to.contain', username);
  });

  it("it ensures that email is always required", () => {
    cy.openRegisterDialogue();
    cy.get("input[type='password']").type("test_password");
    cy.get("button").contains("Register").click();

    cy.get(".error-box")
    .should('to.contain', "missing parameters. name and password required"); 
  });

  it("it ensures that password is always required", () => {
    cy.openRegisterDialogue();
    const username = getDummyEmail();
    cy.get("input#email").type(username);
    cy.get("button").contains("Register").click();

    cy.get(".error-box")
    .should('to.contain', "missing parameters. name and password required"); 
  });

  it("it ensures that user email is always unique", () => {
    cy.openRegisterDialogue();
    cy.get("input#email").type("test_user@mail_account");
    cy.get("input[type='password']").type("test_password");
    cy.get("button").contains("Register").click();

    cy.get(".error-box")
    .should('to.contain', "There is already a user with that name"); 
  });

});
