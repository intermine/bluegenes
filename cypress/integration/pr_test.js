describe("UI Test", function() {
  beforeEach(function() {
    cy.visit("/");
  });

  it("Upload page resolved IDs as expected", function() {
    cy.server();
    cy.route("POST", "*/service/ids").as("uploadData");
    cy.contains("Upload").click();
    cy.url().should("include", "/upload/input");
    cy.contains("Example").click();
    cy.get("button")
      .contains("Continue")
      .click();
    cy.url().should("include", "/upload/save");
    cy.wait("@uploadData");
    cy.get("@uploadData").should(xhr => {
      expect(xhr.status).to.equal(200);
    });
  });

  it("Templates execute and show results", function() {
    cy.get("#bluegenes-main-nav").within(() => {
      cy.contains("Templates").click();
    });
    cy.url().should("include", "/templates");
    cy.get("div[class=template-list]").within(() => {
      cy.get(":nth-child(n) > .col")
        .its("length")
        .should("be.gt", 0);
    });
  });

  it("Templates allow you to select lists and type in identifiers", function() {
    cy.server();
    cy.route("POST", "*/service/query/results/tablerows").as("getData");
    cy.get("#bluegenes-main-nav").within(() => {
      cy.contains("Templates").click();
    });
    cy.url().should("include", "/templates");
    cy.get("div[class=template-list]").within(() => {
      cy.get(":nth-child(n) > .col")
        .its("length")
        .should("be.gt", 0);
    });
    cy.get("div[class=template-list]").within(() => {
      cy.get(":nth-child(2) > .col")
        .find("button")
        .contains("View >>")
        .click({ force: true });
    });
    cy.wait("@getData");
    cy.get("@getData").should(xhr => {
      expect(xhr.status).to.equal(200);
    });
    cy.get("select[class=form-control]").select("!=");
  });

  it("Perform a region search using existing example", function() {
    cy.server();
    cy.route("POST", "*/service/query/results").as("getData");
    cy.contains("Regions").click();
    cy.get(".guidance")
      .contains("[Show me an example]")
      .click();
    cy.get(".region-text > .form-control").should("not.be.empty");
    cy.get("button")
      .contains("Search")
      .click();
    cy.wait("@getData");
    cy.get("@getData").should(xhr => {
      expect(xhr.status).to.equal(200);
    });
    cy.get(".results-summary").should("have", "Results");
  });

  it("Login and logout works", function() {
    cy.server();
    cy.route("POST", "/api/auth/login").as("auth");
    cy.get("#bluegenes-main-nav").within(() => {
      cy.get("ul")
        .find("li.dropdown.mine-settings.secondary-nav")
        .click()
        .contains("FlyMine")
        .click();
    });
    cy.contains("Log In").click();
    cy.get("#email").type("demo@intermine.org");
    cy.get("#password").type("demo");
    cy.get("form")
      .find("button")
      .click();
    cy.wait("@auth");
    cy.get("@auth").should(xhr => {
      expect(xhr.status).to.equal(200);
    });
    cy.contains("demo@intermine.org");
  });
});
