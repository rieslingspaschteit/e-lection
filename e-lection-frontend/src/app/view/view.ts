import { ref, type Ref } from 'vue'
import type { Election, MutableElectionManifest } from '../model/election'
import type { User } from '../model/user'

export class ElectionStore {
  private static _instance?: ElectionStore
  private readonly _electionCollection: Ref<Map<number, Election>>
  private readonly _voterElections: Ref<Map<number, Election>>
  private readonly _trusteeElections: Ref<Map<number, Election>>
  private readonly _authorityElections: Ref<Map<number, Election>>
  private readonly _user: Ref<User | undefined>
  private readonly _mutableElectionManifest: Ref<MutableElectionManifest | undefined>
  private readonly _authenticated: Ref<boolean>

  private constructor () {
    this._electionCollection = ref(new Map())
    this._voterElections = ref(new Map())
    this._trusteeElections = ref(new Map())
    this._authorityElections = ref(new Map())
    this._user = ref()
    this._mutableElectionManifest = ref()
    this._authenticated = ref(false)
  }

  public static get instance (): ElectionStore {
    if (this._instance == null) {
      this._instance = new ElectionStore()
    }
    return this._instance
  }

  public get authenticated (): boolean {
    return this._authenticated.value
  }

  public set authenticated (v: boolean) {
    this._authenticated.value = v
  }

  public set voterElections (voterElections: Election[]) {
    this._voterElections.value.forEach((_e, i) => this._electionCollection.value.delete(i))
    this._voterElections.value.clear()
    voterElections.forEach(election => {
      this._voterElections.value.set(election.electionId, election)
      this._electionCollection.value.set(election.electionId, election)
    })
  }

  public get voterElections (): Election[] {
    return [...this._voterElections.value.values()]
  }

  public set trusteeElections (trusteeElections: Election[]) {
    this._trusteeElections.value.forEach((_e, i) => this._electionCollection.value.delete(i))
    this._trusteeElections.value.clear()
    trusteeElections.forEach(election => {
      this._trusteeElections.value.set(election.electionId, election)
      this._electionCollection.value.set(election.electionId, election)
    })
  }

  public get trusteeElections (): Election[] {
    return [...this._trusteeElections.value.values()]
  }

  public set authorityElections (authorityElections: Election[]) {
    this._authorityElections.value.forEach((_e, i) => this._electionCollection.value.delete(i))
    this._authorityElections.value.clear()
    authorityElections.forEach(election => {
      this._authorityElections.value.set(election.electionId, election)
      this._electionCollection.value.set(election.electionId, election)
    })
  }

  public get authorityElections (): Election[] {
    return [...this._authorityElections.value.values()]
  }

  public setVoterElection (election: Election): void {
    this._voterElections.value.set(election.electionId, election)
    this._electionCollection.value.set(election.electionId, election)
  }

  public setAuthorityElection (election: Election): void {
    this._authorityElections.value.set(election.electionId, election)
    this._electionCollection.value.set(election.electionId, election)
  }

  public setTrusteeElection (election: Election): void {
    this._trusteeElections.value.set(election.electionId, election)
    this._electionCollection.value.set(election.electionId, election)
  }

  public set user (user: User | undefined) {
    this._user.value = user
  }

  public get user (): User | undefined {
    return this._user.value
  }

  public getElection (id: number): Election | undefined {
    return this._electionCollection.value.get(id)
  }

  public setElection (election: Election): void {
    this._electionCollection.value.set(election.electionId, election)
  }

  public get mutableElectionManifest (): MutableElectionManifest {
    // eslint-disable-next-line @typescript-eslint/no-non-null-assertion
    return this._mutableElectionManifest.value!
  }

  public set mutableElectionManifest (v: MutableElectionManifest) {
    this._mutableElectionManifest.value = v
    v.authority = this.user?.email
  }
}
