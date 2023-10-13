package pse.election.backendserver.controller.app;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import pse.election.backendserver.controller.ElectionVoterAPI;
import pse.election.backendserver.core.service.BallotService;
import pse.election.backendserver.entity.Ballot;
import pse.election.backendserver.payload.dto.BallotProofDTO;
import pse.election.backendserver.payload.dto.ChaumPedersenProofDTO;
import pse.election.backendserver.payload.dto.ConstantChaumPedersenDTO;
import pse.election.backendserver.payload.dto.DisjunctiveChaumPedersenDTO;
import pse.election.backendserver.payload.dto.EncryptedOptionDTO;
import pse.election.backendserver.payload.request.BallotCommitRequest;
import pse.election.backendserver.payload.request.ChaumPedersenProofRequest;
import pse.election.backendserver.payload.request.ConstantChaumPedersenRequest;
import pse.election.backendserver.payload.request.DisjunctiveChaumPedersenRequest;
import pse.election.backendserver.payload.request.EncryptedOptionRequest;
import pse.election.backendserver.payload.response.BallotSentResponse;
import pse.election.backendserver.payload.response.EmptyResponse;
import pse.election.backendserver.payload.response.ResponseBuilder;
import pse.election.backendserver.payload.response.VoterInformationResponse;

/**
 * This class is used as an api controller and handles voter functionalities. It implements the
 * {@link ElectionVoterAPI} and gets controlled by the {@link FrontController}. This includes ballot
 * submissions and voter information.
 *
 * @version 1.0
 */
@Component
@Qualifier("VoterController")
public class VoterController implements ElectionVoterAPI {

  @Autowired
  private BallotService ballotService;

  @Autowired
  private ResponseBuilder responseBuilder;

  @Override
  public VoterInformationResponse getVoterDetails(Long electionId, String email) {
    return this.responseBuilder.buildVoterInformationResponse(electionId, email);
  }

  @Override
  public BallotSentResponse setVotersBallotSent(Long electionId,
      BallotCommitRequest ballotCommitRequest, String voterEmail) {
    Map<Integer, EncryptedOptionDTO[]> parsedCipherText = parseCipherText(
        ballotCommitRequest.getCipherText());
    Map<Integer, DisjunctiveChaumPedersenDTO[]> parsedIndividualProofs = parseIndividualProofs(
        ballotCommitRequest.getIndividualProofs());
    Map<Integer, ConstantChaumPedersenDTO> parseAccumulatedProofs = parseAccumulatedProofs(
        ballotCommitRequest.getAccumulatedProofs());

    BallotProofDTO ballot = new BallotProofDTO(
        parsedCipherText,
        parsedIndividualProofs,
        parseAccumulatedProofs,
        ballotCommitRequest.getDeviceInformation(),
        ballotCommitRequest.getDate()
    );

    Ballot savedBallot = ballotService.addBallot(ballot, electionId,
        ballotCommitRequest.getBallotId(), voterEmail);
    return this.responseBuilder.buildBallotSentResponse(savedBallot.getBallotId());
  }

  @Override
  public EmptyResponse setVotersBallotSubmitted(Long electionId, String trackingCode,
      String email) {
    Ballot submittedBallot = ballotService.convertSpoiledToSubmitted(trackingCode, electionId,
        email);
    return this.responseBuilder.buildEmptyResponse();
  }

  private Map<Integer, ConstantChaumPedersenDTO> parseAccumulatedProofs(
      Map<String, ConstantChaumPedersenRequest> accumulatedProofs) {
    Map<Integer, ConstantChaumPedersenDTO> parsedAccumulatedProofs = new LinkedHashMap<>();

    for (String proof : accumulatedProofs.keySet()) {
      ChaumPedersenProofDTO chaumPedersenProofDTO = new ChaumPedersenProofDTO(
          accumulatedProofs.get(proof).getPad(),
          accumulatedProofs.get(proof).getData(),
          accumulatedProofs.get(proof).getChallenge(),
          accumulatedProofs.get(proof).getResponse()
      );

      ConstantChaumPedersenDTO constantChaumPedersenDTO = new ConstantChaumPedersenDTO(
          chaumPedersenProofDTO,
          accumulatedProofs.get(proof).getConstant()
      );

      parsedAccumulatedProofs.put(Integer.valueOf(proof), constantChaumPedersenDTO);
    }
    return parsedAccumulatedProofs;
  }

