package pse.election.backendserver.core.bot;

import static com.sunya.electionguard.Group.TWO_MOD_Q;

import com.sunya.electionguard.ChaumPedersen;
import com.sunya.electionguard.ElGamal;
import com.sunya.electionguard.ElectionPolynomial;
import com.sunya.electionguard.Group;
import com.sunya.electionguard.SchnorrProof;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import pse.election.backendserver.core.electionguard.DecryptionFacade;
import pse.election.backendserver.core.electionguard.KeyCeremonyFacade;
import pse.election.backendserver.core.service.BallotService;
import pse.election.backendserver.core.service.DecryptionService;
import pse.election.backendserver.core.service.ElectionService;
import pse.election.backendserver.core.service.TallyService;
import pse.election.backendserver.core.service.TrusteeService;
import pse.election.backendserver.core.state.ElectionState;
import pse.election.backendserver.entity.Ballot;
import pse.election.backendserver.entity.BotTrustee;
import pse.election.backendserver.entity.Election;
import pse.election.backendserver.entity.OptionEncrypted;
import pse.election.backendserver.entity.Tally;
import pse.election.backendserver.entity.Trustee;
import pse.election.backendserver.payload.dto.ElgamalProofDTO;
import pse.election.backendserver.payload.dto.SchnorrProofDTO;
import pse.election.backendserver.payload.error.exception.IllegalStateSwitchOperation;
import pse.election.backendserver.payload.response.record.ElectionGuardInitializedWrapper;

/**
 * This class is the BotFacade an encapsulates the bot functionalities in the e-lection application. This involves
 * key creation, as well as partial and partial partial decryption.
 *
 * @version 1.0
 * */
@Service
public class BotFacade {

  private static final String INVALID_BACKUP = "Trustees delivered invalid backup for bot";
  private static final String BOT_EMAIL = "bot@bot.de";
  private static final String BOT_KEY_TYPE = "RSA";
  private static final int BASE_OF_HEX = 16;

  @Lazy
  private final TrusteeService trusteeService;

  private final KeyCeremonyFacade keyCeremonyFacade;

  @Lazy
  private final TallyService tallyService;

  @Lazy
  private final BallotService ballotService;

  @Lazy
  private final DecryptionService decryptionService;

  @Lazy
  private final ElectionGuardInitializedWrapper electionGuardInitializedWrapper;

  @Lazy
  private final ElectionService electionService;

  /**
   * Constructor of new BotFacade.
   * */
  public BotFacade(TrusteeService trusteeService, KeyCeremonyFacade keyCeremonyFacade,
      TallyService tallyService, BallotService ballotService, DecryptionService decryptionService,
      ElectionGuardInitializedWrapper electionGuardInitializedWrapper,
      ElectionService electionService) {
    this.trusteeService = trusteeService;
    this.keyCeremonyFacade = keyCeremonyFacade;
    this.tallyService = tallyService;
    this.ballotService = ballotService;
    this.decryptionService = decryptionService;
    this.electionGuardInitializedWrapper = electionGuardInitializedWrapper;
    this.electionService = electionService;
  }

  /**
   * This method is used to verify election partial key backups.
   * */
  public static boolean verifyEPKB(List<Group.ElementModP> publicKeys, Group.ElementModQ backup,
      int id) {
    List<Group.ElementModP> mults = new ArrayList<>();
    Group.ElementModP idMod = Group.int_to_p_unchecked(BigInteger.valueOf(id));
    for (int j = 0; j < publicKeys.size(); j++) {
      Group.ElementModP jmod = Group.int_to_p_unchecked(BigInteger.valueOf(j));
      Group.ElementModP exponent = Group.pow_p(idMod, jmod);
      Group.ElementModP publicKey = publicKeys.get(j);
      mults.add(Group.pow_p(publicKey, exponent));
    }
    Group.ElementModP rightPart = Group.mult_p(mults);
    Group.ElementModP leftPart = Group.g_pow_p(backup);
    return rightPart.equals(leftPart);
  }

  /**
   * This method creates a new bot to an election. The bot is used to simulate a trustee.
   *
   * @param electionId       is the id of the election
   * @param numberOfTrustees is the number of trustees in the election
   */
  public void createBot(long electionId, int numberOfTrustees) {
    Trustee botAsTrustee = createBotAsTrustee(electionId, numberOfTrustees);

    KeyPair auxKey = keyCeremonyFacade.generateRsaKey();
    String privateKey = Base64.getEncoder().encodeToString(auxKey.getPrivate().getEncoded());
    String publicKey = Base64.getEncoder().encodeToString(auxKey.getPublic().getEncoded());

    //saving a new bot that simulates the trustee
    botAsTrustee.setAuxkey(publicKey);
    trusteeService.addTrustee(botAsTrustee);

    //saving a new bot that contains bot information
    BotTrustee bot = new BotTrustee(electionId, null, botAsTrustee.getTrusteeId(), privateKey);
    trusteeService.saveBotTrustee(bot);
  }

