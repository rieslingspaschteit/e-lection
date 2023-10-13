describe('test interaction with the e-lection-cli', () => {
  it('tests cy.exec', () => {
    cy.exec('echo "hi"')
      .its('stdout')
      .should('contain', 'hi')

    cy.exec('java --version')
      .then(res => cy.log(res.stdout))
  })

  it('generates aux-keys', () => {
    cy.task('resetOrInitTestDir', 'jarTest').then(res => {
      const dirPath = (res as any).dirPath as string

      const cliJar = Cypress.env('cliJar') as string
      const targetDir = dirPath

      cy.exec(`java -jar ${cliJar} aux targetdir=${targetDir}`)
        .its('stdout')
        .should('include', 'Success')
      cy.readFile(targetDir + '/aux_public.json')
        .its('keyType')
        .should('eq', 'RSA')
    })
    // cy.exec('java -jar ' + Cypress.env('cliJar') + ' aux targetdir=' + Cypress.env('targetDir'))
  })
})
