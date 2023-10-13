// ***********************************************************
// This example support/e2e.ts is processed and
// loaded automatically before your test files.
//
// This is a great place to put global configuration and
// behavior that modifies Cypress.
//
// You can change the location of this file or turn off
// automatically serving support files with the
// 'supportFile' configuration option.
//
// You can read more here:
// https://on.cypress.io/configuration
// ***********************************************************

// Import commands.js using ES2015 syntax:
import './commands'

// Alternatively you can use CommonJS syntax:
// require('./commands')

export const getCypressDateString = (date: Date): string => {
  const year = date.toLocaleString('en-US', { year: 'numeric' })
  const month = date.toLocaleString('en-US', { month: '2-digit' })
  const day = date.toLocaleString('en-US', { day: '2-digit' })
  const hour = date.toLocaleTimeString('en-US', { hour: '2-digit', hour12: false })
  let minute = date.toLocaleTimeString('en-US', { minute: '2-digit' })
  let second = date.toLocaleTimeString('en-US', { second: '2-digit' })

  if (minute.length < 2) minute = '0' + minute // wtf???
  if (second.length < 2) second = '0' + second

  return `${year}-${month}-${day}T${hour}:${minute}:${second}`
}
