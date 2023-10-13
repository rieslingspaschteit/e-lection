// ***********************************************
// This example commands.ts shows you how to
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
Cypress.Commands.add('login', (email, password) => {
  cy.session([email, password], () => {
    const testProvider = 'auth0'
    const credentials = { email, password }
    // visit the start page and get redirected to '/login'
    cy.visit('/')

    // click on auth0 provider to get redirected to the auth0 login page
    cy.contains(testProvider).click()

    // enter testUser credentials
    cy.origin(
      'https://dev-kvj23udrb4zvaxe2.us.auth0.com',
      { args: credentials },
      ({ email, password }) => {
        cy.get('#username').type(email)
        cy.get('#password').type(password)

        // submit for login
        cy.contains('Continue').click()
      })

    cy.url().should('contain', '/')

    cy.log(`logged in user: {email: ${email}, password: ${password}}`)
  })
})

//
// -- This is a child command --
// Cypress.Commands.add('drag', { prevSubject: 'element'}, (subject, options) => { ... })
//
//
// -- This is a dual command --
// Cypress.Commands.add('dismiss', { prevSubject: 'optional'}, (subject, options) => { ... })
//
//
// -- This will overwrite an existing command --
// Cypress.Commands.overwrite('visit', (originalFn, url, options) => { ... })
//
declare global {
  // eslint-disable-next-line @typescript-eslint/no-namespace
  namespace Cypress {
    interface Chainable {
      login: (email: string, password: string) => Chainable<void>
      // drag(subject: string, options?: Partial<TypeOptions>): Chainable<Element>
      // dismiss(subject: string, options?: Partial<TypeOptions>): Chainable<Element>
      // visit(originalFn: CommandOriginalFn, url: string, options: Partial<VisitOptions>): Chainable<Element>
    }
  }
}

export {}
