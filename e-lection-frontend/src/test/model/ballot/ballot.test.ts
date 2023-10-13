/* eslint-disable no-new */
import { MutableQuestion, Ballot, Question, MutableBallot } from '@/app/model/ballot'
import { ConfigError } from '@/app/model/error'

describe('fromJSON testing', () => {
  test('stringify works', () => {
    const json = {
      0: {
        questionText: 'Who',
        options: ['1', '2', '3', '4'],
        max: 3
      },
      1: {
        questionText: 'What',
        options: ['a', 'b', 'c'],
        max: 1
      }
    }
    const ballot = new MutableBallot()
    const question1 = new MutableQuestion()
    const question2 = new MutableQuestion()
    question1.questionText = 'Who'
    question1.addOption(4, '1')
    question1.addOption(5, '2')
    question1.addOption(6, '3')
    question1.addOption(7, '4')
    question1.maxSelections = 3

    question2.questionText = 'What'
    question2.addOption(-1, 'a')
    question2.addOption(0, 'b')
    question2.addOption(1, 'c')
    question2.maxSelections = 1

    ballot.addQuestion(4, question2)
    ballot.addQuestion(2, question1)
    // let b2 = Ballot.fromJSON(ballot.create().stringify())
    expect(ballot.create().stringify()).toStrictEqual(json)
  })

  test('fromJSON works correctly on multiple questions', () => {
    const json = {
      0: {
        questionText: 'Why',
        options: ['Yes'],
        min: 0,
        max: 2
      },
      1: {
        questionText: 'What',
        options: ['No', 'Stop'],
        min: 2,
        max: 2
      }
    }

    const ballot = Ballot.fromJSON(json)
    const question00 = ballot.questions.get(0)
    const question01 = Question.fromJSON(json[0])
    const question10 = ballot.questions.get(0)
    const question11 = Question.fromJSON(json[0])
    expect(question00).toStrictEqual(question01)
    expect(question10).toStrictEqual(question11)
  })

  test('fromJSON throws on undefined json', () => {
    let json: any
    expect(() => { Ballot.fromJSON(json) }).toThrow(ConfigError)
  })

  test('fromJSON throws on missing option', () => {
    const json = {
      question: {
        0: {
          questionText: 'Why',
          options: ['Yes'],
          min: 0,
          max: 2
        },
        1: {
          questionText: 'What',
          options: ['No', 'Stop'],
          min: 2,
          max: 2
        }
      }
    }
    expect(() => { Ballot.fromJSON(json) }).toThrow(ConfigError)
  })
})

describe('testing constructor', () => {
  test('constructor throws on undefined questions', () => {
    let map: Map<number, Question>
    expect(() => {
      new Ballot(map)
    }).toThrow(ConfigError)
  })

  test('constructor throws on empty questions', () => {
    const map: Map<number, Question> = new Map<number, Question>()
    expect(() => {
      new Ballot(map)
    }).toThrow(ConfigError)
  })

  test('constructor works with correct Question', () => {
    const map: Map<number, Question> = new Map<number, Question>()
    const question = {
      questionText: 'Why',
      options: ['Yes'],
      min: 0,
      max: 2
    }
    map.set(-1, Question.fromJSON(question))
    const ballot = new Ballot(map)
    const copiedQuestion = ballot.questions.get(-1)
    const secondCopiedQuestion = Question.fromJSON(question)
    expect(copiedQuestion).toStrictEqual(secondCopiedQuestion)
  })
})
/*
test("deserialization of maps", () => {
    let map = new Map<string, string>();
    map.set("one", "value1");
    map.set("two", "value2");
    map.set("three", "value3");
    let jsonObject: any = {};
    map.forEach((value, key) => {
      jsonObject[key] = value;
    });
    //expect(x).toStrictEqual({"2": "3"})
    expect(jsonObject).toBe(JSON.stringify({"2": "3"}))
})
*/

export {}
