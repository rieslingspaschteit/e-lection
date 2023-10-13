package pse.election.backendserver.core.electionguard;


import com.sunya.electionguard.ChaumPedersen;
import com.sunya.electionguard.ElGamal;
import com.sunya.electionguard.ElectionCryptoContext;
import com.sunya.electionguard.Group;
import com.sunya.electionguard.SchnorrProof;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import pse.election.backendserver.core.service.BallotService;
import pse.election.backendserver.core.service.ElectionService;
import pse.election.backendserver.core.service.TallyService;
import pse.election.backendserver.entity.Contest;
import pse.election.backendserver.entity.Election;
import pse.election.backendserver.entity.OptionEncrypted;
import pse.election.backendserver.entity.Tally;
import pse.election.backendserver.payload.dto.BallotProofDTO;
import pse.election.backendserver.payload.dto.ChaumPedersenProofDTO;
import pse.election.backendserver.payload.dto.ConstantChaumPedersenDTO;
import pse.election.backendserver.payload.dto.DecryptionDTO;
import pse.election.backendserver.payload.dto.DisjunctiveChaumPedersenDTO;
import pse.election.backendserver.payload.dto.ElgamalProofDTO;
import pse.election.backendserver.payload.dto.EncryptedOptionDTO;
import pse.election.backendserver.payload.dto.SchnorrProofDTO;
import pse.election.backendserver.payload.error.exception.IllegalProofException;
import pse.election.backendserver.payload.response.record.ElectionGuardInitializedWrapper;

/**
 * Facade for the electionGuard java implementation. Implements the interface Verifiable. Uses the
 * proof verification tools of the electionGuard java implementation to validate proofs accumulated
 * by an election.
 */
@Component
public class VerificationFacade {

  @Autowired
  @Lazy
  private ElectionService electionService;

  @Autowired
  @Lazy
  private TallyService tallyService;

  @Autowired
  @Lazy
  private BallotService ballotService;

  @Autowired
  @Lazy
  private ElectionGuardInitializedWrapper electionGuardInitializedWrapper;

  /**
   * Verifies the decryption send by a trustee by verifying the associated ChaumPedersen proofs.
   *
   * @return true if it's a correct Decryption, else false.
   */
  public boolean verifyDecryption(DecryptionDTO.PartialDecryptionDTO decryptionProofDTO,
      long electionId, BigInteger key) {

    List<Contest> contestList = electionService.getAllContestsOfElection(electionId);

    Map<Integer, EncryptedOptionDTO[]> tally = new HashMap<>();
    for (Contest contest : contestList) {
      tally.put(contest.getIndex(),
          new EncryptedOptionDTO[contest.getOptions().size() + contest.getMax()]);
    }
    if (decryptionProofDTO.ballotId() == -1) {
      List<Tally> tallies = tallyService.getAllTalliesOfElection(electionId);
      tallies
          .forEach(t -> tally.get(t.getContestIndex())[t.getOptionIndex()] = new EncryptedOptionDTO(
              t.getCiphertextPAD(), t.getCiphertextDATA()));
    } else {
      List<OptionEncrypted> tallies = ballotService.getAllOptionsEncryptedOfBallot(
          decryptionProofDTO.ballotId());
      tallies
          .forEach(t -> tally.get(t.getContestIndex())[t.getOptionIndex()] = new EncryptedOptionDTO(
              t.getCiphertextPAD(), t.getCiphertextDATA()));
    }
    for (Contest contest : contestList) {
      ChaumPedersenProofDTO[] chaumPedersenProofDTOList =
          decryptionProofDTO.chaumPedersonProofs().get(contest.getIndex());
      BigInteger[] decryption = decryptionProofDTO.partialDecryptedOptions()
          .get(contest.getIndex());
      EncryptedOptionDTO[] ciphertext = tally.get(contest.getIndex());
      for (int i = 0; i < decryption.length; i++) {
        if (!validateGenericChaumPedersenProof(
            chaumPedersenProofDTOList[i],
            ciphertext[i].pad(),
            ciphertext[i].data(),
            key,
            electionId,
            decryption[i])) {
          return false;
        }
      }
    }
    return true;
  }


