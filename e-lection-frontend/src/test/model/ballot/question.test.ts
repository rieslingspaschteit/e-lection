/* eslint-disable no-new */
import { ConfigError } from '@/app/model/error'
import { Question } from '@/app/model/ballot/question'

describe('testing Question', () => {
  test('constructor checks for empty options', () => {
    const options = new Map<number, string>()
    expect(() => {
      new Question('Why', options as ReadonlyMap<number, string>, 1)
    }).toThrow(ConfigError)
  })

  test('constructor checks for empty name', () => {
    const options = new Map<number, string>()
    options.set(1, 'Yes')
    expect(() => {
      new Question('', options as ReadonlyMap<number, string>, 1)
    }).toThrow(ConfigError)
  })

  test('constructor checks for empty option name', () => {
    const options = new Map<number, string>()
    options.set(1, 'Yes')
    options.set(1, '')
    expect(() => {
      new Question('Why', options as ReadonlyMap<number, string>, 1)
    }).toThrow(ConfigError)
  })

  test('constructor checks for min higher than max', () => {
    const options = new Map<number, string>()
    options.set(1, 'Yes')
    expect(() => {
      new Question('Why', options as ReadonlyMap<number, string>, -1)
    }).toThrow(ConfigError)
  })

  test('constructor works for correct input', () => {
    const options = new Map<number, string>()
    options.set(1, 'Yes')
    expect(() => {
      new Question('Why', options as ReadonlyMap<number, string>, 2)
    }).not.toThrow(ConfigError)
  })

  test('serialization works', () => {
    const options = new Map<number, string>()
    options.set(1, 'Yes')
    const question = new Question('Why', options as ReadonlyMap<number, string>, 2)
    const json = question.stringify()
    expect(json).toStrictEqual({
      questionText: 'Why',
      options: ['Yes'],
      max: 2
    })
  })

  test('deserialization works', () => {
    const options = new Map<number, string>()
    options.set(0, 'Yes')
    const json = {
      questionText: 'Why',
      options: ['Yes'],
      max: 2
    }
    const question2 = Question.fromJSON(json)
    expect(question2.questionText).toBe('Why')
    expect(question2.maxSelections).toBe(2)
    expect(question2.options.size).toBe(options.size)
    expect(question2.options.get(0)).toBe(options.get(0))
  })

  test('deserialization should be rejected on missing question text input', () => {
    const json = '{"options":["Yes"],"min":0,"max":2}'
    expect(() => {
      Question.fromJSON(json)
    }).toThrow(ConfigError)
  })

  test('deserialization should be rejected on missing options input', () => {
    const json = '{"questionText":"Why","min":0,"max":2}'
    expect(() => {
      Question.fromJSON(json)
    }).toThrow(ConfigError)
  })

  test('deserialization should be rejected on missing max input', () => {
    const json = '{"options":["Yes"],"min":0}'
    expect(() => {
      Question.fromJSON(json)
    }).toThrow(ConfigError)
  })
})

export {}
