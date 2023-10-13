/* eslint-disable no-new */
import { ConfigError } from '@/app/model/error'
import { Ballot } from '@/app/model/ballot'
import { Question } from '@/app/model/ballot/question'
import { ElectionMeta, ElectionManifest } from '@/app/model/election'
import { globalContext } from '@/app/utils/cryptoConstants'
import manifestData from '../../../resources/manifest-payload.json'
import { ManifestHashes } from '@/app/model/election/manifest/manifestHashes'

const title = 'some title'
const description = 'some description'
const authority = 'authority@mail.com'
const end = new Date(15000)
const threshold = 4
const start = new Date(10000)
const key = globalContext.createElementModPFromHex('0AB123')

const voters = ['v1', 'v2', 'v3']
const trustees = ['t1', 't2', 't3']

let meta: ElectionMeta
let ballot: Ballot

beforeAll(() => {
  meta = new ElectionMeta(title, description, authority, end, threshold, start, key)
  ballot = new Ballot(
    new Map([
      [1, new Question('q1', new Map([[1, 'o1'], [2, 'o2']]), 1)]
    ])
  )
})

describe('testing constructor', () => {
  test('valid instantiation', () => {
    expect(() => {
      new ElectionManifest(meta)
    }).not.toThrow(ConfigError)
    expect(() => {
      new ElectionManifest(meta, voters)
    }).not.toThrow(ConfigError)
    expect(() => {
      new ElectionManifest(meta, voters, trustees)
    }).not.toThrow(ConfigError)
    expect(() => {
      new ElectionManifest(meta, undefined, trustees)
    }).not.toThrow(ConfigError)
    expect(() => {
      new ElectionManifest(meta, voters, trustees, false, ballot)
    }).not.toThrow(ConfigError)
  })

  test('blank voters and trustees', () => {
    expect(() => {
      new ElectionManifest(meta, ['v1', '', 'v3'])
    }).toThrow(ConfigError)
    expect(() => {
      new ElectionManifest(meta, undefined, ['t1', '', 't3'])
    }).toThrow(ConfigError)
  })
})

const fullManifest = manifestData['full-manifest']
const questions = new Map<number, Question>()
Object.entries(fullManifest.questions)
  .forEach(([key, val]) => questions.set(
    Number(key),
    new Question(
      val.questionText,
      new Map(val.options.map(option => [val.options.indexOf(option), option])),
      val.max
    )
  ))
const fromJSONMock = jest.fn(() => {
  return new Ballot(questions)
})

describe('test fromJSON', () => {
  beforeAll(() => {
    jest.spyOn(Ballot, 'fromJSON').mockImplementation(fromJSONMock)
  })

  test('load manifest from JSON', () => {
    expect(() => ElectionManifest.fromJSON(fullManifest))
      .not.toThrow(ConfigError)

    expect(fromJSONMock).toBeCalledTimes(1)

    const manifestFromJSON = ElectionManifest.fromJSON(fullManifest)

    const expectedManifest = new ElectionManifest(
      new ElectionMeta(
        fullManifest.electionMeta.title,
        fullManifest.electionMeta.description,
        fullManifest.electionMeta.authority,
        new Date(fullManifest.electionMeta.end),
        fullManifest.electionMeta.threshold,
        new Date(fullManifest.electionMeta.start),
        globalContext.createElementModPFromHex(fullManifest.electionMeta.key)
      ),
      undefined,
      fullManifest.trustees,
      fullManifest.isBotEnabled,
      fromJSONMock(),
      ManifestHashes.fromJSON(fullManifest.hashes)
    )

    expect(manifestFromJSON).toStrictEqual(expectedManifest)
  })

  /*
    test("load invalid manifest from JSON", () => {
        const trusteesNotArray = manifestData["manifest-with-trustees-not-an-array"]

        expect(() => ElectionManifest.fromJSON(trusteesNotArray))
        .toThrow(ConfigError)
    })

    test("InvalitBot manifest from json",() => {
        const notABoolean = manifestData["manifest-with-isBotEnabled-not-a-boolean"]
        expect(() => ElectionManifest.fromJSON(notABoolean))
        .toThrow(ConfigError)
    })
    */
  afterAll(() => {
    jest.clearAllMocks()
  })
})

test('test serialization', () => {
  jest.spyOn(Ballot.prototype, 'stringify').mockImplementation(() => {
    return manifestData['full-manifest'].questions
  })
  jest.spyOn(Ballot, 'fromJSON').mockImplementation(fromJSONMock)

  const manifest = ElectionManifest.fromJSON(manifestData['full-manifest'])

  const serialized = manifest.stringify()

  expect(serialized).toStrictEqual(manifestData['full-manifest'])
})