  /**
   * Verifies an encrypted ballot by verifying the associated ChaumPedersen proofs.
   *
   * @return true if its a correct encrypted Ballot, else false.
   */
  public boolean verifyBallot(BallotProofDTO ballotProofDTO, Election election) {
    List<Contest> contestList = electionService.getAllContestsOfElection(election.getElectionId());

    ElectionCryptoContext electionCryptoContext = electionGuardInitializedWrapper.generateCryptoContext(
        election.getElectionId());

    for (Contest contest : contestList) {
      EncryptedOptionDTO[] encryptedOptionDTO = ballotProofDTO.cipherText().get(contest.getIndex());
      DisjunctiveChaumPedersenDTO[] individualProofs = ballotProofDTO.individualProofs()
          .get(contest.getIndex());
      ConstantChaumPedersenDTO accumulatedProof = ballotProofDTO.accumulatedProofs()
          .get(contest.getIndex());
      ElGamal.Ciphertext[] combinedCiphertexts = new ElGamal.Ciphertext[encryptedOptionDTO.length];
      for (int i = 0; i < encryptedOptionDTO.length; i++) {
        combinedCiphertexts[i] = new ElGamal.Ciphertext(
            Group.int_to_p_unchecked(encryptedOptionDTO[i].pad()),
            Group.int_to_p_unchecked(encryptedOptionDTO[i].data()));
        if (!validateDisjunctiveChaumPedersen(individualProofs[i],
            election,
            electionCryptoContext.cryptoExtendedBaseHash.getBigInt(),
            encryptedOptionDTO[i].pad(),
            encryptedOptionDTO[i].data()
        )) {
          return false;
        }
      }
      ElGamal.Ciphertext accumulatedCiphertext = ElGamal.elgamal_add(combinedCiphertexts);
      if (!validateConstantChaumPedersen(accumulatedProof, election,
          accumulatedCiphertext.pad().getBigInt(),
          accumulatedCiphertext.data().getBigInt(),
          electionCryptoContext.cryptoExtendedBaseHash.getBigInt())) {
        return false;
      }
      if (accumulatedProof.constant() != contest.getMax()) {
        return false;
      }
    }
    return true;
  }

  /**
   * Verifies an ElGamalKey by verifying the associated SchnorrProof.
   *
   * @return true if the trustee data and keys are correct, else false.
   */
  public boolean verifyKeyCeremony(ElgamalProofDTO elgamalProofDTO) {
    for (SchnorrProofDTO elem : elgamalProofDTO.proofs()) {
      if (!validateSchnorrProof(elem)) {
        return false;
      }
    }
    return true;
  }

  /**
   * Verifies a given SchnorrProof.
   *
   * @param schnorrProofDTO the proof to be verified
   */
  private boolean validateSchnorrProof(SchnorrProofDTO schnorrProofDTO)
      throws IllegalProofException {
    SchnorrProof schnorrProof = new SchnorrProof(
        Group.int_to_p_unchecked(schnorrProofDTO.publicKey()),
        Group.int_to_p_unchecked(schnorrProofDTO.commitment()),
        Group.int_to_q_unchecked(schnorrProofDTO.challenge()),
        Group.int_to_q_unchecked(schnorrProofDTO.response()));
    return schnorrProof.isValidVer1()
        && schnorrProof.isValidVer2(Group.int_to_p_unchecked(schnorrProofDTO.publicKey()));

  }

  /**
   * Validates a Constant ChaumPedersen Proof.
   *
   * @param constantChaumPedersenDTO the proof that gets verified
   * @param election                 elecion for which the proof gets generated
   * @param accumulatedPad           accumulation over all EncryptionPads
   * @param accumulatedData          accumulation over all EncryptionData
   * @param baseHash                 electionBase Hash
   * @return true if it's a valid proof
   */

  public boolean validateConstantChaumPedersen(ConstantChaumPedersenDTO constantChaumPedersenDTO,
      Election election,
      BigInteger accumulatedPad, BigInteger accumulatedData,
      BigInteger baseHash) {
    ChaumPedersen.ConstantChaumPedersenProof constantChaumPedersenProof =
        new ChaumPedersen.ConstantChaumPedersenProof(
            Group.int_to_p_unchecked(constantChaumPedersenDTO.pedersenProofDTO().pad()),
            Group.int_to_p_unchecked(constantChaumPedersenDTO.pedersenProofDTO().data()),
            Group.int_to_q_unchecked(constantChaumPedersenDTO.pedersenProofDTO().challenge()),
            Group.int_to_q_unchecked(constantChaumPedersenDTO.pedersenProofDTO().response()),
            constantChaumPedersenDTO.constant());
    return constantChaumPedersenProof.is_valid(
        // Ciphertext of pad and data
        new ElGamal.Ciphertext(
            Group.int_to_p_unchecked(accumulatedPad),
            Group.int_to_p_unchecked(accumulatedData)
        ),
        // public Key of the election
        Group.int_to_p_unchecked(election.getPublicKey()),
        // base hash of the election
        Group.int_to_q_unchecked(baseHash));
  }

