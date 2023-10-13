import { ConfigError } from '@/app/model/error'
import { Election, ElectionMeta } from '@/app/model/election'
import electionData from '../../resources/election-payload.json'

const validElection = electionData['valid-no-optionals']
const validMeta = validElection.electionMeta
let meta: ElectionMeta
let fromJSONMock: any

beforeAll(() => {
  meta = new ElectionMeta(
    validMeta.title,
    validMeta.description,
    validMeta.authority,
    new Date(validMeta.end),
    validMeta.threshold
  )

  fromJSONMock = jest.fn(() => meta)
  jest
    .spyOn(ElectionMeta, 'fromJSON')
    .mockImplementation(fromJSONMock)
})

describe('testing fromJSON', () => {
  test('load from valid json', () => {
    expect(() => Election.fromJSON(validElection))
      .not.toThrow(ConfigError)

    expect(fromJSONMock)
      .toBeCalledTimes(1)

    const election = Election.fromJSON(validElection)

    expect(election.electionId)
      .toBe(validElection.electionId)

    expect(election.electionMeta)
      .toBe(meta)

    expect(election.electionState)
      .toStrictEqual({ name: validElection.state, subStates: [] })

    expect(election.fingerprint)
      .toBeNull()
  })

  test('undefined mandatory attributes', () => {
    expect(() => Election.fromJSON(
      electionData['undefined-electionId']
    )).toThrow(ConfigError)

    expect(() => Election.fromJSON(
      electionData['undefined-meta']
    )).toThrow(ConfigError)

    expect(() => Election.fromJSON(
      electionData['undefined-state']
    )).toThrow(ConfigError)
  })

  test('invalid values', () => {
    expect(() => Election.fromJSON(
      electionData['invalid-electionId-NaN']
    )).toThrow(ConfigError)

    expect(() => Election.fromJSON(
      electionData['invalid-electionId-negative']
    )).toThrow(ConfigError)

    expect(() => Election.fromJSON(
      electionData['invalid-not-a-state']
    )).toThrow(ConfigError)
  })
})

test('clone', () => {
  const election = Election.fromJSON(validElection)

  const meta = election.electionMeta

  const clone = election.cloneWithMeta(meta)

  expect(election).not.toBe(clone)

  expect(clone.electionMeta).toBe(meta)

  expect({
    electionId: clone.electionId,
    electionMeta: clone.electionMeta,
    electionState: clone.electionState,
    fingerprint: clone.fingerprint
  }).toStrictEqual({
    electionId: election.electionId,
    electionMeta: meta,
    electionState: election.electionState,
    fingerprint: election.fingerprint
  })
})

afterAll(() => {
  jest.clearAllMocks()
})