  private Map<Integer, DisjunctiveChaumPedersenDTO[]> parseIndividualProofs(
      Map<String, Collection<DisjunctiveChaumPedersenRequest>> individualProofs) {
    Map<Integer, DisjunctiveChaumPedersenDTO[]> parsedIndividualProofs = new LinkedHashMap<>();

    for (String proof : individualProofs.keySet()) {
      List<DisjunctiveChaumPedersenDTO> parsedDisjunctiveChaumPedersen = parseDisjunctiveChaumPedersonFromRequest(
          individualProofs.get(proof));
      parsedIndividualProofs.put(Integer.valueOf(proof),
          parsedDisjunctiveChaumPedersen.toArray(DisjunctiveChaumPedersenDTO[]::new));
    }
    return parsedIndividualProofs;
  }

  private List<DisjunctiveChaumPedersenDTO> parseDisjunctiveChaumPedersonFromRequest(
      Collection<DisjunctiveChaumPedersenRequest> disjunctiveChaumPedersenRequests) {
    List<DisjunctiveChaumPedersenDTO> parsedDisjunctiveChaumPedersen = new ArrayList<>();

    for (DisjunctiveChaumPedersenRequest disjunctiveChaumPedersenRequest : disjunctiveChaumPedersenRequests) {
      ChaumPedersenProofRequest proof0 = disjunctiveChaumPedersenRequest.getProof0();
      ChaumPedersenProofRequest proof1 = disjunctiveChaumPedersenRequest.getProof1();

      ChaumPedersenProofDTO chaumPedersenProofDTO0 = new ChaumPedersenProofDTO(
          proof0.getPad(),
          proof0.getData(),
          proof0.getChallenge(),
          proof0.getResponse()

      );

      ChaumPedersenProofDTO chaumPedersenProofDTO1 = new ChaumPedersenProofDTO(
          proof1.getPad(),
          proof1.getData(),
          proof1.getChallenge(),
          proof1.getResponse()
      );

      DisjunctiveChaumPedersenDTO disjunctiveChaumPedersenDTO = new DisjunctiveChaumPedersenDTO(
          chaumPedersenProofDTO0,
          chaumPedersenProofDTO1,
          disjunctiveChaumPedersenRequest.getChallenge()
      );

      parsedDisjunctiveChaumPedersen.add(disjunctiveChaumPedersenDTO);
    }
    return parsedDisjunctiveChaumPedersen;
  }

  private Map<Integer, EncryptedOptionDTO[]> parseCipherText(
      Map<String, Collection<EncryptedOptionRequest>> cipherText) {
    Map<Integer, EncryptedOptionDTO[]> parsedCipherText = new LinkedHashMap<>();

    for (String cipher : cipherText.keySet()) {
      List<EncryptedOptionDTO> parsedEncryptedOptionList = new ArrayList<>();

      for (EncryptedOptionRequest encryptedOptionRequest : cipherText.get(cipher)) {
        EncryptedOptionDTO encryptedOptionDTO = new EncryptedOptionDTO(
            encryptedOptionRequest.getPad(),
            encryptedOptionRequest.getData()
        );
        parsedEncryptedOptionList.add(encryptedOptionDTO);
      }

      parsedCipherText.put(Integer.valueOf(cipher),
          parsedEncryptedOptionList.toArray(EncryptedOptionDTO[]::new));
    }
    return parsedCipherText;
  }
}
