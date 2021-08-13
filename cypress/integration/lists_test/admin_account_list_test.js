describe("Admin account list test", function() {
    beforeEach(function() {
        cy.loginToAdminAccount();
    });

    after(function() {
        cy.get(".logon.dropdown.success").click().should('contain', 'test_user@mail_account');
        cy.contains("Lists").click();
        cy.url().should("include","/lists");
        cy.get(".lists-headers").within(() => {
            cy.get('[type="checkbox"]').check();
        })
        cy.get('.bottom-controls').within(() => {
            cy.contains("Delete").click({delay:100});
        })
        cy.get('.modal-footer').within(() => {
            cy.contains("Delete list(s)").click({delay:100});
        })
        cy.get('.no-lists').should('exist');
    });

    it("can make a list public",function(){
        cy.createGeneList("SODB, GBP, GST, CDPK1");
        cy.url().should("include","/lists");
        cy.get('.lists-item').its("length").should('be.gt',0);

        cy.get(".lists-item").first().within(() => {
            cy.get("button").find('svg[class="icon icon-list-more"]')
            .should("be.visible")
            .click();

            cy.get("button").find('svg[class="icon icon-list-edit"]')
            .should("be.visible")
            .first()
            .click();
        });

        cy.get(".modal-body").within(()=>{
            cy.contains("Tags").parent().find("input").type("im:public{enter}",{delay:100});
        })
        cy.get('.modal-footer').within(()=>{
            cy.contains("Save").click();
        })
        cy.get(".logon.dropdown.success").click().within(() =>{
            cy.contains("Logout").click();
        });
        cy.get(".logon.dropdown.warning").click().should("exist");
        cy.get('.lists-item').its("length").should("be.gt",0);
    })

    it("can add tags to a list",function(){
        cy.createGeneList("SODB, GBP, GST, CDPK1");
        cy.url().should("include","/lists");
        cy.get('.lists-item').its("length").should('be.gt',0);

        cy.get(".lists-item").first().within(() => {
            cy.get("button").find('svg[class="icon icon-list-more"]')
            .should("be.visible")
            .click();

            cy.get("button").find('svg[class="icon icon-list-edit"]')
            .should("be.visible")
            .first()
            .click();
        });

        cy.get(".modal-body").within(()=>{
            cy.contains("Tags").parent().find("input").type("Major genes{enter}",{delay:100});
        })
        cy.get('.modal-footer').within(()=>{
            cy.contains("Save").click();
        })
        cy.get(".lists-table").within(()=>{
            cy.get(".tag").should("contain","Major genes");
        })
    })

    it("can filter lists by tags", function(){
        cy.contains("Lists").click();
        cy.url().should("include","/lists");
        cy.get('.lists-item').its("length").should('be.gt',0);

        cy.get('.lists-headers').within(() => {
            cy.contains("Tags").siblings(".dropdown").click();
            cy.get("li").contains("Major genes").click();
        })
        cy.get('.lists-item').its("length").should('eq',1);
    })

    it("can move list(s) to folder",function(){
        cy.contains("Lists").click();
        cy.url().should("include","/lists");
        cy.get('.lists-item').its("length").should('be.gt',0);

        cy.get('.lists-headers').within(() => {
            cy.get('[type="checkbox"]').check();
        })
        cy.get('.bottom-controls').within(() => {
            cy.contains("Move all").click();
        })
        cy.get(".css-1wy0on6").click().type("List of common genes{enter}",{delay:100},{force:true});
        cy.get(".modal-footer").within(() => {
            cy.contains("Move list(s)").click();
        })
        cy.get('.list-title').should("contain","List of common genes");
    })

    it("can copy list(s) to folder", function(){
        cy.contains("Lists").click();
        cy.url().should("include","/lists");
        cy.get('.lists-item').its("length").should('be.gt',0);

        cy.get('.lists-headers').within(() => {
            cy.get('[type="checkbox"]').check();
        })
        cy.get('.bottom-controls').within(() => {
            cy.contains("Copy all").click();
        })
        cy.get(".css-1wy0on6").click().type("List 2{enter}",{delay:100},{force:true});
        cy.get(".modal-footer").within(() => {
            cy.contains("Copy list(s)").click();
        })
        cy.get('.list-title').should("contain","List 2");
    })

    it("can create subfolders and view folder contents", function(){
        cy.contains("Lists").click();
        cy.url().should("include","/lists");
        cy.get('.lists-item').its("length").should('be.gt',0);

        cy.get('.lists-headers').within(() => {
            cy.get('[type="checkbox"]').check();
        })
        cy.get('.bottom-controls').within(() => {
            cy.contains("Move all").click();
        })
        cy.get(".css-1wy0on6").click().type("List 3{enter}",{delay:100},{force:true});
        cy.get(".css-1hwfws3").click().type("Subfolder 1{enter}",{delay:100},{force:true});
        cy.intercept("POST","/biotestmine/service/list/tags").as("listLoad");
        cy.get(".modal-footer").within(() => {
            cy.contains("Move list(s)").click();
        })
        cy.wait("@listLoad");

        cy.get('.list-title').should("contain","List 3");
        cy.get('.icon-expand-folder').eq(0).click();
        cy.get('.list-title').should("contain","Subfolder 1");
        cy.get('.icon-expand-folder').last().click();
        cy.get('.list-title').should("contain","Gene list");
    })
});