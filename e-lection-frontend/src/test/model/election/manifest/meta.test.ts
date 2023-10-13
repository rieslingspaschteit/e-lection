/* eslint-disable no-new */
import { ConfigError } from '@/app/model/error'
import { Ballot, Question } from '@/app/model/ballot'
import { ElectionManifest, ElectionMeta } from '@/app/model/election'
import { globalContext } from '@/app/utils/cryptoConstants'
import metaTestData from '../../../resources/meta-payload.json'

const title = 'some title'
const description = 'some description'
const authority = 'authority@mail.com'
const start = new Date(12300)
const threshold = 4
const end = new Date(123001)
const key = globalContext.createElementModPFromHex('1234')

describe('testing constructor', () => {
  test('valid instantiation without optionals', () => {
    expect(() => {
      new ElectionMeta(
        title, description, authority, end, threshold
      )
    }).not.toThrow(ConfigError)
  })

  test('valid instantiation with optionals', () => {
    expect(() => {
      new ElectionMeta(
        title, description, authority, end, threshold, start, key
      )
    }).not.toThrow(ConfigError)
  })

  test('blank values', () => {
    expect(() => {
      new ElectionMeta('', description, authority, end, threshold)
    }).toThrow(ConfigError)
    expect(() => {
      new ElectionMeta(title, description, '', start, threshold)
    }).toThrow(ConfigError)
  })

  test('invalid threshold', () => {
    expect(() => {
      new ElectionMeta(title, description, authority, end, 0)
    }).toThrow(ConfigError)
  })

  test('end before start', () => {
    expect(() => {
      new ElectionMeta(title, description, authority, end, threshold, new Date(end.getTime() + 1))
    }).toThrow(ConfigError)
  })

  test('invalid date', () => {
    expect(() => {
      new ElectionMeta(
        title, description, authority, new Date('abc'), threshold
      )
    }).toThrow(ConfigError)

    expect(() => {
      new ElectionMeta(
        title, description, authority, new Date('2023-01-19T1220:38.811Z'), threshold
      )
    }).toThrow(ConfigError)

    expect(() => {
      new ElectionMeta(
        title, description, authority, end, threshold, new Date('01-19T12:20:38.811Z')
      )
    }).toThrow(ConfigError)
  })
})

describe('test fromJSON', () => {
  test('valid fromJSON no optionals', () => {
    const validMetaObjNoOpts = metaTestData['valid-no-optionals']

    expect(() => ElectionMeta.fromJSON(validMetaObjNoOpts))
      .not.toThrow(ConfigError)

    expect(ElectionMeta.fromJSON(validMetaObjNoOpts))
      .toStrictEqual(new ElectionMeta(
        validMetaObjNoOpts.title,
        validMetaObjNoOpts.description,
        validMetaObjNoOpts.authority,
        new Date(validMetaObjNoOpts.end),
        validMetaObjNoOpts.threshold
      ))
  })

  test('valid fromJSON with optionals', () => {
    const validMetaObjOpts = metaTestData['valid-with-optionals']

    expect(() => ElectionMeta.fromJSON(validMetaObjOpts))
      .not.toThrow(ConfigError)

    expect(ElectionMeta.fromJSON(validMetaObjOpts))
      .toStrictEqual(new ElectionMeta(
        validMetaObjOpts.title,
        validMetaObjOpts.description,
        validMetaObjOpts.authority,
        new Date(validMetaObjOpts.end),
        validMetaObjOpts.threshold,
        new Date(validMetaObjOpts.start),
        globalContext.createElementModPFromHex(validMetaObjOpts.key)
      ))
  })

  test('undefined fields', () => {
    const falsyData = [
      metaTestData['undefined-title'],
      metaTestData['undefined-authority'],
      metaTestData['undefined-threshold'],
      metaTestData['undefined-end']
    ]

    falsyData.forEach(data => {
      expect(() => ElectionMeta.fromJSON(data))
        .toThrow(ConfigError)
    }
    )
  })
})

describe('test cloneWith', () => {
  let electionMeta: ElectionMeta
  beforeEach(() => {
    electionMeta = new ElectionMeta(
      title, description, authority,
      end, threshold, start, key
    )
  })

  test('cloneWithBallot', () => {
    const ballot = new Ballot(
      new Map([[1, new Question('q1', new Map([[1, 'o1']]), 1)]])
    )
    const manifest = electionMeta.cloneWithBallot(ballot)

    expect(manifest)
      .toStrictEqual(new ElectionManifest(electionMeta, undefined, undefined, undefined, ballot))
    expect((electionMeta as ElectionManifest).ballot)
      .toBeUndefined()
  })

  test('cloneWithTrustees', () => {
    const trustees = ['t1', 't2']
    const manifest = electionMeta.cloneWithTrustees(trustees, false)
    expect(manifest)
      .toStrictEqual(new ElectionManifest(electionMeta, undefined, trustees, false))
    expect((electionMeta as ElectionManifest).trustees)
      .toBeUndefined()
  })
})
