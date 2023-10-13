import { ConfigError } from '@/app/model/error'
import { Ballot, MutableBallot } from '@/app/model/ballot'
import { ElectionManifest, ElectionMeta, MutableElectionManifest } from '@/app/model/election'
import manifestData from '../../../resources/manifest-payload.json'

describe.skip('testing mutable manifest', () => { // FIXME mock doesn't work.. maybe wait fpr mutableBallot to be implemented
  let mutableManifest: MutableElectionManifest
  const validManifest = manifestData['full-manifest-voters']
  beforeAll(() => {
    jest.spyOn(MutableBallot.prototype, 'create').mockImplementation(() => {
      return new Ballot(new Map())
    })
  })

  beforeEach(() => {
    mutableManifest = new MutableElectionManifest()
    mutableManifest.title = validManifest.electionMeta.title
    mutableManifest.description = validManifest.electionMeta.description
    mutableManifest.authority = validManifest.electionMeta.authority
    mutableManifest.end = validManifest.electionMeta.end
    mutableManifest.threshold = validManifest.electionMeta.threshold

    mutableManifest.voters = validManifest.voters
    mutableManifest.trustees = validManifest.trustees
    mutableManifest.isBotEnabled = validManifest.isBotEnabled
  })

  test('valid create', () => {
    expect(() => mutableManifest.create())
      .not.toThrow(ConfigError)

    expect(mutableManifest.create())
      .toStrictEqual(
        new ElectionManifest(
          new ElectionMeta(
            validManifest.electionMeta.title,
            validManifest.electionMeta.description,
            validManifest.electionMeta.authority,
            new Date(validManifest.electionMeta.end),
            validManifest.electionMeta.threshold
          ),
          validManifest.voters,
          validManifest.trustees,
          validManifest.isBotEnabled
        )
      )
  })

  afterAll(() => {
    jest.clearAllMocks()
  })
})
