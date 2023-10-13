package pse.election.backendserver.controller.app;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import pse.election.backendserver.controller.ElectionTrusteeAPI;
import pse.election.backendserver.core.service.DecryptionService;
import pse.election.backendserver.core.service.TrusteeService;
import pse.election.backendserver.payload.dto.ChaumPedersenProofDTO;
import pse.election.backendserver.payload.dto.DecryptionDTO;
import pse.election.backendserver.payload.dto.ElgamalProofDTO;
import pse.election.backendserver.payload.dto.SchnorrProofDTO;
import pse.election.backendserver.payload.error.exception.UnauthorizedAccessException;
import pse.election.backendserver.payload.request.AuxiliaryKeyRequest;
import pse.election.backendserver.payload.request.ChaumPedersenProofRequest;
import pse.election.backendserver.payload.request.DecryptionRequest;
import pse.election.backendserver.payload.request.ElgamalKeysAndBackupsRequest;
import pse.election.backendserver.payload.request.SchnorrProofRequest;
import pse.election.backendserver.payload.response.ElectionAuxiliaryKeysResponse;
import pse.election.backendserver.payload.response.ElectionEncryptedResultResponse;
import pse.election.backendserver.payload.response.ElgamalKeysAndBackupsResponse;
import pse.election.backendserver.payload.response.EmptyResponse;
import pse.election.backendserver.payload.response.ResponseBuilder;
import pse.election.backendserver.payload.response.TrusteeDecryptionStateResponse;
import pse.election.backendserver.payload.response.TrusteeKeyCeremonyStateResponse;

/**
 * This class is used as an api controller and handles trustee functionalities. It implements the
 * {@link ElectionTrusteeAPI} and is controller by the {@link FrontController}. This includes
 * providing the functionalities for a key ceremony and election decryption.
 *
 * @version 1.0
 */
@Component
@Qualifier("TrusteeController")
public class TrusteeController implements ElectionTrusteeAPI {

  @Autowired
  private TrusteeService trusteeService;

  @Autowired
  private DecryptionService decryptionService;

  @Autowired
  private ResponseBuilder responseBuilder;

  @Override
  public TrusteeKeyCeremonyStateResponse getElectionKeyCeremonyState(Long electionId,
      String email) {
    return this.responseBuilder.buildTrusteeKeyCeremonyStateResponse(electionId, email);
  }

  @Override
  public ElectionAuxiliaryKeysResponse getElectionAuxKeys(Long electionId, String email) {
    return this.responseBuilder.buildElectionAuxiliaryKeysResponse(electionId, email);
  }

  @Override
  public EmptyResponse setTrusteeAuxKey(Long electionId, AuxiliaryKeyRequest auxiliaryKeyRequest,
      String email) {
    trusteeService.addAuxKey(email, auxiliaryKeyRequest.getPublicKey(),
        auxiliaryKeyRequest.getKeyType(), electionId);
    return this.responseBuilder.buildEmptyResponse();
  }

  @Override
  public ElgamalKeysAndBackupsResponse getElgamalKeysAndBackups(Long electionId,
      String trusteeEmail) {
    return this.responseBuilder.buildElgamalKeysAndBackupsResponse(electionId, trusteeEmail);
  }

  @Override
  public EmptyResponse setElgamalKeysAndBackups(Long electionId,
      ElgamalKeysAndBackupsRequest keysAndBackupsRequest, String email) {

    List<SchnorrProofDTO> schnorrProofDTOs = parseSchnorrProofsFromRequests(
        keysAndBackupsRequest.getProofs());
    Map<Integer, String> backups = parseBackupsFromRequest(keysAndBackupsRequest.getBackups());

    ElgamalProofDTO elgamalProofDTO = new ElgamalProofDTO(
        schnorrProofDTOs.toArray(SchnorrProofDTO[]::new),
        backups
    );

    trusteeService.addElgamalKeysAndBackups(elgamalProofDTO, email, electionId);
    return this.responseBuilder.buildEmptyResponse();
  }


  @Override
  public TrusteeDecryptionStateResponse getDecryptionState(Long electionId, String email) {
    return this.responseBuilder.buildTrusteeDecryptionStateResponse(electionId, email);
  }

  @Override
  public ElectionEncryptedResultResponse getEncryptedResult(Long electionId, String email) {
    return this.responseBuilder.buildElectionEncryptedResultResponse(electionId, email);
  }


  @Override
  public EmptyResponse setPartialDecryptionResult(Long electionId,
      DecryptionRequest decryptionRequest, String email) throws UnauthorizedAccessException {

    Map<Integer, DecryptionDTO.PartialDecryptionDTO[]> partialDecryptionSpoiledBallots =
            parseSpoiledBallotDecryptionFromRequest(decryptionRequest.getPartialDecryptedSpoiledBallots());
    Map<Integer, DecryptionDTO.PartialDecryptionDTO> partialDecryptionTalliedBallots = parseTallyDecryptionFromRequest(
        decryptionRequest.getPartialDecryptedTalliedBallots());

    DecryptionDTO decryptionDTO = new DecryptionDTO(partialDecryptionSpoiledBallots,
        partialDecryptionTalliedBallots);

    decryptionService.addDecryption(decryptionDTO, electionId, email);
    return this.responseBuilder.buildEmptyResponse();
  }

