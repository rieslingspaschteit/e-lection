package pse.election.backendserver.repository;

import org.springframework.data.repository.CrudRepository;
import pse.election.backendserver.entity.BotTrustee;

/**
 * This interface provides methods for the bot trustee repository.
 * */
public interface BotTrusteeRepository extends CrudRepository<BotTrustee, Long> {

  /**
   * Getter if the bot trustee to a referenced election exists.
   * */
  boolean existsByTrusteeIdAndElectionId(long trusteeId, long electionId);


}
