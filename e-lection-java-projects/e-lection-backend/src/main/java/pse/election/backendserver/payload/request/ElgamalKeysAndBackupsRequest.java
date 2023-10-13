package pse.election.backendserver.payload.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Collection;
import java.util.Map;

/**
 * This class is used for translation of elgamal keys from the requests into an object that can be
 * managed by the system. The elgamal keys are used to create a public election key and for the
 * therefore used to encrypt the ballots.
 *
 * @version 1.0
 */
public class ElgamalKeysAndBackupsRequest {

  @JsonProperty("proofs")
  private Collection<SchnorrProofRequest> proofs;

  @JsonProperty("backups")
  private Map<String, String> backups;

  /**
   * Getter for the proofs from the public key. They ensure that the user uploading the key is in
   * possession of the private key without sharing the secret.
   *
   * @return collection of proofs to the elgamal key
   */
  public Collection<SchnorrProofRequest> getProofs() {
    return proofs;
  }

  /**
   * Setter of the proofs for the keys translated from the request.
   *
   * @param proofs are the proofs containing the details of a key
   */
  public void setProofs(Collection<SchnorrProofRequest> proofs) {
    this.proofs = proofs;
  }

  /**
   * Getter for the election partial key backups used to be shared among the trustees. Without them,
   * every assigned trustee has to participate on the decryption.
   *
   * @return election partial key backups created with the elgamal keys of the trustee
   */
  public Map<String, String> getBackups() {
    return backups;
  }

  /**
   * Setter for the election partial key backups translated from the request.
   *
   * @param backups are they partial key backups
   */
  public void setBackups(Map<String, String> backups) {
    this.backups = backups;
  }
}