  private Map<Integer, DecryptionDTO.PartialDecryptionDTO[]> parseSpoiledBallotDecryptionFromRequest(
      Map<Integer,
          DecryptionRequest.PartialSpoiledBallotDecryptionRequest[]> partialDecryptedSpoiledBallots) {

    Map<Integer, DecryptionDTO.PartialDecryptionDTO[]> decryptedBallotsDTO = new LinkedHashMap<>();

    for (Integer trusteeIndex : partialDecryptedSpoiledBallots.keySet()) {
      List<DecryptionDTO.PartialDecryptionDTO> partialDecryptionDTOS = parsePartialDecryptionFromRequest(
          partialDecryptedSpoiledBallots, trusteeIndex);

      decryptedBallotsDTO.put(trusteeIndex,
          partialDecryptionDTOS.toArray(DecryptionDTO.PartialDecryptionDTO[]::new));
    }

    return decryptedBallotsDTO;
  }

  private List<DecryptionDTO.PartialDecryptionDTO> parsePartialDecryptionFromRequest(
      Map<Integer, DecryptionRequest.PartialSpoiledBallotDecryptionRequest[]> partialDecryptedSpoiledBallots,
      Integer trusteeIndex) {
    List<DecryptionDTO.PartialDecryptionDTO> partialDecryptionDTOS = new ArrayList<>();

    for (DecryptionRequest.PartialSpoiledBallotDecryptionRequest partialSpoiledBallotDecryptionRequest :
            partialDecryptedSpoiledBallots.get(trusteeIndex)) {
      Map<Integer, ChaumPedersenProofDTO[]> chaumPedersenProofDTOS = parseChaumPedersonDTOsFromRequest(
          partialSpoiledBallotDecryptionRequest.getChaumPedersonProofs());

      DecryptionDTO.PartialDecryptionDTO partialDecryptionDTO = new DecryptionDTO.PartialDecryptionDTO(
          partialSpoiledBallotDecryptionRequest.getPartialDecryptedOptions(),
          chaumPedersenProofDTOS,
          partialSpoiledBallotDecryptionRequest.getBallotId()
      );
      partialDecryptionDTOS.add(partialDecryptionDTO);
    }

    return partialDecryptionDTOS;
  }

  private Map<Integer, DecryptionDTO.PartialDecryptionDTO> parseTallyDecryptionFromRequest(
      Map<Integer,
          DecryptionRequest.PartialTallyDecryptionRequest> partialTallyDecryptionRequestMap) {
    Map<Integer, DecryptionDTO.PartialDecryptionDTO> decryptedBallotsDTO = new LinkedHashMap<>();

    for (Integer trusteeIndex : partialTallyDecryptionRequestMap.keySet()) {
      DecryptionRequest.PartialTallyDecryptionRequest partialSpoiledBallotDecryptionRequest =
              partialTallyDecryptionRequestMap.get(trusteeIndex);
      Map<Integer, ChaumPedersenProofDTO[]> chaumPedersenProofDTOS = parseChaumPedersonDTOsFromRequest(
          partialSpoiledBallotDecryptionRequest.getChaumPedersonProofs());

      DecryptionDTO.PartialDecryptionDTO partialDecryptionDTO = new DecryptionDTO.PartialDecryptionDTO(
          partialSpoiledBallotDecryptionRequest.getPartialDecryptedOptions(),
          chaumPedersenProofDTOS,
          -1
      );
      decryptedBallotsDTO.put(trusteeIndex, partialDecryptionDTO);
    }

    return decryptedBallotsDTO;
  }

  private Map<Integer, ChaumPedersenProofDTO[]> parseChaumPedersonDTOsFromRequest(
      Map<Integer, ChaumPedersenProofRequest[]> chaumPedersonProofs) {

    Map<Integer, ChaumPedersenProofDTO[]> parsedFromRequest = new LinkedHashMap<>();

    for (Integer index : chaumPedersonProofs.keySet()) {
      ChaumPedersenProofRequest[] request = chaumPedersonProofs.get(index);
      List<ChaumPedersenProofDTO> chaumPedersenProofDTOList = new ArrayList<>();

      for (ChaumPedersenProofRequest chaumPedersenProofRequest : request) {
        chaumPedersenProofDTOList.add(
            new ChaumPedersenProofDTO(
                chaumPedersenProofRequest.getPad(),
                chaumPedersenProofRequest.getData(),
                chaumPedersenProofRequest.getChallenge(),
                chaumPedersenProofRequest.getResponse()
            )
        );
      }
      parsedFromRequest.put(index, chaumPedersenProofDTOList.toArray(ChaumPedersenProofDTO[]::new));
    }

    return parsedFromRequest;
  }


  private Map<Integer, String> parseBackupsFromRequest(Map<String, String> backups) {
    Map<Integer, String> backupsMap = new LinkedHashMap<>();
    for (String trusteeIndex : backups.keySet()) {
      backupsMap.put(Integer.parseInt(trusteeIndex), backups.get(trusteeIndex));
    }

    return backupsMap;
  }

  private List<SchnorrProofDTO> parseSchnorrProofsFromRequests(
      Collection<SchnorrProofRequest> proofs) {
    return proofs.stream().map(schnorrProofRequest -> new SchnorrProofDTO(
        schnorrProofRequest.getPublicKey(),
        schnorrProofRequest.getCommitment(),
        schnorrProofRequest.getChallenge(),
        schnorrProofRequest.getResponse())
    ).collect(Collectors.toList());
  }
}
