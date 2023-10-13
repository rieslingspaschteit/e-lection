import fs from 'fs'
import path from 'path'
import { createConnection } from 'mysql2/promise'
import unzipper from 'unzipper'

export interface ConnectionOptions {
  host: string
  user: string
  password: string
  database: string
}

class File {
  private readonly _subDir?: File[]
  constructor (
    private readonly _name: string,
    private readonly _isFile: boolean
  ) {
    if (!this.isFile) {
      this._subDir = []
    }
  }

  public get name (): string {
    return this._name
  }

  public get isFile (): boolean {
    return this._isFile
  }

  public get subDir (): File[] {
    if (this._subDir === undefined) throw new Error(`${this.name} is not a directory!`)
    return this._subDir
  }
}

export const resetDatabase = async (options: ConnectionOptions): Promise<void> => {
  if (options.database === undefined) {
    throw new Error('database is not set in env.db')
  }

  const connection = await createConnection(options)

  const tablesQuery = `select table_name from information_schema.tables where table_schema = '${options.database}'`
  const tables = await connection.query(tablesQuery).then(([rows, _fields]) => rows as any[])

  const queries = tables
    .filter(entry => (entry.TABLE_NAME as string).toLocaleLowerCase() !== 'authority')
    .map(entry => 'TRUNCATE TABLE '.concat(entry.TABLE_NAME))

  await connection.query('SET FOREIGN_KEY_CHECKS = 0')

  for (let i = 0; i < queries.length; i++) {
    await connection.query(queries[i])
  }

  await connection.query('SET FOREIGN_KEY_CHECKS = 1')
  await connection.end()
}

export const deleteFilesRecursive = (dirPath: string, deletedFiles: File[]): File[] => {
  fs.readdirSync(dirPath).forEach(file => {
    const subFile = path.join(dirPath, file)
    if (fs.lstatSync(subFile).isDirectory()) {
      const deletedFile = new File(file, false)
      deletedFiles.push(deletedFile)
      deleteFilesRecursive(subFile, deletedFile.subDir)
    } else {
      fs.unlinkSync(subFile)
      deletedFiles.push(new File(file, true))
    }
  })
  fs.rmdirSync(dirPath)
  return deletedFiles
}

export const unzipFiles = (zipFilePath: string, destination: string): void => {
  fs.createReadStream(zipFilePath)
    .pipe(unzipper.Extract({ path: path.normalize(destination) }))
}

export const wait = async (ms: number): Promise<unknown> => {
  return await new Promise((resolve) => {
    setTimeout(resolve, ms)
  })
}
