describe("Regions Export Test", function(){
    beforeEach(function(){
        cy.visit("/biotestmine/regions");
        cy.get(".input-section").within(() => {
            cy.contains("Show Example").click();
            cy.get("textarea").should("include.text","MAL");
            cy.intercept("POST","/biotestmine/service/query/results").as("regionSearch");
            cy.get("button").filter(':contains("Search")').click();
            cy.wait("@regionSearch");
        })
    });

    it("can export results of region search in TAB", function(){
        cy.window().document().then(function (doc) {
            doc.addEventListener('click', () => {
              setTimeout(function () { doc.location.reload() }, 5000)
            })
        })
        cy.get(".results-actions").within(() => {
            cy.contains("TAB").click();
        })
        cy.readFile("cypress/downloads/result.tsv").then(newResult => {
            cy.readFile("cypress/fixtures/result.tsv").should("eq",newResult);
        })
    })

    it("can export results of region search in CSV", function(){
        cy.window().document().then(function (doc) {
            doc.addEventListener('click', () => {
              setTimeout(function () { doc.location.reload() }, 5000)
            })
        })
        cy.get(".results-actions").within(() => {
            cy.contains("CSV").click();
        })
        cy.readFile("cypress/downloads/result.csv").then(newResult => {
            cy.readFile("cypress/fixtures/result.csv").should("eq",newResult);
        })
    })

    it("can export results of region search in GFF3", function(){
        cy.intercept('**/service/query/results/**').as('records');
        cy.window().document().then(function (doc) {
            doc.addEventListener('click', () => {
              setTimeout(function () { doc.location.reload() }, 5000)
            })
        })
        cy.get(".results-actions").within(() => {
            cy.contains("GFF3").click();
        })
        cy.wait('@records').its('request').then((req) => {
            cy.request(req)
            .then(({ body, headers }) => {
                cy.readFile("cypress/fixtures/result.gff3").should("eq",body);
            })
        })
    })
    it("can export results of region search in BED", function(){
        cy.intercept('**/service/query/results/**').as('records');
        cy.window().document().then(function (doc) {
            doc.addEventListener('click', () => {
              setTimeout(function () { doc.location.reload() }, 5000)
            })
        })
        cy.get(".results-actions").within(() => {
            cy.contains("BED").click();
        })
        cy.wait('@records').its('request').then((req) => {
            cy.request(req)
            .then(({ body, headers }) => {
                cy.readFile("cypress/fixtures/result.bed").should("eq",body);
            })
          })
    })
    it("can export results of region search in FASTA", function(){
        cy.intercept('**/service/query/results/**').as('records');
        cy.window().document().then(function (doc) {
            doc.addEventListener('click', () => {
              setTimeout(function () { doc.location.reload() }, 5000)
            })
        })
        cy.get(".results-actions").within(() => {
            cy.contains("FASTA").click();
        })
        cy.wait('@records').its('request').then((req) => {
            cy.request(req)
            .then(({ body, headers }) => {
                cy.readFile("cypress/fixtures/result.fa").should("eq",body);
            })
          })
    })

})
