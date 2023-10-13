package pse.election.backendserver.integration;


import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import pse.election.backendserver.controller.app.AuthorityController;
import pse.election.backendserver.payload.error.exception.IdentityConflictException;
import pse.election.backendserver.payload.error.exception.InvalidConfigurationException;
import pse.election.backendserver.payload.request.ElectionCreationRequest;
import pse.election.backendserver.payload.response.ElectionCreationResponse;
import pse.election.backendserver.repository.ElectionRepository;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

@SpringBootTest(args = "--oidcClients=./oidcClients.toml")
public class ElectionCreationTest {

    @Autowired
    private AuthorityController authorityController;

    @Autowired
    private ElectionRepository electionRepository;

    @AfterEach
    void reset() {
        electionRepository.deleteAll();
    }

    @Test
    void checkValidRequest() {
        ElectionCreationRequest request = createValidRequest();
        ElectionCreationResponse response = authorityController.createElection("valid.authority@email.address", request);
        Assertions.assertNotNull(response.state());
        Assertions.assertNotNull(response.electionId());
    }

    @Test
    void checkInvalidRequest_ThresholdInvalid() {
        ElectionCreationRequest request = createValidRequest();
        request.getElectionMeta().setThreshold(-1);

        Assertions.assertThrows(InvalidConfigurationException.class,
                () -> authorityController.createElection("valid.authority@email.address", request));
    }

    @Test
    void checkInvalidStateSwitch_IllegalSwitch() {
        ElectionCreationRequest request = createValidRequest();
        ElectionCreationResponse response = authorityController.createElection("valid.authority@email.address", request);

        authorityController.nextState(response.electionId(), "KEYCEREMONY_FINISHED");

        //nothing changed in case of success
        Assertions.assertEquals("AUX_KEYS", electionRepository.findByElectionId(response.electionId()).getState().toString());
    }

    @Test
    void checkInvalidStateSwitch_IllegalArgument() {
        ElectionCreationRequest request = createValidRequest();
        ElectionCreationResponse response = authorityController.createElection("valid.authority@email.address", request);

        Assertions.assertThrows(IllegalArgumentException.class, () -> authorityController.nextState(response.electionId(), null));

    }

    @Test
    void electionWithSameNames() {
        ElectionCreationRequest request = createValidRequest();
        ElectionCreationResponse response = authorityController.createElection("valid.authority@email.address", request);
        Assertions.assertNotNull(response.state());
        Assertions.assertNotNull(response.electionId());

        ElectionCreationRequest request2 = createValidRequest();
        Assertions.assertThrows(IdentityConflictException.class, () -> authorityController.createElection("valid.authority@email.address", request2));
    }

    private ElectionCreationRequest createValidRequest() {
        ElectionCreationRequest electionCreationRequest = new ElectionCreationRequest();

        Map<String, ElectionCreationRequest.Question> questionMap = new LinkedHashMap<>();
        ElectionCreationRequest.Question question =  new ElectionCreationRequest.Question();
        question.setQuestionText("Test");
        question.setOptions(List.of("Option1"));
        question.setMaxSelections(1);
        questionMap.put("Question1", question);
        electionCreationRequest.setQuestions(questionMap);

        ElectionCreationRequest.ElectionCreationBody electionCreationBody = new ElectionCreationRequest.ElectionCreationBody();
        electionCreationBody.setEnd(Date.from(LocalDateTime.now().plusDays(1).atZone(ZoneId.systemDefault()).toInstant()));
        electionCreationBody.setDescription("Description");
        electionCreationBody.setThreshold(1);
        electionCreationBody.setTitle("Title");

        electionCreationRequest.setTrustees(List.of("test@example.test"));
        electionCreationRequest.setVoters(List.of("test@example.test"));
        electionCreationRequest.setElectionMeta(electionCreationBody);

        return electionCreationRequest;
    }




}