  private Trustee createBotAsTrustee(long electionId, int numberOfTrustees) {
    Trustee botAsTrustee = new Trustee(electionId, BOT_EMAIL, numberOfTrustees + 1);
    botAsTrustee.setAuxkeyType(BOT_KEY_TYPE);

    //initially waiting
    botAsTrustee.setWaiting(true);
    return botAsTrustee;
  }

  /**
   * Creating and adding the elgamal keys and backups of the bot.
   *
   * @param election is the election to create the keys and backups for
   */
  public void addElgamalKeysAndBackups(Election election) {
    List<Trustee> allTrusteesOfElection = trusteeService.getAllTrustees(election.getElectionId());
    List<Group.ElementModP> publicKeys = new ArrayList<>();
    List<Group.ElementModQ> privateKeys = new ArrayList<>();
    List<SchnorrProof> schnorrProofs = new ArrayList<>();
    List<SchnorrProofDTO> proofs = new ArrayList<>();

    //Generate ElGamalKeys
    Collections.sort(allTrusteesOfElection);
    for (int i = 0; i < election.getTrusteeThreshold(); i++) {
      Group.ElementModQ privateKey = Group.rand_range_q(TWO_MOD_Q);
      ElGamal.KeyPair pair = ElGamal.elgamal_keypair_from_secret(privateKey).orElseThrow();
      Group.ElementModP publicKey = pair.public_key();
      SchnorrProof schnorrProof = SchnorrProof.make_schnorr_proof(pair, privateKey);

      publicKeys.add(publicKey);
      privateKeys.add(privateKey);
      schnorrProofs.add(schnorrProof);
      proofs.add(new SchnorrProofDTO(
          schnorrProof.publicKey.getBigInt(),
          schnorrProof.commitment.getBigInt(),
          schnorrProof.challenge.getBigInt(),
          schnorrProof.response.getBigInt())
      );
    }

    Map<Integer, String> encryptedBackups;
    BotTrustee botTrustee = trusteeService.getBotTrustee(election.getElectionId());
    Trustee botAsTrustee = trusteeService.getTrustee(botTrustee.getTrusteeId());
    int botIndex = botAsTrustee.getIndex();

    try {
      encryptedBackups = createEncryptedBackups(privateKeys, publicKeys, schnorrProofs,
          allTrusteesOfElection, botIndex);
    } catch (NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeySpecException
             | InvalidKeyException | IllegalBlockSizeException | BadPaddingException e) {
      throw new IllegalArgumentException(
          "There has been an error creating the bot's elgamal keys and backups. More details: "
              + e.getMessage());
    }
    botTrustee.setPrivateElgamalKey(privateKeys.get(0).base16());
    ElgamalProofDTO elgamalProofDTO = new ElgamalProofDTO(proofs.toArray(SchnorrProofDTO[]::new),
        encryptedBackups);
    trusteeService.addElgamalKeysAndBackups(elgamalProofDTO, botAsTrustee.getEmail(),
        election.getElectionId());
    trusteeService.saveBotTrustee(botTrustee);
  }

  /**
   * Verifies that the backups provided by other Trustees are correct.
   *
   * @param electionId id of the election
   */
  public void verifyProvidedBackup(long electionId, ElgamalProofDTO proof) {
    BotTrustee bot = trusteeService.getBotTrustee(electionId);
    Trustee botTrustee = trusteeService.getTrustee(BOT_EMAIL, electionId);
    List<Group.ElementModP> publicKeys = new ArrayList<>();
    SchnorrProofDTO[] encodedPublicKeys = proof.proofs();
    for (SchnorrProofDTO encodedKey : encodedPublicKeys) {
      publicKeys.add(Group.int_to_p_unchecked(encodedKey.publicKey()));
    }
    String backup = decryptBackup(bot, proof.backups().get(botTrustee.getIndex()));
    Group.ElementModQ coordinatePoint = Group.hex_to_q_unchecked(backup);
    if (!verifyEPKB(publicKeys, coordinatePoint, botTrustee.getIndex())) {
      throw new IllegalStateSwitchOperation(INVALID_BACKUP);
    }
  }

