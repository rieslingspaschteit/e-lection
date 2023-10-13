# <u>e-lection Web-API Specification</u>

## Defined Codes
- **`OK`** - Alles ok und Response erstellt
- **`NO_CONTENT`** - Alles ok aber keine Response gebraucht
- **`CREATED`** - Election created
- **`FORBIDDEN`** - Nutzer unauthorisiert
- **`BAD_REQUEST`** - Ein argument war illegal oder hat gefehlt
- **`NOT_FOUND`** - Provider wurde nicht gefunden

## UserController
#### Base URL `/api/user`

**Mapping: `/`**
**Method: `GET`**
**Response-Body:**
```
{
  userRoles: String[],
  email: String
}
```

## ElectionController
#### Base URL: `/api/elections`

**Mapping: `/{fingerprint}`**  
**Method: `GET`**  
**Response-Body:**  
```
{
  electionId: Long,
  electionMeta: {
    title: String,
    description: String,
    start: LocalDateTime / String,
    end: LocalDateTime / String,
    authority: String,
    threshold: int,
    key: BigInt
  },
  state: String (created || key_ceremony || open || decryption || finished),
  fingerprint: String
}
```

**Mapping: `/{electionId}`**  
**Method: `GET`**  
**Response-Body:**  
```
{
  electionId: Long,
  electionMeta: {
    title: String,
    description: String,
    start: LocalDateTime / String,
    end: LocalDateTime / String,
    authority: String,
    threshold: int,
    key: BigInt
  },
  state: String (created || key_ceremony || open || decryption || finished),
  fingerprint: String
}
```

**Mapping: `/{electionId}/vote`**  
**Method: `GET`**  
**Response-Body:**  
```
{
  eligibleToVote: boolean
}
```

**Mapping: `/{userRole}`**  
**Method: `GET`**  
**Response-Body:**  
```
[
  {
    electionId: Long,
    electionMeta: {
      title: String,
      description: String,
      start: LocalDateTime / String,
      end: LocalDateTime / String,
      authority: String,
      threshold: int,
      key: BigInt,
    },
    state: String (created || key_ceremony || open || decryption || finished),
    fingerprint: String
  }
]

```

**Mapping:`/{electionId}/manifest/ballot`**  
**Method: `GET`**  
**Response-Body:**  
```
{
  questions: Map<int, 
    {
      questionText: String,
      options: String[],
      max: int
    }>
}
```

**Mapping:`/{electionId}/manifest/trustees`**  
**Method: `GET`**  
**Response-Body:**  
```
{
  trustees: [
    {
      email: String
    }
  ],
  isBotEnabled: boolean
}
```

**Mapping:`/{electionId}/manifest/hashes`**  
**Method: `GET`**  
**Response-Body:**  
```
{
  manifestHash: bigint
  contestHashes: Map<int, bigint>
  selectionHashes: Map<int, bigint[]>
  contestIds: Map<int, bigint>
  selectionIds: Map<int, bigint[]>
  commitments: bigint
  voterHash: bigint
}
```

**Mapping: `/{electionId}/ballot-board`**  
**Method: `GET`**  
**Response-Body:**  
```
{
  tracking-codes: String[]
}
```

**Mapping: `/{electionId}/election-record/`**  
**Method: `GET`**  
**Response-Body:**  
```
record: ZipStream
```

**Mapping: `/{electionId}/result`**
**Method: `GET`**  
**Response-Body:**  
```
{
  result: Map<int, int[]>
}
```

## AuthorityController
#### **Base URL: `/api/authority/elections`**

**Mapping: `/create`**  
**Method: `POST`**  
**Request-Body:**  
```
{
  electionMeta: {
    title: String,
    description: String,
    start: LocalDateTime / String,
    end: LocalDateTime / String,
    threshold: int,
  },
  ballot: {
    questions: Map<int, 
      {
        questionText: String,
        options: String[],
        maxSelections: int
      }>
  },
  trustees: [
    {
      email: String
    }
  ],
  bot: boolean,
  voters: [
    {
      email: String
    }
  ]
}
```
**Response-Body:**
```
{
  electionId: Long,
  state: String
}
```

**Mapping**

**Mapping: `/{electionId}`**  
**Method: `PATCH`**  
**Request-Body:**  
```
{
  state: String
}
```

