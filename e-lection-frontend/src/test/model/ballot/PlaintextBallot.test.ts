import { ConfigError } from '@/app/model/error'
import { PlainTextBallot, Question } from '@/app/model/ballot'

let json1: any
let q1: any
let q2: any
// let plaintextBallot: PlainTextBallot;
let map: Map<number, Question>
let modifiedMap: Map<number, Map<number, number>>
beforeEach(() => {
  json1 = {
    questionText: 'Hello',
    options: ['1', '2', '3'],
    min: 1,
    max: 2
  }
  q1 = Question.fromJSON(json1)
  const map1 = new Map<number, string>()
  map1.set(-3, 'a')
  map1.set(4, 'b')
  q2 = new Question('Bye', map1, 1)
  map = new Map<number, Question>()
  map.set(2, q1)
  map.set(4, q2)

  modifiedMap = new Map<number, Map<number, number>>()
  const innerMap1 = new Map<number, number>()
  const innerMap2 = new Map<number, number>()
  innerMap1.set(0, 0)
  innerMap1.set(1, 0)
  innerMap1.set(2, 0)
  innerMap2.set(-3, 0)
  innerMap2.set(4, 0)
  modifiedMap.set(2, innerMap1)
  modifiedMap.set(4, innerMap2)
})

describe('constructor testing', () => {
  test('constructor doesn\'t throw', () => {
    expect(() => new PlainTextBallot(map)).not.toThrow()
  })

  test('constructor initializes the map correctly', () => {
    const plaintextBallot = new PlainTextBallot(map)
    const optionMap = plaintextBallot.selectedOptions
    expect(optionMap.get(2)?.get(0)).toBe(0)
    expect(optionMap.get(2)?.get(1)).toBe(0)
    expect(optionMap.get(4)?.get(-3)).toBe(0)
    expect(optionMap.get(4)?.get(4)).toBe(0)
  })

  test('constructor does not initialize indices in between', () => {
    const plaintextBallot = new PlainTextBallot(map)
    const optionMap = plaintextBallot.selectedOptions
    expect(optionMap.get(3)).toBe(undefined)
    expect(optionMap.get(1)).toBe(undefined)
    expect(optionMap.get(5)).toBe(undefined)
    expect(optionMap.get(4)?.get(0)).toBe(undefined)
  })
})
describe('isSelected throwing', () => {
  test('isSelected does not throw on invalid question Index', () => {
    const plaintextBallot = new PlainTextBallot(map)
    expect(() => plaintextBallot.isSelected(3, 0)).not.toThrow()
  })

  test('isSelected does not throw on invalid option Index', () => {
    const plaintextBallot = new PlainTextBallot(map)
    expect(() => plaintextBallot.isSelected(4, 0)).not.toThrow()
  })

  test('isSelected does throw on missing option or question Index', () => {
    const plaintextBallot = new PlainTextBallot(map)
    let empty: number
    expect(() => plaintextBallot.isSelected(empty, 0)).toThrow(ConfigError)
    expect(() => plaintextBallot.isSelected(0, empty)).toThrow(ConfigError)
  })
})

describe('toggleSelection does not throw', () => {
  test('toggleSelection does not throw on invalid question Index', () => {
    const plaintextBallot = new PlainTextBallot(map)
    expect(() => { plaintextBallot.toggleSelection(3, 0) }).not.toThrow()
  })

  test('toggleSelection does not throw on invalid option Index', () => {
    const plaintextBallot = new PlainTextBallot(map)
    expect(() => { plaintextBallot.toggleSelection(4, 0) }).not.toThrow()
  })

  test('toggleSelection does throw on missing option or question Index', () => {
    const plaintextBallot = new PlainTextBallot(map)
    let empty: number
    expect(() => { plaintextBallot.toggleSelection(empty, 0) }).toThrow(ConfigError)
    expect(() => { plaintextBallot.toggleSelection(0, empty) }).toThrow(ConfigError)
  })
})

