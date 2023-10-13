import { MutableBallot, MutableQuestion, Question } from '@/app/model/ballot'
import { describe, expect, test } from '@jest/globals'
import { ConfigError } from '@/app/model/error'

describe('test manipulation methods working', () => {
  test('add question and get question work correctly', () => {
    const mutable = new MutableBallot()
    const q1 = new MutableQuestion()
    const q2 = new MutableQuestion()
    q2.addOption(1, 'a')
    q1.addOption(-1, 'b')
    mutable.addQuestion(1, q1)
    mutable.addQuestion(-1, q2)
    const out1 = mutable.getQuestion(1)
    const out2 = mutable.getQuestion(-1)
    expect(out1).toStrictEqual(q1)
    expect(out2).toStrictEqual(q2)
  })

  test('remove option doesn\'t throw on missing index', () => {
    const mutable = new MutableBallot()
    expect(() => {
      mutable.removeQuestion(1)
    }).not.toThrow()
  })

  test('get question doesn\'t throw on missing index', () => {
    const mutable = new MutableBallot()
    expect(() => {
      mutable.getQuestion(1)
    }).not.toThrow()
  })

  test('remove option works properly', () => {
    const mutable = new MutableBallot()
    mutable.addQuestion(1, new MutableQuestion())
    mutable.removeQuestion(1)
    const out1 = typeof mutable.getQuestion(1)
    expect(out1).toBe('undefined')
  })

  test('add option throws on undefined question', () => {
    const mutable = new MutableBallot()
    let option: MutableQuestion
    expect(() => {
      mutable.addQuestion(1, option)
    }).toThrow(ConfigError)
  })
})

describe('manipulation methods throwing on undefined index', () => {
  test('add question throws on undefined index', () => {
    const mutable = new MutableBallot()
    let index: number
    expect(() => {
      mutable.addQuestion(index, new MutableQuestion())
    }).toThrow(ConfigError)
  })

  test('get question throws on undefined index', () => {
    const mutable = new MutableBallot()
    let index: number
    expect(() => {
      mutable.getQuestion(index)
    }).toThrow(ConfigError)
  })

  test('remove option throws on undefined index', () => {
    const mutable = new MutableBallot()
    let index: number
    expect(() => {
      mutable.removeQuestion(index)
    }).toThrow(ConfigError)
  })
})

describe('ballot creation works', () => {
  test('compacting map works', () => {
    const q1 = Question.fromJSON({
      questionText: 'Who',
      options: ['I', 'You'],
      min: 2,
      max: 3
    })
    const q2 = Question.fromJSON({
      questionText: 'Why',
      options: ['Because', '42'],
      min: 1,
      max: 3
    })
    const q3 = Question.fromJSON({
      questionText: 'What',
      options: ['This', 'That'],
      min: 0,
      max: 20
    })

    const x1 = new MutableQuestion()
    x1.questionText = 'Who'
    x1.addOption(1, 'I')
    x1.addOption(4, 'You')
    x1.maxSelections = 3

    const x2 = new MutableQuestion()
    x2.questionText = 'Why'
    x2.addOption(1, 'Because')
    x2.addOption(4, '42')
    x2.maxSelections = 3

    const x3 = new MutableQuestion()
    x3.questionText = 'What'
    x3.addOption(1, 'This')
    x3.addOption(4, 'That')
    x3.maxSelections = 20
    const mutable = new MutableBallot()
    mutable.addQuestion(3, x1)
    mutable.addQuestion(-2, x2)
    mutable.addQuestion(0, x3)
    expect(() => {
      mutable.create()
    }).not.toThrow()
    const ballot = mutable.create()
    const option1 = ballot.questions.get(0)
    const option2 = ballot.questions.get(1)
    const option3 = ballot.questions.get(2)
    expect(option1).toStrictEqual(q2)
    expect(option2).toStrictEqual(q3)
    expect(option3).toStrictEqual(q1)
  })
})

export {}