**Mapping: `/{electionId}`**  
**Method: `GET`**  
**Response-Body:**  
```
{
  keyCerCount: int
  decryptionCount: int,
  keyCerState: String
  decryptionState: String
}
```

## TrusteeController
#### **Base URL: `/api/trustee/elections/{electionId}`**

**Mapping: `/key-ceremony`**
**Method: `GET`**  
**Response-Body:**
```
{
  state: AUX_KEYS | EPKB | FINISHED,
  waiting: boolean
}
```

**Mapping: `/aux-keys`**
**Method: `GET`**  
**Response-Body:**
```
{
  //Key: order of trustee the key is from
  auxKeys: Map<int, 
    {
      publicKey: String,
      keyType: String,
    }>
}
```

**Mapping: `/auxkeys`**  
**Method: `POST`**  
**Request-Body:** 
```
{
  publicKey: String,
  keyType: String
}
```

**Mapping: `/keys-and-backups`**  
**METHOD: `GET`**  
**Response-Body**  
```
{
  //Key: Order of trustee the backup is from
  backups: Map<int, string>
  publicKeys: Map<int, bigint[]>
}
```

**Mapping: `/keys-and-backups`**  
**METHOD: `POST`**  
**Request-Body**  
```
{
  proofs: [
    { 
      publicKey: bigint,
      commitment: bigint,  
      challenge: bigint, 
      response: bigint
    } 
  ]
  backups: Map<int, string>
}
```

**Mapping: `/decryption`**
**Method: `GET`**  
**Response-Body:**
```
{
  state: P_DECRYPTION | PP_DECRYPTION,
  waiting: boolean
}
```

**Mapping: `/result`**
**Method: `GET`**  
**Response-Body:**  
```
{
  //Key: id of the spoiled ballot
  encryptedSpoiledBallotQuestions: Map<long, 
    //Key: question index
    Map<int, [
      {
        pad: bigint
        data: bigint
      }
    ]> 
  >,
  encryptedTally: Map<int, [
    {
      pad: bigint
      data: bigint
    }
  ]>
  keyBackups?: Map<int, String>
}
```

**Mapping: `/result`**  
**Method: `POST`**  
**Request-Body:**  
```
{
  partialDecryptedSpoiledBallots: Map<int, [
    { 
      partialDecryption: Map<int, BigInt[]>, 
      proofs: Map<int, [
        {
          pad: bigint
          data: bigint
          challenge: bigint
          response: bigint
        }
      ]>,
      ballotId: int
    } 
  ]>,
  partialDecryptedTally: Map<int, { // index des trustees im falle von teil entschlüsselung, indices von jeweiligem trustee bei teil-teil entschlüsselung
    Map<int, BigInt[]>, // frageindex auf Teilentschlüsselte Optionen
    Map<int, [
      {
        pad: bigint
        data: bigint
        challenge: bigint
        response: bigint
      }
    ]> // frageindex auf beweise zur jeweiligen Teilentschlüsselung
  }>
}
```

## VoterController
#### **Base URL: `/api/voter/{electionId}`**

**Mapping: `/`**  
**METHOD: `POST`**  
**Request-Body**  
```
{
  cipherText: Map<int, [    //Enthält auch Dummy 
    {
      pad: bigint
      data: bigint
    }
  ]>,
  individualProofs: Map<int, [
    {
      proof0: {
        pad: bigint     //Selbst pads und datas können anscheinend unterschiedlich sein
        data: bigint
        challenge: bigint
        response: bigint
      }
      proof1: {
        pad: bigint
        data: bigint
        challenge: bigint
        response: bigint
      }
      challenge: bigint
    }
  ]>, // jeweilige option ist verschlüsselung von 0 oder 1
  accumulatedProofs: Map<int, {
    pad: bigint
    data<.> bigint
    challenge: bigint
    response: bigint
    constant: int
  }>, // auswahl aufaddiert ist entspricht genau den max Auswahlmöglichkeiten
  Date: DateTime / String,
  ballotId: bigint
}
```
**Response-Body:**
```
  {
    trackingCode: String
    lastTrackingCode: String
  }
```

**Mapping: `/{trackingCode}`**  
**METHOD: `PATCH`**  
**Request-Body**  
```
{

}
```
