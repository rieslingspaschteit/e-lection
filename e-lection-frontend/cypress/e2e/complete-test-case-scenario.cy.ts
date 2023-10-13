import path = require('path')
import { getCypressDateString } from '../support/e2e'

describe('complete test election run', { taskTimeout: 90000 }, () => {
  interface user {
    email: string
    password: string
  }

  let authority: user
  let voter1: user
  let voter2: user
  let voter3: user
  let voter4: user
  let trustee1: user
  let trustee2: user

  let endDate: Date

  let testDir: string
  let cli: string

  before(() => {
    cy.fixture('users').then(users => {
      authority = users.authority
      voter1 = users.voter1
      voter2 = users.voter2
      voter3 = users.voter3
      voter4 = users.voter4
      trustee1 = users.trustee1
      trustee2 = users.trustee2
    })
    cy.task('resetDatabase')
    cy.task('resetOrInitTestDir', 'complete-e2e').then(res => {
      testDir = (res as any).dirPath
    })
    cli = Cypress.env('cliJar') as string
  })

  it('creates sessions', () => {
    cy.login(authority.email, authority.password)
    cy.login(trustee1.email, trustee1.password)
    cy.login(trustee2.email, trustee2.password)
    cy.login(voter1.email, voter1.password)
    cy.login(voter2.email, voter2.password)
    cy.login(voter3.email, voter3.password)
    cy.login(voter4.email, voter4.password)
  })

  it('setups directories', () => {
    cy.task('resetOrInitTestDir', 'complete-e2e/trustees')
      .then(() => {
        cy.task('resetOrInitTestDir', 'complete-e2e/trustees/trustee1')
        cy.task('resetOrInitTestDir', 'complete-e2e/trustees/trustee2')
      })
    cy.task('resetOrInitTestDir', 'complete-e2e/voters')
      .then(() => {
        cy.task('resetOrInitTestDir', 'complete-e2e/voters/voter1')
        cy.task('resetOrInitTestDir', 'complete-e2e/voters/voter2')
        cy.task('resetOrInitTestDir', 'complete-e2e/voters/voter3')
        cy.task('resetOrInitTestDir', 'complete-e2e/voters/voter4')
      })
  })

  it('creates a new election', () => {
    cy.login(authority.email, authority.password)

    cy
      .visit('/#/')
      .get('#nav-bar')
      .get('#authority')
      .click()

    cy.get('#create-btn').click()

    cy.get('#title').type('test title')
    cy.get('#description').type('test description')

    // const endDate = new Date(new Date().getTime() + (60 * 60 * 1000))
    // const localEndDate = new Date(endDate.getTime() - new Date().getTimezoneOffset() * 60000)

    endDate = new Date()
    endDate.setSeconds(endDate.getSeconds() + 120)

    cy.get('#end').focus().type(getCypressDateString(endDate))

    cy.fixture('voters.txt').then(voters => {
      cy.get('#voters').selectFile({
        fileName: 'voters.txt',
        contents: Cypress.Buffer.from(voters)
      })
    })

    cy.fixture('trustees.txt').then(trustees => {
      cy.get('#trustees').selectFile({
        fileName: 'trustees.txt',
        contents: Cypress.Buffer.from(trustees)
      })
    })

    cy.get('#question').type('question title')

    cy
      .get('.options-container')
      .first()
      .children()
      .first()
      .type('option 1')

    cy
      .get('.option-container')
      .last()
      .children()
      .first()
      .type('option 2')

    cy.get('#max').type('1')

    cy
      .get('#create').children()
      .contains('Wahl erstellen')
      .click()

    cy
      .get('#success-message')
      .should('be.visible')
  })

  it('carries out key-ceremony', () => {
    uploadAuxKeys('trustee1', trustee1.email, trustee1.password)

    cy.contains('Alle Schlüssel für die aktuelle Phase abgegeben')
      .should('be.visible')

    uploadAuxKeys('trustee2', trustee2.email, trustee2.password)

    cy.contains('Key Ceremony Phase 2')
      .should('be.visible')

    uploadEPKB('trustee2')

    cy.contains('Alle Schlüssel für die aktuelle Phase abgegeben')
      .should('be.visible')

    cy.login(trustee1.email, trustee1.password)
    cy.visit('/#/').contains('Verschlüsselung').click()
    cy.get('#view-election-1').focus().click()

    // hack for button somehow not working on Debian-chrome
    cy.url().then((url: string) => {
      if (!url.includes('key-ceremony')) {
        console.log('button did not work :(')
        cy.visit('/#/trustee/1/key-ceremony')
      }
    })

    uploadEPKB('trustee1')

    cy.contains('Key Ceremony ist abgeschlossen')
      .should('be.visible')

    downloadBackups(trustee1, 'trustee1')
    downloadBackups(trustee2, 'trustee2')
  })

  const uploadAuxKeys = (name: string, email: string, password: string): void => {
    cy.login(email, password)

    cy.visit('/#/').contains('Verschlüsselung').click()

    cy.contains('test title').should('be.visible')

    cy.get('#view-election-1').focus().dblclick()

    // hack for button somehow not working on Debian-chrome
    cy.url().then((url: string) => {
      if (!url.includes('key-ceremony')) {
        console.log('button did not work :(')
        cy.visit('/#/trustee/1/key-ceremony')
      }
    })

    const targetDir = testDir + '/trustees/' + name

    cy.exec(`java -jar ${cli} aux targetdir=${targetDir}`)
      .its('stdout')
      .should('contain', 'Success')

    cy.readFile(targetDir + '/aux_public.json').as('aux-key')
    cy.get('#aux_key').selectFile('@aux-key')
  }

  const uploadEPKB = (name: string): void => {
    cy.task('clearDownloads').wait(4000)

    cy.get('#download-aux-keys').focus().click()

    const zipFilePath = './cypress/downloads/aux_keys.zip'
    cy.readFile(zipFilePath).should('exist')
    const destination = path.normalize(testDir + '/trustees/' + name)
    cy.task('unzipFiles', { zipFilePath, destination })

    cy.readFile(destination + '/aux_keys.json')
      .should('exist')

    cy.exec(`java -jar ${cli} electionkey sourcedir=${destination} targetdir=${destination}`)
      .its('stdout').should('contain', 'Success')

    cy.readFile(destination + '/ceremony_public.json').as('epkb')
    cy.get('#epkb').selectFile('@epkb')
  }

  const downloadBackups = (trustee: user, name: string): void => {
    cy.login(trustee.email, trustee.password)

    cy.task('clearDownloads').wait(3000)

    cy.visit('/#/trustee/1/key-ceremony')
      .contains('Herunterladen')
      .click()

    const zipFilePath = './cypress/downloads/backup_keys.zip'
    cy.readFile(zipFilePath).should('exist')
    const destination = path.normalize(testDir + '/trustees/' + name)
    cy.task('unzipFiles', { zipFilePath, destination })
    cy.readFile(destination + '/backup_keys.json')
      .should('exist')
  }

  it('opens the election', () => {
    cy.login(authority.email, authority.password)

    cy.visit('/#/')
      .get('#nav-bar')
      .get('#authority')
      .click()

    cy.contains('eröffnen').focus().dblclick()
    cy.get('.due-date').should('contain', 'Offen')
  })

  it('votes for the election', () => {
    vote(voter1, 'voter1', 1)
    vote(voter2, 'voter2', 2)
    vote(voter3, 'voter3', 2)
    vote(voter4, 'voter4', 1)
  })

  const vote = (voter: user, name: string, option: number): void => {
    cy.login(voter.email, voter.password)

    cy.visit('/#/')
      .contains('test title')
      .should('be.visible')

    cy.get('#view-election-1').click()

    cy.url({ timeout: 4000 }).should('contain', '/dashboard')

    cy.contains('Stimme abgeben').click()

    cy.url({ timeout: 4000 }).should('contain', '')

    cy.get('h1').should('contain', 'test title')

    cy.get(`:nth-child(${2 + option}) > label > .checkable > .unchecked`)
      .click()

    cy.contains('Verschlüsseln').click()

    cy.contains('Die Auswahl wurde erfolgreich verschlüsselt')
      .should('be.visible')

    cy.get('#code').then(code => {
      const savedCodeFile = testDir + '/voters/' + name + '/saved-tracking-code.txt'
      cy.writeFile(savedCodeFile, code)
    })

    cy.get('#buttons > :nth-child(2)').click()

    cy.contains('Der Stimmzettel wurde abgegeben')
      .should('be.visible')
  }

  it('waits for the election to end', () => {
    cy.log(`end-date: ${endDate.toString()}`)
    const now = new Date()
    const diffTime = Math.abs(endDate.getTime() - now.getTime())
    cy.log(`need to wait for ${diffTime} ms`)
    cy.task('waitForPSEToEndGracefully', diffTime).then(() => {
      cy.log(`now its: ${endDate.toLocaleString()}`)
    })
  })

  it('decrypts the election', () => {
    decryptForTrustee('trustee1', trustee1)
    decryptForTrustee('trustee2', trustee2)
  })

  const decryptForTrustee = (name: string, trustee: user): void => {
    cy.login(trustee.email, trustee.password)

    cy.visit('/#/trustee')

    cy.contains('P_DECRYPTION')
      .should('be.visible')

    cy.get('#view-election-1').focus().click()
    cy.url({ timeout: 4000 }).should('contain', '/trustee/1/decryption')
    cy.get('h1').should('contain', 'Ergebnis entschlüsseln')

    cy.task('clearDownloads').wait(2000)
    cy.get('#result').click()

    const zipFilePath = './cypress/downloads/result.zip'
    cy.readFile(zipFilePath).should('exist')

    const trusteeDir = testDir + '/trustees/' + name

    const destination = path.normalize(testDir + '/trustees/' + name)
    cy.task('unzipFiles', { zipFilePath, destination }).wait(2000)

    const encryptedTalliesFile = trusteeDir + '/encrypted_tallies.json'
    cy.readFile(encryptedTalliesFile).should('exist')

    cy.exec(`java -jar ${cli} decrypt sourcedir=${trusteeDir} targetdir=${trusteeDir}`)
      .its('stdout').should('contain', 'Success')

    cy.readFile(trusteeDir + '/partial_decryption.json').as('partial_decryption.json')
    cy.get('#decryption-upload').selectFile('@partial_decryption.json')

    cy.get('#success-message').should('be.visible')
  }

  it('views results', () => {
    cy.login(voter1.email, voter1.password)

    cy.visit('/#/').get('.due-date')
      .should('contain', 'Geschlossen')

    cy.get('#view-election-1').click()

    cy.url().should('contain', '/dashboard')
    cy.get('#election-state-value').should('contain.text', 'FINISHED')
    cy.get('#options-container > :nth-child(2) > .value').should('contain', 2)
    cy.get('#options-container > :nth-child(3) > .value').should('contain', 2)

    cy.get('#record').click()
    cy.readFile('./cypress/downloads/record.zip')
      .should('exist')
  })
})
