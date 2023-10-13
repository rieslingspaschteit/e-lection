package pse.election.backendserver.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.math.BigInteger;

/**
 * This class encapsulated the information that has to be stored for the bot. This contains his private keys.
 *
 * @version 1.0
 * */
@Entity
public class BotTrustee {

  /**
   * This is the foreign key to the election. There can only be one bot trustee to each election, so
   * this being also the primary key is no problem.
   */
  @Id
  private long electionId;

  @Column(columnDefinition = "TEXT")
  private String privateElgamalKey;

  /*
   * This is the foreign key to the trustee entity for further information
   * on the bot trustee.
   */
  private long trusteeId;

  @Column(columnDefinition = "TEXT")
  private String privateAuxKey;

  public BotTrustee() {
  }

  /**
   * Constructor of new BotTrustee.
   * */
  public BotTrustee(long electionId, String privateElgamalKey, long trusteeId,
      String privateAuxKey) {
    this.electionId = electionId;
    this.privateElgamalKey = privateElgamalKey;
    this.trusteeId = trusteeId;
    this.privateAuxKey = privateAuxKey;
  }

  /**
   * Getter for the foreign key of an election.
   *
   * @return {@link long} identifier of election
   */
  public long getElectionId() {
    return this.electionId;
  }

  /**
   * Getter for the private Elgamal key of the bot trustee.
   *
   * @return private elgamal key
   */
  public BigInteger getPrivateElgamalKey() {
    return new BigInteger(privateElgamalKey, 16);
  }

  public void setPrivateElgamalKey(String privateElgamalKey) {
    this.privateElgamalKey = privateElgamalKey;
  }

  /**
   * Getter for the trustee identifier.
   *
   * @return id of trustee
   */
  public long getTrusteeId() {
    return trusteeId;
  }

  /**
   * Getter for the private auxiliary key needed to assure a secure communication with other
   * trustees.
   *
   * @return private auxiliary key
   */
  public String getPrivateAuxKey() {
    return privateAuxKey;
  }
}

