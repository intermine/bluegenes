describe("Query view test", function() {
    beforeEach(function() {
      cy.visit("/biotestmine/querybuilder");
    });

    // it.only("can change the order of attributes", function(){
    //   cy.viewport(1000, 600);
    //   cy.get(".model-browser-root").within(() => {
    //     cy.selectFromDropdown("Protein");        
    //     cy.contains("Add summary").click();
    //   });

    //   cy.get("div.panel-body").first().as("queryEditorTab").within(()=>{
    //     cy.contains("Manage Columns").click();
    //     cy.get(".sort-order-container > :nth-child(4)").drag(".sort-order-container > :nth-child(3)",{ position: 'topLeft' });
    //     cy.get(".sort-order-container > :nth-child(3)").drag(".sort-order-container > :nth-child(2)",{ position: 'topLeft' });
    //   });
    // })
    
    //Query editor 
    it("can save, clear, and load query", function() {
      cy.get(".model-browser-root").within(() => {
        cy.selectFromDropdown("Protein");        
        cy.contains("Add summary").click();
      });

      cy.get("div.panel-body").first().as("queryEditorTab").within(()=>{
        cy.contains("Save Query").click();
        cy.get("div.input-group").type("Protein summary", {delay:100});
        cy.contains("Save Query").click();
        cy.contains("Clear Query").click();
      });
        
      cy.contains("Saved Queries").click();
      cy.get("table").contains("td","Protein summary"); //Assertion
      cy.contains("Load").click();

      cy.get("@queryEditorTab").within(() => {
        cy.get(".qb-label").should("include.text","Protein");
        cy.get(".qb-label").should("include.text","Organism");
      })
    });

    it("can remove attributes on query editor", function(){
      cy.get(".model-browser-root").within(() => {
        cy.selectFromDropdown("Protein");        
        cy.get("select").should("have.value", "Protein");
        cy.contains("Add summary").click();
      });

      cy.get("div.panel-body").first().as("queryEditorTab").within(() => {
        cy.contains('Protein').parentsUntil('.lab qb-class').children('.icon.icon-bin').click();
        cy.get("p").should("include.text","Please select at least one attribute from the Model Browser on the left.");
      });
    });

    //Manage columns
    it("can sort attribute alphabetically", function(){
      cy.get(".model-browser-root").within(() => {
        cy.selectFromDropdown("Protein");        
        cy.get("select").should("have.value", "Protein");
        cy.contains("Add summary").click();
      });

      cy.get('.query-view-column').within(() => {
        cy.contains('Manage Columns').click();
        cy.get('.sort-item').filter(':contains("Length")')
          .find('.button-group > [title="Sort ascending"]').click();
        cy.get('.sort-priority').should('have.text','Sort 1st');
      });
    });

    //Preview tab
    it("can preview the results table of the query", function(){
      cy.get(".model-browser-root").within(() => {
        cy.selectFromDropdown("Protein");        
        cy.get("select").should("have.value", "Protein");
        cy.contains("Add summary").click();
      });

      cy.get(".panel-body").eq(1).as("previewTab").within(() => {
        cy.contains("Preview").click();
        cy.get("table").find("th").should("include.text","Protein > DB identifier");
        cy.get("table").find("th").should("include.text","Protein > Primary Accession");
      })
    })

    //XML tab
    it('can copy XML of the query', function(){
      cy.get(".model-browser-root").within(() => {
        cy.selectFromDropdown("Protein");        
        cy.get("select").should("have.value", "Protein");
        cy.contains("Add summary").click();
      });

      cy.get('.query-view-column').within(() => {
        cy.get('.qb-label').should('include.text','Protein');
        cy.contains('XML').click();
        cy.contains('Copy XML').click();
        cy.get('.success').should('have.text','Copied to clipboard');
      });
    });

    //Saved queries tab
    it('can rename a saved query', function(){
      cy.get('.model-browser-root').within(() => {
        cy.selectFromDropdown("Gene");        
        cy.contains("Add summary").click();
      });
 
      cy.get('.query-view-column').within(()=>{
        cy.contains("Save Query").click();
        cy.get("div.input-group").click().type("Protein summary",{delay:100});
        cy.contains("Save Query").click();
        cy.contains("Clear Query").click();
        
        cy.contains("Saved Queries").click();
        cy.get('table').contains('td','Protein summary'); //Assertion

        cy.contains('Rename').click();
        cy.get('input.form-control').clear();
        cy.get('input.form-control').click().type('Gene summary{enter}',{delay:100});
        cy.get('.query-table').contains('td', 'Gene summary');      
      });
    });

    it('can delete a saved query', function(){
      cy.get('.model-browser-root').within(() => {
        cy.selectFromDropdown("Gene");        
        cy.contains("Add summary").click();
      });

      cy.get(".panel-body").first().as("queryEditorTab").within(() => {
        cy.contains("Save Query").click();
        cy.get("div.input-group").click().type("Protein summary",{delay:100});
        cy.contains("Save Query").click();
      })

      cy.get(".panel-body").last().as("savedQueriesTab").within(() => {
        cy.contains("Saved Queries").click();
        cy.get('table').find('td').should("include.text","Protein summary"); //Assertion
        cy.contains('Delete').click();    
      })
      cy.get(".alert").should("include.text","The query Protein summary has been deleted");
    });

    //Import from XML tab
    it('can load query from imported XML', function(){
      cy.get('.query-view-column').within(() => {
        cy.contains('Import from XML').click();
        cy.get('textarea.form-control').click()
          .type('<query model="genomic" view="Protein.primaryIdentifier Protein.organism.name Protein.length" sortOrder=""></query>');
        cy.contains('Load query').click();
        cy.get('.success').should('have.text','XML loaded successfully');
        cy.get('.qb-label').should('include.text','Protein');
        cy.get('.qb-label').should('include.text','Length');
        cy.get('.qb-label').should('include.text','DB identifier');
        cy.get('.qb-label').should('include.text','Organism');
        cy.get('.qb-label').should('include.text','Name');
      });
    });
});

