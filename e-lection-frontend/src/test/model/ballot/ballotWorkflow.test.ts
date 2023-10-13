import { Ballot, MutableBallot, MutableQuestion, Question } from '@/app/model/ballot'

test('Testing Ballot transformation', () => {
  const mballot = new MutableBallot()
  const mquestion1 = new MutableQuestion()
  const mquestion2 = new MutableQuestion()
  mquestion1.questionText = 'who'
  mquestion1.maxSelections = 1
  mquestion1.addOption(1, 'a')
  mquestion1.addOption(3, 'b')

  mquestion2.questionText = 'what'
  mquestion2.maxSelections = 1
  mquestion2.addOption(0, '1')

  mballot.addQuestion(-1, mquestion1)
  mballot.addQuestion(0, mquestion2)

  const optionsMap1 = new Map<number, string>()
  optionsMap1.set(0, 'a')
  optionsMap1.set(1, 'b')

  const optionsMap2 = new Map<number, string>()
  optionsMap2.set(0, '1')
  const question1 = new Question('who', optionsMap1, 1)
  const question2 = new Question('what', optionsMap2, 1)
  const questionMap = new Map<number, Question>()
  questionMap.set(0, question1)
  questionMap.set(1, question2)
  const ballot = new Ballot(questionMap)
  expect(mquestion1.create()).toStrictEqual(question1)
  expect(mquestion2.create()).toStrictEqual(question2)
  expect(mballot.create()).toStrictEqual(ballot)

  expect(Ballot.fromJSON(ballot.stringify())).toStrictEqual(ballot) // This is not always the case, only if map indizes of options are from 0,...,k
  // However, this schould be the case due to compacting at create
  const plaintext = ballot.getPlainTextBallot()
  plaintext.toggleSelection(0, 1)
  plaintext.toggleSelection(0, 0)
  expect(plaintext.check()).toBe(false)
  plaintext.toggleSelection(0, 0)
  expect(plaintext.check()).toBe(true)
})

export {}