  /**
   * Creates the encrypted Backups for the BotTrustee. Computes them by using the before generated
   * public and private ElGamal Key to compute the election Polynomial. Encrypts each backup with
   * the public Aux key of the trustee to whom the backup is addressed to.
   *
   * @param privateKeys           private ElGamal Key of the bot Trustee
   * @param publicKeys            public ElGamal key of the bot Trustee
   * @param schnorrProofs         proofs which verify the validity of the ElGamal keys
   * @param allTrusteesOfElection the trustees for which the backups get created
   * @param botIndex              the index of the bot Trustee
   * @return A Map which maps the encoded Backup to the corresponding TrusteeIndex
   */
  private Map<Integer, String> createEncryptedBackups(List<Group.ElementModQ> privateKeys,
      List<Group.ElementModP> publicKeys,
      List<SchnorrProof> schnorrProofs,
      List<Trustee> allTrusteesOfElection,
      int botIndex)
      throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeySpecException,
      InvalidKeyException, IllegalBlockSizeException, BadPaddingException {

    Map<Integer, String> encryptedBackups = new HashMap<>();
    ElectionPolynomial polynomial = new ElectionPolynomial(privateKeys, publicKeys, schnorrProofs);

    for (Trustee trustee : allTrusteesOfElection) {
      if (botIndex != trustee.getIndex()) {

        Group.ElementModQ cord = (ElectionPolynomial.compute_polynomial_coordinate(
            BigInteger.valueOf(trustee.getIndex()), polynomial));

        byte[] cordBytes = cord.getBigInt().toString(16).getBytes();

        Cipher cipher = Cipher.getInstance(trustee.getAuxkeyType());

        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(
            Base64.getDecoder().decode(trustee.getAuxkey().getBytes()));
        KeyFactory keyFactory = KeyFactory.getInstance(trustee.getAuxkeyType());
        PublicKey myKey = keyFactory.generatePublic(keySpec);

        cipher.init(Cipher.ENCRYPT_MODE, myKey);
        encryptedBackups.put(trustee.getIndex(),
            Base64.getEncoder().encodeToString(cipher.doFinal(cordBytes)));
      }
    }
    return encryptedBackups;
  }

  /**
   * Method which decrypts all the combined Tally of an Election for a specific key.
   *
   * @param election election which gets decrypted.
   * @param key      the corresponding Key
   * @return the decrypted values mapped to the tallies.
   */
  public Map<Long, DecryptionFacade.Decryption> decryptTalliesOfElection(Election election,
      Group.ElementModQ key) {

    List<Tally> tallyList = tallyService.getAllTalliesOfElection(election.getElectionId());
    Map<Long, DecryptionFacade.Decryption> tallyDecryption = new HashMap<>();
    for (Tally tally : tallyList) {
      tallyDecryption.put(
          tally.getTallyId(),
          decrypt(election, key,
              new ElGamal.Ciphertext(
                  Group.int_to_p_unchecked(tally.getCiphertextPAD()),
                  Group.int_to_p_unchecked(tally.getCiphertextDATA())
              )
          )
      );
    }

    return tallyDecryption;

  }

  /**
   * Method which decrypts all SpoiledBallots of an Election for a corresponding key.
   *
   * @param election election for which the spoiled Ballots get decrypted
   * @param key      the corresponding key for the decryption
   * @return the Decryption of all ballots
   */
  private Map<Long, Map<Long, DecryptionFacade.Decryption>> decryptSpoiledBallotsOfElection(
      Election election, Group.ElementModQ key) {
    Map<Long, Map<Long, DecryptionFacade.Decryption>> spoiledBallotDecryption = new HashMap<>();
    List<Ballot> spoiledBallotOfElection = ballotService.getAllSpoiledBallotsOfElection(
        election.getElectionId());

    for (Ballot spoiledBallot : spoiledBallotOfElection) {
      List<OptionEncrypted> optionEncryptedList = ballotService.getAllOptionsEncryptedOfBallot(
          spoiledBallot.getBallotId());
      Collections.sort(optionEncryptedList);
      Map<Long, DecryptionFacade.Decryption> decryptionOfSpoiledBallotList = new HashMap<>();

      for (OptionEncrypted optionEncrypted : optionEncryptedList) {

        decryptionOfSpoiledBallotList.put(
            optionEncrypted.getOptionEncryptedId(),
            decrypt(election, key,
                new ElGamal.Ciphertext(
                    Group.int_to_p_unchecked(optionEncrypted.getCiphertextPAD()),
                    Group.int_to_p_unchecked(optionEncrypted.getCiphertextDATA())
                )
            )
        );
      }
      spoiledBallotDecryption.put(spoiledBallot.getBallotId(), decryptionOfSpoiledBallotList);
    }
    return spoiledBallotDecryption;
  }

