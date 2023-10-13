import { deleteFilesRecursive } from './e2e-utils'
import fs from 'fs'
import path from 'path'
import { expect } from '@jest/globals'

describe('test e2e util functions', () => {
  it('tests deleteFilesRecursive', () => {
    const testDir = path.resolve(__dirname, 'testDir')
    fs.mkdirSync(testDir)
    expect(fs.existsSync(testDir)).toBe(true)

    const testDirFile = path.resolve(testDir, 'testFile.txt')
    fs.writeFileSync(testDirFile, 'test content')
    expect(fs.existsSync(testDirFile)).toBe(true)

    const testSubDir = path.resolve(testDir, 'subDir')
    const testSubDirFile = path.resolve(testDir, 'subDir', 'testFile.txt')

    fs.mkdirSync(testSubDir)
    expect(fs.existsSync(testSubDir)).toBe(true)

    fs.writeFileSync(testSubDirFile, 'test content')
    expect(fs.existsSync(testSubDirFile)).toBe(true)

    expect(() => {
      deleteFilesRecursive(testDir, [])
    }).not.toThrow()

    expect(fs.existsSync(testDir)).toBe(false)
    expect(fs.existsSync(testDirFile)).toBe(false)
    expect(fs.existsSync(testSubDir)).toBe(false)
    expect(fs.existsSync(testSubDirFile)).toBe(false)
  })
})