  /**
   * Validates a GenericChaumPedersenProof.
   *
   * @param chaumPedersenProofDTO the chaum PedersenProof
   * @param ciphertextPAD         Pad of the value that gets proofen
   * @param ciphertextDATA        pad of teh value that gets proofen
   * @param key                   key with which the Proof got generated / Trustee private ELGamal
   *                              Keys
   * @param electionId            electionId for which this proof was generated
   * @param message               which gets validated
   * @return true if the proof is valid, else false
   */
  public boolean validateGenericChaumPedersenProof(ChaumPedersenProofDTO chaumPedersenProofDTO,
      BigInteger ciphertextPAD,
      BigInteger ciphertextDATA,
      BigInteger key,
      long electionId,
      BigInteger message) {
    ElectionCryptoContext electionCryptoContext = electionGuardInitializedWrapper.generateCryptoContext(
        electionId);
    ChaumPedersen.ChaumPedersenProof chaumPedersenProof =
        new ChaumPedersen.ChaumPedersenProof(
            Group.int_to_p_unchecked(chaumPedersenProofDTO.pad()),
            Group.int_to_p_unchecked(chaumPedersenProofDTO.data()),
            Group.int_to_q_unchecked(chaumPedersenProofDTO.challenge()),
            Group.int_to_q_unchecked(chaumPedersenProofDTO.response()));
    return chaumPedersenProof.is_valid(
        // Ciphertext
        new ElGamal.Ciphertext(
            Group.int_to_p_unchecked(ciphertextPAD),
            Group.int_to_p_unchecked(ciphertextDATA)),
        // Public key corresponding to the private key used for the decryption
        Group.int_to_p_unchecked(key),
        // Share
        Group.int_to_p_unchecked(message),
        // Election base Hash
        electionCryptoContext.cryptoExtendedBaseHash);
  }

  /**
   * Verifies that a DisjunctiveChaumPedersen Proof is valid.
   *
   * @param disjunctiveChaumPedersenDTO proof that gets verified
   * @param election                    election for which the proofs get verified
   * @param baseHash                    base Hash of the election
   * @param decryptionPad               Pad of the decryption
   * @param decryptionData              Data of the decryption
   * @return true if valid, else false
   */
  private boolean validateDisjunctiveChaumPedersen(
      DisjunctiveChaumPedersenDTO disjunctiveChaumPedersenDTO,
      Election election, BigInteger baseHash, BigInteger decryptionPad,
      BigInteger decryptionData) {
    ChaumPedersen.DisjunctiveChaumPedersenProof disjunctiveChaumPedersenProof =
        new ChaumPedersen.DisjunctiveChaumPedersenProof(
            Group.int_to_p_unchecked(disjunctiveChaumPedersenDTO.proof0().pad()),
            Group.int_to_p_unchecked(disjunctiveChaumPedersenDTO.proof0().data()),
            Group.int_to_p_unchecked(disjunctiveChaumPedersenDTO.proof1().pad()),
            Group.int_to_p_unchecked(disjunctiveChaumPedersenDTO.proof1().data()),
            Group.int_to_q_unchecked(disjunctiveChaumPedersenDTO.proof0().challenge()),
            Group.int_to_q_unchecked(disjunctiveChaumPedersenDTO.proof1().challenge()),
            Group.int_to_q_unchecked(disjunctiveChaumPedersenDTO.challenge()),
            Group.int_to_q_unchecked(disjunctiveChaumPedersenDTO.proof0().response()),
            Group.int_to_q_unchecked(disjunctiveChaumPedersenDTO.proof1().response()));
    return disjunctiveChaumPedersenProof.is_valid(
        new ElGamal.Ciphertext(
            Group.int_to_p_unchecked(decryptionPad),
            Group.int_to_p_unchecked(decryptionData)),
        Group.int_to_p_unchecked(election.getPublicKey()),
        Group.int_to_q_unchecked(baseHash));
  }

}