  /**
   * This is the functionality to partial decrypt on behalf of the bot trustee.
   *
   * @param election is the election to partial decrypt for
   */
  public void partialDecryption(Election election) {
    BotTrustee botTrustee = trusteeService.getBotTrustee(election.getElectionId());
    Map<Long, DecryptionFacade.Decryption> electionDecryption =
        decryptTalliesOfElection(election,
            Group.int_to_q_unchecked(botTrustee.getPrivateElgamalKey()));
    Map<Long, Map<Long, DecryptionFacade.Decryption>> spoiledBallotDecryption =
        decryptSpoiledBallotsOfElection(election,
            Group.int_to_q_unchecked(botTrustee.getPrivateElgamalKey()));
    decryptionService.addPartialDecryptionBotTrustee(electionDecryption, spoiledBallotDecryption,
        election);
  }

  /**
   * This is the functionality to partial-partial decrypt on behalf of the bot trustee. By doing so,
   * each backup gets decrypted and used for the decryption.
   *
   * @param election is the election to partial-partial decrypt for
   */
  public void partialPartialDecryption(Election election) {
    List<Trustee> trusteeList = trusteeService.getAllTrustees(election.getElectionId());
    Collections.sort(trusteeList);
    BotTrustee botTrustee = trusteeService.getBotTrustee(election.getElectionId());
    Map<Integer, String> backups = trusteeService.getBackups(BOT_EMAIL, election.getElectionId(), true);
    for (Integer trusteeIndex : backups.keySet()) {
      String decryptedBackup = decryptBackup(botTrustee, backups.get(trusteeIndex));
      Map<Long, DecryptionFacade.Decryption> electionDecryption = decryptTalliesOfElection(election,
              Group.int_to_q_unchecked(new BigInteger(decryptedBackup, BASE_OF_HEX)));
      Map<Long, Map<Long, DecryptionFacade.Decryption>> spoiledBallotDecryption =
              decryptSpoiledBallotsOfElection(election,
                      Group.int_to_q_unchecked(new BigInteger(decryptedBackup, BASE_OF_HEX)));
      decryptionService.addPartialPartialDecryptionBotTrustee(electionDecryption,
              spoiledBallotDecryption, trusteeIndex, election);
    }
    electionService.tryUpdateState(election.getElectionId(), ElectionState.DONE);
  }

  private String decryptBackup(BotTrustee bot, String backup) {
    try {
      PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(
          Base64.getDecoder().decode(bot.getPrivateAuxKey().getBytes()));
      KeyFactory keyFactory = KeyFactory.getInstance(BOT_KEY_TYPE);
      PrivateKey privateKey = keyFactory.generatePrivate(keySpec);

      Cipher cipher = Cipher.getInstance(BOT_KEY_TYPE);
      cipher.init(Cipher.DECRYPT_MODE, privateKey);

      byte[] encryptedMessageBytes = Base64.getDecoder().decode(backup);
      byte[] decryptedMessageBytes = cipher.doFinal(encryptedMessageBytes);
      return new String(decryptedMessageBytes, StandardCharsets.UTF_8);

    } catch (NoSuchAlgorithmException | InvalidKeySpecException | NoSuchPaddingException
             | IllegalBlockSizeException | BadPaddingException | InvalidKeyException e) {
      throw new IllegalArgumentException(e.getMessage());
    }
  }

  /**
   * Decrypted a single ElGamal ciphertext for a specific key.
   *
   * @param key        key
   * @param ciphertext ciphertext that gets decrypted
   * @return the decryption
   */
  private DecryptionFacade.Decryption decrypt(Election election, Group.ElementModQ key,
      ElGamal.Ciphertext ciphertext) {
    Group.ElementModP decryption = ciphertext.partial_decrypt(key);
    ChaumPedersen.ChaumPedersenProof correspondingDecryptionProof =
        ChaumPedersen.make_chaum_pedersen(ciphertext, key, decryption, Group.rand_q(),
            electionGuardInitializedWrapper.generateCryptoContext(
                election.getElectionId()).cryptoExtendedBaseHash);
    return new DecryptionFacade.Decryption(decryption, correspondingDecryptionProof);
  }

}
