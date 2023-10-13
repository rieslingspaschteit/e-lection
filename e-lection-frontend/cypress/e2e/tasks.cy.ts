describe('test cy tasks', () => {
  it('test db connection and creates, queries and drops table', () => {
    cy.task('resetDatabase')
  })

  it('test init and reset of test dir', () => {
    cy.task('resetOrInitTestDir', 'taskTest').then(res => {
      const dirPath = (res as any).dirPath as string
      expect(dirPath).to.include('taskTest')
      cy.writeFile(dirPath + '/testFile', 'some content')
      cy.readFile(dirPath + '/testFile').should('exist')
    })

    cy.task('resetOrInitTestDir', 'taskTest').then(res => {
      const dirPath = (res as any).dirPath as string
      cy.readFile(dirPath + 'testFile').should('not.exist')
    })
  })
})
