package pse.election.backendserver.core.electionguard;

import com.sunya.electionguard.ElGamal;
import com.sunya.electionguard.Group;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import pse.election.backendserver.core.service.TrusteeService;
import pse.election.backendserver.entity.Election;
import pse.election.backendserver.entity.Trustee;

/**
 * Facade for the electionGuard java implementation. Implements the interface IKeyCeremony. Uses the
 * ElGamal generations tools of the electionGuard java implementation to execute a KeyCeremony.
 */
@Component
public class KeyCeremonyFacade {

  @Autowired
  @Lazy
  private TrusteeService trusteeService;

  /**
   * Combines the public ElGamal key of all trustees to generate a joinedElGamalKey.
   *
   * @param election the election for which the keys get combined
   * @return the generated ElGamalKeyPair
   */
  public BigInteger combineKeys(Election election) {
    Collection<Group.ElementModP> primaryPublicKeyTrustees = new ArrayList<>();
    for (Trustee elem : trusteeService.getAllTrustees(election.getElectionId())) {
      primaryPublicKeyTrustees.add(Group.int_to_p_unchecked(elem.getPrimaryKey()));
    }
    return ElGamal.elgamal_combine_public_keys(primaryPublicKeyTrustees).getBigInt();
  }

  /**
   * Generates the a key pair consisting of a public RSA key and a private RSA key.
   *
   * @return key pair
   */
  public KeyPair generateRsaKey() {
    KeyPairGenerator generator;
    try {
      generator = KeyPairGenerator.getInstance("RSA");
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalArgumentException();
    }
    return generator.generateKeyPair();
  }

  /**
   * Returns the key share a trustee has for a missing trustee.
   *
   * @param coefficients           the coefficients of the missing trustee
   * @param availableGuardianOrder order of the available trustee
   * @return the key share
   */
  public BigInteger generateKeyShare(List<BigInteger> coefficients, int availableGuardianOrder) {
    List<Group.ElementModP> shares = new ArrayList<>();
    for (int j = 0; j < coefficients.size(); j++) {
      Group.ElementModP commitment = Group.int_to_p_unchecked(coefficients.get(j));
      shares.add(Group.pow_p(commitment,
          Group.int_to_q_unchecked((int) Math.pow(availableGuardianOrder, j))));
    }
    return Group.mult_p(shares).getBigInt();
  }


}
