package electioncli.core;

public enum Identifiers {
    PUBLIC_KEY("publicKey"),
    PUBLIC_KEYS("publicKeys"),
    PRIVATE_KEY("privateKey"),
    ENCRYPTION_TYPE("keyType"),
    DEFAULT_TYPE("RSA"),
    PROOF("proofs"),
    BACKUP("backups"),
    BACKUP_SECRET("point"),
    COUNT("n"),
    THRESHOLD("threshold"),
    ID("id"),
    AUX_KEY("auxKeys"),
    SCNORR_COMMITMENT("commitment"),
    PROOF_CHALLENGE("challenge"),
    PROOF_RESPONSE("response"),
    ENCRYPTED_SPOILED_BALLOT("encryptedSpoiledBallotQuestions"),
    ENCRYPTED_TALLY("encryptedTally"),
    KEY_BACKUPS("keyBackups"),
    PAD("pad"),
    DATA("data"),
    DECRYPTED_SPOILED_BALLOT("partialDecryptedSpoiledBallots"),
    DECRYPTED_TALLY("partialDecryptedTally"),
    BASE_HASH("baseHash"),
    DECRYPTION("partialDecryption"),
    BALLOT_ID("ballotId")
    ;
    private String name;
    private Identifiers(String name) {
        this.name = name;
    }
    public String getName() {
        return this.name;
    }
}
