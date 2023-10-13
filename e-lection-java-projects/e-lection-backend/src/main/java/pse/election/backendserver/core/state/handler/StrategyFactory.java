package pse.election.backendserver.core.state.handler;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import pse.election.backendserver.core.bot.BotFacade;
import pse.election.backendserver.core.electionguard.DecryptionFacade;
import pse.election.backendserver.core.electionguard.HashFacade;
import pse.election.backendserver.core.electionguard.KeyCeremonyFacade;
import pse.election.backendserver.core.service.BallotService;
import pse.election.backendserver.core.service.DecryptionService;
import pse.election.backendserver.core.service.ElectionService;
import pse.election.backendserver.core.service.TrusteeService;

/**
 * This class is used as a factory for the strategy.
 *
 * @version 1.0
 * */
@Component
public class StrategyFactory {
  private final TrusteeService trusteeService;

  private final KeyCeremonyFacade keyCeremonyFacade;

  private final HashFacade hashFacade;

  private final DecryptionService decryptionService;

  private final DecryptionFacade decryptionFacade;

  private final BotFacade botFacade;

  /**
   * Constructor of new StrategyFactory.
   * */
  @Lazy
  public StrategyFactory(TrusteeService trusteeService, KeyCeremonyFacade keyCeremonyFacade,
      HashFacade hashFacade, DecryptionService decryptionService, DecryptionFacade decryptionFacade,
                         BotFacade botFacade) {
    this.trusteeService = trusteeService;
    this.keyCeremonyFacade = keyCeremonyFacade;
    this.hashFacade = hashFacade;
    this.decryptionService = decryptionService;
    this.decryptionFacade = decryptionFacade;
    this.botFacade = botFacade;
  }

  public KeyCeremonyPhaseOneStrategy getKeyCeremonyPhaseOneStrategy() {
    return new KeyCeremonyPhaseOneStrategy(trusteeService);
  }

  public KeyCeremonyPhaseTwoStrategy getKeyCeremonyPhaseTwoStrategy() {
    return new KeyCeremonyPhaseTwoStrategy(trusteeService, botFacade);
  }

  public KeyCeremonyFinishedStrategy getKeyCeremonyFinishedStrategy() {
    return new KeyCeremonyFinishedStrategy(keyCeremonyFacade, trusteeService);
  }

  public OpenElectionStrategy getOpenElectionStrategy() {
    return new OpenElectionStrategy(hashFacade);
  }

  public DecryptionPhaseOneStrategy getDecryptionPhaseOneStrategy() {
    return new DecryptionPhaseOneStrategy(trusteeService,
        decryptionFacade, botFacade);
  }

  public DecryptionPhaseTwoStrategy getDecryptionPhaseTwoStrategy() {
    return new DecryptionPhaseTwoStrategy(trusteeService, decryptionService, botFacade);
  }

  public DoneStrategy getDoneStrategy() {
    return new DoneStrategy(decryptionFacade);
  }

}
