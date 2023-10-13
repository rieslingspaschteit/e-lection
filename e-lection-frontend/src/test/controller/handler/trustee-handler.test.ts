import { TrusteeHandler } from '@/app/controller/handler'
import { DecryptionElection, Election, KeyCeremonyElection } from '@/app/model/election'
import trusteeHandlerData from '../../resources/valid-trustee-handler-responses.json'
import electionJson from '../../resources/election-payload.json'
import axios from 'axios'
import { DecryptionState, KeyCeremonyState } from '@/app/model/election/states'

describe('test trustee-handler successfully request', () => {
  let handler: TrusteeHandler
  let getSpy: jest.SpyInstance<any>
  let postSpy: jest.SpyInstance<any>
  let patchSpy: jest.SpyInstance<any>

  const axiosInstance = axios.create()
  const election = Election.fromJSON(electionJson['valid-no-optionals'])

  beforeAll(() => {
    jest.spyOn(axios, 'create')
      .mockReturnValue(axiosInstance)
    getSpy = jest.spyOn(axiosInstance, 'get')
    postSpy = jest.spyOn(axiosInstance, 'post')
    patchSpy = jest.spyOn(axiosInstance, 'patch')
  })

  beforeEach(() => {
    handler = new TrusteeHandler(
      '', Election.fromJSON(electionJson['valid-no-optionals'])
    )
    getSpy.mockReset()
    postSpy.mockReset()
    patchSpy.mockReset()
  })

  describe('key ceremony', () => {
    const keyCeremonyAuxResponse = trusteeHandlerData['key-ceremony-state-aux']
    const keyCeremonyEPKBResponse = trusteeHandlerData['key-ceremony-state-epkb']

    beforeEach(() => {
      getSpy.mockResolvedValue(keyCeremonyAuxResponse)
    })

    test('get state', async () => {
      await expect(
        handler.fetchKeyCeremony()
          .then(async () => await handler.getKeyCeremonyElection())
      ).resolves.toStrictEqual(
        new KeyCeremonyElection(
          election,
          KeyCeremonyState.AUX_KEYS,
          keyCeremonyAuxResponse.data.waiting
        )
      )
    })

    test('get aux keys', async () => {
      const auxKeysResponse = trusteeHandlerData['aux-keys-response']
      getSpy.mockResolvedValueOnce(auxKeysResponse)

      await expect(
        handler.fetchAuxKeys()
          .then(async () => await handler.getKeyCeremonyElection())
      ).resolves.toStrictEqual(
        new KeyCeremonyElection(
          election,
          KeyCeremonyState.AUX_KEYS,
          keyCeremonyAuxResponse.data.waiting,
          auxKeysResponse.data
        )
      )
    })

    test('get epkb', async () => {
      const epkbResponse = trusteeHandlerData['keys-and-backups-response']
      getSpy.mockReset()
      getSpy.mockResolvedValueOnce(epkbResponse)
      getSpy.mockResolvedValueOnce(keyCeremonyEPKBResponse)

      await expect(
        handler.fetchEBKB()
          .then(async () => await handler.getKeyCeremonyElection())
      ).resolves.toStrictEqual(
        new KeyCeremonyElection(
          election,
          KeyCeremonyState.EPKB,
          keyCeremonyEPKBResponse.data.waiting,
          undefined,
          epkbResponse.data
        )
      )
    })

    describe('post', () => {
      beforeEach(() => {
        postSpy.mockResolvedValue({ data: {}, status: 200 })
      })

      test('aux-keys', async () => {
        const auxKey = trusteeHandlerData['aux-key-post']
        await expect(
          handler.getKeyCeremonyElection()
            .then(election => {
              election.committedAuxKeys = auxKey.data
            })
            .then(async () => { await handler.postKeyCeremony() })
        ).resolves.toBeUndefined()

        expect(postSpy).toBeCalledWith('/trustee/elections/1/auxkeys', auxKey.data)
      })

      test('epkb', async () => {
        const epkb = trusteeHandlerData['keys-and-backups-post']
        getSpy.mockResolvedValueOnce(keyCeremonyEPKBResponse)

        await expect(
          handler.getKeyCeremonyElection()
            .then(election => {
              election.committedEPKB = epkb.data
            })
            .then(async () => { await handler.postKeyCeremony() })
        ).resolves.toBeUndefined()

        expect(postSpy).toBeCalledWith('/trustee/elections/1/keys-and-backups', epkb.data)
      })
    })
  })

  describe('decryption', () => {
    const decryptionState = trusteeHandlerData['decryption-state']
    beforeEach(() => {
      getSpy.mockResolvedValue(decryptionState)
    })

    test('get state', async () => {
      await expect(
        handler.fetchDecryptionState()
          .then(async () => await handler.getDecryptionElection())
      ).resolves.toStrictEqual(
        new DecryptionElection(
          election,
          DecryptionState.P_DECRYPTION,
          decryptionState.data.waiting
        )
      )
    })

    test('get encryptions', async () => {
      const encryptions = trusteeHandlerData['encryptions-response']
      getSpy.mockResolvedValueOnce(encryptions)

      await expect(
        handler.fetchEncryptions()
          .then(async () => await handler.getDecryptionElection())
      ).resolves.toStrictEqual(
        new DecryptionElection(
          election,
          DecryptionState.P_DECRYPTION,
          decryptionState.data.waiting,
          encryptions.data
        )
      )
    })

    test('post decryptions', async () => {
      const decryptions = trusteeHandlerData['decryptions-post']
      postSpy.mockResolvedValue({ data: {}, status: 200 })
      await expect(
        handler.getDecryptionElection()
          .then(election => {
            election.committedDecryptions = decryptions.data
          })
          .then(async () => { await handler.postDecryptions() })
      ).resolves.toBeUndefined()

      expect(postSpy).toBeCalledWith('/trustee/elections/1/result', decryptions.data)
    })
  })
})
