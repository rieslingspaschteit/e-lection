// TODO figure out where to put these

import axios from 'axios'

/**
 * Checks if two maps share the same indices, including nested indices.
 *  Also checks if the values to all the indices are not undefined if both maps have the same depth of nesting
 * @param map1 firt map
 * @param map2 second map
 * @returns
 */
export function indicesMatch (map1: ReadonlyMap<any, any>, map2: ReadonlyMap<any, any>): boolean {
  const keys1: any[] = Array.from(map1.keys())
  const keys2: any[] = Array.from(map2.keys())
  if (keys1.length !== keys2.length) {
    return false
  }
  for (const key of map1.keys()) {
    if (map1.get(key) === undefined || map2.get(key) === undefined) {
      return false
    }
    if ((map1.get(key) instanceof Map<any, any>) && (map2.get(key) instanceof Map<any, any>)) {
      if (!indicesMatch(map1.get(key) as Map<any, any>, map2.get(key) as Map<any, any>)) {
        return false
      }
    }
  }
  return true
}

export function intToHex (value: bigint): string {
  const values = ['0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F']
  let remainingValue = value
  let out: string = ''
  const base: bigint = BigInt(16)
  while (remainingValue > 0) {
    out = values[Number(remainingValue % base)] + out
    remainingValue = remainingValue / base
  }
  return out
}

export class ConfigProvider {
  private static instance: ConfigProvider
  private readonly _config: Promise<{ backendServer: string }>

  private constructor () {
    this._config = ConfigProvider.getConfig().then(response => {
      return {
        backendServer: response.backendServer
      }
    })
  }

  public static getInstance (): ConfigProvider {
    if (ConfigProvider.instance === undefined) {
      ConfigProvider.instance = new ConfigProvider()
    }
    return ConfigProvider.instance
  }

  public get config (): Promise<{ backendServer: string }> {
    return this._config
  }

  private static async getConfig (): Promise<{ backendServer: string }> {
    return await axios.get('/appconfig.json')
      .then(response => {
        console.log('fetched config file: ', JSON.stringify(response.data, undefined, ' '))
        return {
          backendServer: response.data.backendUrl as string
        }
      })
  }
}

export const getLocalDateTimeString = (): string => {
  const date = new Date()
  date.setHours(date.getHours() + 1)
  const year = date.toLocaleString('en-US', { year: 'numeric' })
  const month = date.toLocaleString('en-US', { month: '2-digit' })
  const day = date.toLocaleString('en-US', { day: '2-digit' })
  const hour = date.toLocaleTimeString('en-US', { hour: '2-digit', hour12: false })
  let minute = date.toLocaleTimeString('en-US', { minute: '2-digit' })

  if (minute.length < 2) minute = '0' + minute // wtf???

  return `${year}-${month}-${day}T${hour}:${minute}`
}
