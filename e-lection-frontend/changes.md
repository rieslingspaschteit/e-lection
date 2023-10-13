# <u>Entwurfsänderungen</u>

## Model

### User
- User Klasse
### Ballot
#### Alle ballots haben eine Map für Optionen (PlainTextBallot, EncryptedBallot)

### Reihenfolge von Attributen geändert
#### EncryptedBallot deviceInformation sind jetzt optional
#### MutableQuestion: Leere Option darf fehlen, Reihenfolge in MutableBallot aus konsistenzgründen geändert

### Attribute geändert
#### SpoiledBallot hat nur noch die Optionen als Attribut statt PlaintextBallot, da restliche Funktionalität nicht benötigt wird
#### SpoiledBallot hat ELementModQ als nonce für bessere Kompatibilität

### Globale Konstanten
#### GroupContext als globale Konstante

## Election

### Manifest
- votersHash dazu, voters können nicht mehr gefetched werden

### AuthorityElection
- decryptionState ist readonly -> kein setter mehr
- alle attribute optional

## Handler

### ElectionHandler

- neue Funktion `protected handleError(error: any) : void` die errors einer Backend anfrage behandelt
- return type aller Funktion außer getElection() ist async und returned ein Promise
- fetchBallot aus IVoterHandler entfernt
- fetchUserRoles -> fetchUserInformation
- docs of fetch /user/ to /elections/
- ResponseError um unvorhergesehene responses zu handel
- fetchVoters -> fetchVotersHash

### VoterHandler
- spoilBallot() entfernt (ist implizit spoiled)

# <u>Entwurfsänderungen</u>

## Model
### Ballot
#### Alle ballots haben eine Map für Optionen (PlainTextBallot, EncryptedBallot)

### Reihenfolge von Attributen geändert
#### EncryptedBallot deviceInformation sind jetzt optional
#### MutableQuestion: Leere Option darf fehlen, Reihenfolge in MutableBallot aus konsistenzgründen geändert

### Attribute geändert
#### SpoiledBallot hat nur noch die Optionen als Attribut statt PlaintextBallot, da restliche Funktionalität nicht benötigt wird
#### SpoiledBallot hat ELementModQ als nonce für bessere Kompatibilität
#### SpoiledBallot hat hashes zur Verschlüsselung
#### EncryptedBallot kein isSubmitted

### Globale Konstanten
#### GroupContext als globale Konstante


## Encryptor
#### Alle internen BigInts sind jetzt Elements aus Einheitsgründen
#### Encryptor: nonce heißt jetzt seed

#### nonceGenerator für nonces, manifestHahses für hashes
#### plaintextBallot kann jetzt die Dummy Optionen hinzufügen, da am einfachsten
#### global utils Methode um Key Gleichheut zu prüfen
## Web-API Specification
- Attribut 'tracking-codes' => 'trackingCodes' bei get ballot-board
- 