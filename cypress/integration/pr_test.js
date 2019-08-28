// There is a bug triggered by the combination of reitit and cypress:
// https://github.com/metosin/reitit/pull/301
// Until the above PR gets merged, we ignore uncaught exceptions containing the
// false positive error to keep CI going.
Cypress.on('uncaught:exception', (err, runnable) => {
  expect(err.message).to.include('is not ISeqable')

  return false;
});

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
    cy.get("select.constraint-chooser").eq(1).select("!=");
  });

  it("Gives suggestion results when typing in search", function() {
    cy.get(".home .search").within(() => {
      cy.get("input[class=typeahead-search]").type("eve");
      cy.get(".quicksearch-result").should("have.length", 5);
    });
  });

  it("Opens the search page to show search results", function() {
    cy.get(".home .search").within(() => {
      cy.get("input[class=typeahead-search]").type("eve{enter}");
    });
    cy.url().should("include", "/search");

    cy.get(".results").within(() => {
      cy.get(".result").should("have.length.of.at.least", 10)
    });
  });

  it("Saves the list from an upload and deletes it", function() {
    var listName = "Automated CI test list ".concat(Number(new Date()));

    cy.contains("Upload").click();
    cy.contains("Example").click();
    cy.get("button")
      .contains("Continue")
      .click();
    cy.get(".save-list input")
      .clear()
      .type(listName, { delay: 50 });

    cy.server();
    cy.route("POST", "*/service/query/tolist").as("tolist");

    cy.get("button")
      .contains("Save List")
      .click();

    cy.wait("@tolist");

    cy.contains("Data") .click();
    cy.contains(listName);
    cy.contains(listName).parent().within(() => {
      cy.get("input[type=checkbox]").check();
    });
    cy.contains("Delete").click();
    cy.contains(listName).should("not.exist");
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

  it("Successfully clears invalid anonymous token using dialog", function() {
    cy.server();
    cy.route("GET", "*/service/search?*").as("getSearch");

    cy.window().then(win => {
      win.bluegenes.events.scrambleTokens();
    });

    cy.get(".home .search").within(() => {
      cy.get("input[class=typeahead-search]").type("zen{enter}");
    });

    cy.wait("@getSearch");
    cy.get("@getSearch").should(xhr => {
      expect(xhr.status).to.equal(401);
    });

    cy.route("GET", "*/service/session?*").as("getSession");

    cy.contains("Refresh").click();

    cy.wait("@getSession");

    cy.contains("Home").click();
    cy.get(".home .search").within(() => {
      cy.get("input[class=typeahead-search]").clear().type("adh{enter}");
    });

    cy.wait("@getSearch");
    cy.get("@getSearch").should(xhr => {
      expect(xhr.status).to.equal(200);
    });
  });
});
