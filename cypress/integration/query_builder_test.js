describe("Query builder test", function() {
    beforeEach(function() {
      cy.visit("/");
    });

    it("allows you to select data type and summary attributes with model browser", function() {
      cy.openQueryBuilderTab();
      cy.url().should("include", "/querybuilder");

      cy.get('.model-browser-root').within(() => {
        cy.get('select').then($option => {$option.val("Protein")}).parent().trigger('change');
        cy.contains("Summary").click();
      });
    });

    it("can save and load query", function() {
      cy.openQueryBuilderTab();
      cy.url().should("include", "/querybuilder");

      cy.get('.model-browser-root').within(() => {
        cy.get('select').then($option => {$option.val("Protein")}).parent().trigger('change');
        cy.contains("Summary").click();
      });

      cy.get('div.panel-body').within(()=>{
        cy.contains("Save Query").click();
        cy.get("div.input-group").type("Protein summary", {delay:100});
        cy.contains("Save Query").click();
        cy.contains("Clear Query").click();
      });
        
      cy.contains("Saved Queries").click();
      cy.get('table').contains('td','Protein summary'); //Assertion
      cy.contains('Load').click();
    });

    it("allows you to choose specific attributes", function(){
      cy.openQueryBuilderTab();
      cy.url().should("include", "/querybuilder");

      cy.get('.model-browser-root').within(() => {
        cy.get('select').then($option => {$option.val("Gene")}).parent().trigger('change');
        cy.get('select').should('have.value', 'Gene');
        cy.contains("Summary").click();
      });

      cy.get('.model-browser').within(() => {
        cy.get("ul").its('length').should("be.gt", 0);
        cy.get(".qb-class").contains('UTRs').click();
        cy.get('.expanded-group > ul').find('.qb-label').filter(':contains("Name")').click();
        cy.get('.expanded-group > ul').find('.qb-label').filter(':contains("Length")').click();
        cy.get('.expanded-group > ul').find('.qb-label').filter(':contains("DB identifier")').click();
      });

      cy.get('.query-view-column').within(() => {
        cy.get('.qb-label').should('include.text','Name');
        cy.get('.qb-label').should('include.text','Length');
        cy.get('.qb-label').should('include.text','DB identifier');
      });
    });

    it("allows selection of data type from data model", function(){
      cy.openQueryBuilderTab();
      cy.url().should("include", "/querybuilder");

      cy.contains("Data Model").click();
      cy.contains("Gene").click();
      cy.contains("Summary").click();

      cy.get('.query-view-column').within(() => {
        cy.get('.qb-label').should('include.text','Gene');
      })
    });

    it('can add filters on query editor', function(){
      cy.openQueryBuilderTab();
      cy.url().should("include", "/querybuilder");

      cy.get('.model-browser-root').within(() => {
        cy.get('select').then($option => {$option.val('Gene')}).parent().trigger('change');
        cy.get('select').should('have.value', 'Gene');
        cy.contains("Summary").click();
      });

      cy.get('.query-view-column').within(() => {
        cy.get('.qb-label').should('include.text','Gene');
        cy.get(':nth-child(1) > :nth-child(1) > :nth-child(1) > .lab > .icon-filter').click(); //rewrite
        cy.get('select').select('Not in list');
        cy.get('select').should('have.value','NOT IN');

        cy.contains('DB identifier').parentsUntil('.lab qb-class').children('.icon.icon-filter').click();
        cy.get('select.constraint-chooser').last().select('Contains');
        // cy.wait(500);
        cy.get('input.form-control').click().type("MAL{enter}",{delay:100});
        // cy.get('.panel-body').first().click();
      })
            
    });

    it('can remove attributes on query editor', function(){
      cy.openQueryBuilderTab();
      cy.url().should("include", "/querybuilder");

      cy.get('.model-browser-root').within(() => {
        cy.get('select').then($option => {$option.val('Protein')}).parent().trigger('change');
        cy.get('select').should('have.value', 'Protein');
        cy.contains("Summary").click();
      });

      cy.get('.query-view-column').within(() => {
        cy.contains('Protein').parentsUntil('.lab qb-class').children('.icon.icon-bin').click();
      });
    });

    it('can sort attribute alphabetically', function(){
      cy.openQueryBuilderTab();
      cy.url().should("include", "/querybuilder");
      cy.get('.model-browser-root').within(() => {
        cy.get('select').then($option => {$option.val('Protein')}).parent().trigger('change');
        cy.get('select').should('have.value', 'Protein');
        cy.contains("Summary").click();
      });

      cy.get('.query-view-column').within(() => {
        cy.contains('Manage Columns').click();
        cy.get('.sort-item').filter(':contains("Length")')
          .find('.button-group > [title="Sort ascending"]').click();
        cy.get('.sort-priority').should('have.text','Sort 1st');
      });
    });

    it('can copy XML of the query', function(){
      cy.openQueryBuilderTab();
      cy.url().should("include", "/querybuilder");

      cy.get('.model-browser-root').within(() => {
        cy.get('select').then($option => {$option.val('Protein')}).parent().trigger('change');
        cy.get('select').should('have.value', 'Protein');
        cy.contains("Summary").click();
      });

      cy.get('.query-view-column').within(() => {
        cy.get('.qb-label').should('include.text','Protein');
        cy.contains('XML').click();
        cy.contains('Copy XML').click();
        cy.get('.success').should('have.text','Copied to clipboard');
      });
    });

    it('can load query from imported XML', function(){
      cy.openQueryBuilderTab();
      cy.url().should("include", "/querybuilder");

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

    it('can add details on query editor', function(){
      cy.openQueryBuilderTab();
      cy.url().should("include", "/querybuilder");

      cy.get('.model-browser-root').within(() => {
        cy.get('select').then($option => {$option.val('Protein')}).parent().trigger('change');
        cy.get('select').should('have.value', 'Protein');
        cy.contains("Summary").click();
      });
    
      cy.get('.query-view-column').within(() => {
        cy.contains('Length').parentsUntil('.lab qb-class').children('.icon.icon-filter').click();
        cy.get('select.constraint-chooser').select('>=');
        cy.wait(500);
        cy.get('input.form-control').click().type('450{enter}',{delay:100});
        cy.get('.query-browser').click();
      });
    });

    it('can clear attributes on model browser', function(){
      cy.openQueryBuilderTab();
      cy.url().should("include", "/querybuilder");

      cy.get('.model-browser-column').within(() => {
        cy.get('select').then($option => {$option.val('Protein')}).parent().trigger('change');
        cy.get('select').should('have.value', 'Protein');
        cy.contains("Summary").click();
        cy.get('[title="Remove all selected attributes"]').click();
      });
    });

    it('can rename saved query', function(){
      cy.openQueryBuilderTab();
      cy.url().should("include", "/querybuilder");

      cy.get('.model-browser-root').within(() => {
        cy.get('select').then($option => {$option.val("Gene")}).parent().trigger('change');
        cy.contains("Summary").click();
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
        // cy.contains(/^Save$/).click();
        // cy.get('[title="Load this query"]').find('.td').should('have.text','Gene summary');
        cy.get('.panel-body > .table > tbody > tr > td').first().should('include.text','Gene summary');
      });
    });

});
  