describe('toggleSelection and isSelected work', () => {
  test('toggleSelection and isSelected work on valid input', () => {
    const plaintextBallot = new PlainTextBallot(map)
    expect(plaintextBallot.selectedOptions).toStrictEqual(modifiedMap)
    plaintextBallot.toggleSelection(2, 0)
    modifiedMap.get(2)?.set(0, 1)
    expect(plaintextBallot.selectedOptions).toStrictEqual(modifiedMap)
    expect(plaintextBallot.isSelected(2, 0)).toBe(true)

    plaintextBallot.toggleSelection(2, 0)
    modifiedMap.get(2)?.set(0, 0)
    expect(plaintextBallot.selectedOptions).toStrictEqual(modifiedMap)
    expect(plaintextBallot.isSelected(2, 0)).toBe(false)

    plaintextBallot.toggleSelection(4, -3)
    modifiedMap.get(4)?.set(-3, 1)
    expect(plaintextBallot.selectedOptions).toStrictEqual(modifiedMap)
    expect(plaintextBallot.isSelected(4, -3)).toBe(true)
  })

  test('toggleSelection on invalid question Index has no effect', () => {
    const plaintextBallot = new PlainTextBallot(map)
    plaintextBallot.toggleSelection(3, 0)
    expect(plaintextBallot.selectedOptions).toStrictEqual(modifiedMap)
    expect(plaintextBallot.isSelected(3, 0)).toBe(false)

    plaintextBallot.toggleSelection(3, 0)
    expect(plaintextBallot.selectedOptions).toStrictEqual(modifiedMap)
    expect(plaintextBallot.isSelected(3, 0)).toBe(false)
  })

  test('toggleSelection on invalid option Index has no effect', () => {
    const plaintextBallot = new PlainTextBallot(map)
    plaintextBallot.toggleSelection(2, 7)
    expect(plaintextBallot.selectedOptions).toStrictEqual(modifiedMap)
    expect(plaintextBallot.isSelected(2, 7)).toBe(false)

    plaintextBallot.toggleSelection(2, 7)
    expect(plaintextBallot.selectedOptions).toStrictEqual(modifiedMap)
    expect(plaintextBallot.isSelected(2, 7)).toBe(false)
  })
})

describe('getNumberOfSelectdOptions works', () => {
  test('getNumberOfSelectedOptions does not throw on invalid question index', () => {
    const plaintextBallot = new PlainTextBallot(map)
    expect(() => plaintextBallot.getNumberOfSelectedOptions(5)).not.toThrow()
  })

  test('getNumberOfSelectedOptions returns 0 on invalid question index', () => {
    const plaintextBallot = new PlainTextBallot(map)
    expect(plaintextBallot.getNumberOfSelectedOptions(5)).toBe(0)
  })

  test('getNumberOfSelectedOptions does not throw on missing question index', () => {
    const plaintextBallot = new PlainTextBallot(map)
    let empty: number
    expect(() => plaintextBallot.getNumberOfSelectedOptions(empty)).toThrow(ConfigError)
  })

  test('getNumberOfSelectedOptions gives correct output', () => {
    const plaintextBallot = new PlainTextBallot(map)
    expect(plaintextBallot.getNumberOfSelectedOptions(2)).toBe(0)
    plaintextBallot.toggleSelection(2, 0)
    expect(plaintextBallot.getNumberOfSelectedOptions(2)).toBe(1)
    expect(plaintextBallot.getNumberOfSelectedOptions(4)).toBe(0)
    plaintextBallot.toggleSelection(2, 0)
    expect(plaintextBallot.getNumberOfSelectedOptions(2)).toBe(0)
    expect(plaintextBallot.getNumberOfSelectedOptions(4)).toBe(0)
    plaintextBallot.toggleSelection(2, 0)
    plaintextBallot.toggleSelection(2, 1)
    expect(plaintextBallot.getNumberOfSelectedOptions(2)).toBe(2)
    expect(plaintextBallot.getNumberOfSelectedOptions(4)).toBe(0)
  })
})

describe('check works', () => {
  test('check works on incorrect first question', () => {
    const plaintextBallot = new PlainTextBallot(map)
    plaintextBallot.toggleSelection(2, 0)
    plaintextBallot.toggleSelection(2, 1)
    plaintextBallot.toggleSelection(2, 2)
    expect(plaintextBallot.check()).toBe(false)
  })

  test('check works on incorrect second question', () => {
    const plaintextBallot = new PlainTextBallot(map)
    plaintextBallot.toggleSelection(2, 2)
    plaintextBallot.toggleSelection(4, -3)
    plaintextBallot.toggleSelection(4, 4)
    expect(plaintextBallot.check()).toBe(false)
  })

  test('check works on correct question', () => {
    const plaintextBallot = new PlainTextBallot(map)
    plaintextBallot.toggleSelection(2, 0)
    expect(plaintextBallot.check()).toBe(true)
  })
})

export {}
