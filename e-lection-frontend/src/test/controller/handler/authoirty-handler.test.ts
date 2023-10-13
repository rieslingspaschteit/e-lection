import { AuthorityHandler } from '@/app/controller/handler'
import axios from 'axios'
import authorityHandlerData from '../../resources/valid-authority-handler-data.json'
import manifestJson from '../../resources/manifest-payload.json'
import electionJson from '../../resources/election-payload.json'
import { AuthorityElection, Election, ElectionManifest } from '@/app/model/election'
import { DecryptionState, KeyCeremonyState } from '@/app/model/election/states'

const axiosInstance = axios.create()
const election = Election.fromJSON(electionJson['valid-no-optionals'])

let getSpy: jest.SpyInstance
let postSpy: jest.SpyInstance
let patchSpy: jest.SpyInstance
let handler: AuthorityHandler

beforeAll(() => {
  jest.spyOn(axios, 'create')
    .mockReturnValue(axiosInstance)
  getSpy = jest.spyOn(axiosInstance, 'get')
  postSpy = jest.spyOn(axiosInstance, 'post')
  patchSpy = jest.spyOn(axiosInstance, 'patch')
})

beforeEach(() => {
  getSpy.mockReset()
  postSpy.mockReset()
  patchSpy.mockReset()
  handler = new AuthorityHandler('', election)
})

describe('test AuthorityHandler successful request', () => {
  const createResponse = authorityHandlerData['create-response']
  test('post election', async () => {
    const manifest = ElectionManifest
      .fromJSON(manifestJson['config-manifest'])

    postSpy.mockResolvedValue(createResponse)

    await expect(
      handler.postElection(manifest)
        .then(() => handler.getAuthorityElection())
    ).resolves.toStrictEqual(
      new AuthorityElection(
        Election.fromJSON({
          electionId: createResponse.data.electionId,
          electionMeta: manifestJson['config-manifest'].electionMeta,
          fingerprint: null,
          state: 'CREATED'
        })
      )
    )

    expect(postSpy).toBeCalledWith('/authority/elections/create', manifest.stringify())
  })

  test('get key ceremony details', async () => {
    const keyCerDetails = authorityHandlerData['key-cer-response']

    getSpy.mockResolvedValue(keyCerDetails)

    await expect(
      handler.fetchKeyCeremonyAttendance()
        .then(() => handler.getAuthorityElection())
    ).resolves.toStrictEqual(
      new AuthorityElection(
        election,
        KeyCeremonyState.AUX_KEYS,
        keyCerDetails.data.keyCerCount
      )
    )
  })

  test('get decryption details', async () => {
    const decryptionDetails = authorityHandlerData['decryption-response']
    getSpy.mockResolvedValue(decryptionDetails)

    await expect(
      handler.fetchDecryptionAttendance()
        .then(() => handler.getAuthorityElection())
    ).resolves.toStrictEqual(
      new AuthorityElection(
        election,
        undefined,
        undefined,
        DecryptionState.P_DECRYPTION,
        decryptionDetails.data.decCount
      )
    )
  })

  test('open election', async () => {
    patchSpy.mockResolvedValue({ data: {}, status: 200 })

    await expect(handler.openElection())
      .resolves.toBeUndefined()

    expect(patchSpy)
      .toBeCalledWith('/authority/elections/1', { state: 'OPEN' })
  })

  test('pp-decryption', async () => {
    patchSpy.mockResolvedValue({ data: {}, status: 200 })

    await expect(handler.updateDecryption())
      .resolves.toBeUndefined()

    expect(patchSpy)
      .toBeCalledWith('/authority/elections/1', { state: 'PP_DECRYPTION' })
  })
})
