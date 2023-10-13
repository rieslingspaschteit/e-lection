import { MutableQuestion } from '@/app/model/ballot'
import { describe, expect, test } from '@jest/globals'

describe('test manipulation methods working', () => {
  test('add option and get option work correctly', () => {
    const mutable = new MutableQuestion()
    mutable.addOption(1, 'option1')
    mutable.addOption(-1, 'option2')
    const out1 = mutable.getOption(1)
    const out2 = mutable.getOption(-1)
    expect(out1).toBe('option1')
    expect(out2).toBe('option2')
  })

  test('remove option doesn\'t throw on missing index', () => {
    const mutable = new MutableQuestion()
    expect(() => {
      mutable.removeOption(1)
    }).not.toThrow()
  })

  test('get option doesn\'t throw on missing index', () => {
    const mutable = new MutableQuestion()
    expect(() => {
      mutable.getOption(1)
    }).not.toThrow()
  })

  test('remove option works properly', () => {
    const mutable = new MutableQuestion()
    mutable.addOption(1, 'option1')
    mutable.removeOption(1)
    const out1 = typeof mutable.getOption(1)
    expect(out1).toBe('undefined')
  })

  test.skip('add option works on undefined option', () => {
    const mutable = new MutableQuestion()
    const option = mutable.getOption(1)
    expect(() => {
      mutable.addOption(1, option)
    }).not.toThrow()
    expect(option).toBe('')
  })

  test('add option overwrites existing option', () => {
    const mutable = new MutableQuestion()
    mutable.addOption(1, 'a')
    mutable.addOption(1, 'b')
    const option = mutable.getOption(1)
    expect(option).toBe('b')
  })
})

describe('manipulation methods throwing on undefined index', () => {
  test('add option throws on undefined index', () => {
    const mutable = new MutableQuestion()
    let index: number
    expect(() => {
      mutable.addOption(index, 'test')
    }).toThrow()
  })

  test('get option throws on undefined index', () => {
    const mutable = new MutableQuestion()
    let index: number
    expect(() => {
      mutable.getOption(index)
    }).toThrow()
  })

  test('remove option throws on undefined index', () => {
    const mutable = new MutableQuestion()
    let index: number
    expect(() => {
      mutable.removeOption(index)
    }).toThrow()
  })
})

describe('getters don\'t throw on undefined attributes', () => {
  const mutable = new MutableQuestion()
  test('get question text works', () => {
    expect(() => {
      // eslint-disable-next-line @typescript-eslint/no-unused-expressions
      mutable.questionText
    }).not.toThrow()
  })

  test('get minSelection text works', () => {
    expect(() => {
      // eslint-disable-next-line @typescript-eslint/no-unused-expressions
      mutable.maxSelections
    }).not.toThrow()
  })
})

describe('ballot creation works', () => {
  test('compacting map works', () => {
    const mutable = new MutableQuestion()
    mutable.questionText = 'Who'
    mutable.maxSelections = 2
    mutable.addOption(3, 'a')
    mutable.addOption(-2, 'b')
    mutable.addOption(0, 'c')
    expect(() => {
      mutable.create()
    }).not.toThrow()
    const question = mutable.create()
    const option1 = question.options.get(0)
    const option2 = question.options.get(1)
    const option3 = question.options.get(2)
    expect(option1).toBe('b')
    expect(option2).toBe('c')
    expect(option3).toBe('a')
  })
})
