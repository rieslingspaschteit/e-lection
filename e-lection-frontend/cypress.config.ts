import { defineConfig } from 'cypress'
import * as fs from 'fs'
import path from 'path'
import * as utils from './cypress/support/e2e-utils'

export default defineConfig({
  projectId: 'zicgtu',
  e2e: {
    setupNodeEvents (on, config) {
      on('task', {

        resetDatabase: async () => {
          if (config.env.isCi === true) return false
          return await utils.resetDatabase(config.env.db)
            .then(() => true)
        },

        resetOrInitTestDir: (dirName: string) => {
          const cyOut = path.resolve(__dirname, config.env.testDirName)
          if (!fs.existsSync(cyOut)) {
            fs.mkdirSync(cyOut)
          }

          const testDir = path.resolve(cyOut, dirName)
          let retVal
          if (fs.existsSync(testDir)) {
            retVal = {
              message: '',
              dirPath: testDir,
              removedFiles: utils.deleteFilesRecursive(testDir, [])
            }
          }

          fs.mkdirSync(testDir)

          retVal = {
            message: 'created new directory',
            dirPath: testDir
          }

          return retVal
        },

        unzipFiles: (args: { zipFilePath: string, destination: string }) => {
          try {
            utils.unzipFiles(args.zipFilePath, args.destination)
            return true
          } catch (error) {
            return {
              message: `the task failed, unzipFiles(${args.zipFilePath}, ${args.destination}) threw error`,
              error
            }
          }
        },

        clearDownloads: () => {
          const downloads = path.resolve(__dirname, 'cypress/downloads')
          const clearedDownloads: any[] = []
          if (fs.existsSync(downloads)) {
            fs.readdirSync(downloads).forEach(file => {
              const subFile = path.join(downloads, file)
              if (fs.lstatSync(subFile).isDirectory()) {
                const deleted = utils.deleteFilesRecursive(subFile, [])
                clearedDownloads.push(deleted)
              } else {
                fs.unlinkSync(subFile)
                clearedDownloads.push(subFile)
              }
            })
          }
          return clearedDownloads
        },

        waitForPSEToEndGracefully: async (diffTime: number) => {
          await utils.wait(diffTime)
          return null
        }

      })
    },
    baseUrl: 'http://localhost:5173',
    watchForFileChanges: false
  },
  env: {
    testDirName: 'cy-out'
  }
})
