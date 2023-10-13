package pse.election.backendserver.core.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import pse.election.backendserver.entity.Election;
import pse.election.backendserver.entity.Tally;
import pse.election.backendserver.repository.ElectionRepository;
import pse.election.backendserver.repository.TallyRepository;

import java.util.List;

@SpringBootTest(args = "--oidcClients=./oidcClients.toml")
public class TallyServiceTest {
    @Autowired
    TallyRepository tallyRepository;
    @Autowired
    TallyService tallyService;
    @Autowired
    ElectionRepository electionRepository;
    long electionId1;
    long electionId2;
    @AfterEach
    void clearDatabase() {
        tallyRepository.deleteAll();
        electionRepository.deleteAll();
    }

    @BeforeEach
    void addElections() {
        electionRepository.save(new Election(null, "1", null, null, false, 0));
        electionRepository.save(new Election(null, "2", null, null, false, 0));
        electionId1 = electionRepository.findByTitle("1").getElectionId();
        electionId2 = electionRepository.findByTitle("2").getElectionId();
    }

    @Test
    void testAddNullTally() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> tallyService.addTally(null));
    }

    @Test
    void testValidTally() {
        Tally tally1 = new Tally(electionId1, 0, 0, "A", "B");
        Tally tally2 = new Tally(electionId1, 1, 0, "C", "D");
        Tally tally3 = new Tally(electionId1, 1, 1, "E", "F");

        Tally tally4 = new Tally(electionId2, 0, 0, "AA", "BB");
        Tally tally5 = new Tally(electionId2, 1, 0, "CC", "DD");
        Tally tally6 = new Tally(electionId2, 1, 1, "EE", "FF");

        tallyService.addTally(tally1);
        tallyService.addTally(tally2);
        tallyService.addTally(tally3);
        tallyService.addTally(tally4);
        tallyService.addTally(tally5);
        tallyService.addTally(tally6);
        deepEqual(List.of(tally1, tally2, tally3), tallyService.getAllTalliesOfElection(electionId1));
        deepEqual(List.of(tally4, tally5, tally6), tallyService.getAllTalliesOfElection(electionId2));
    }

    void deepEqual(List<Tally> expected, List<Tally> actual) {
        for (int i = 0; i < expected.size(); i++) {
            Assertions.assertEquals(expected.get(i).getElectionId(), actual.get(i).getElectionId());
            Assertions.assertEquals(expected.get(i).getContestIndex(), actual.get(i).getContestIndex());
            Assertions.assertEquals(expected.get(i).getOptionIndex(), actual.get(i).getOptionIndex());
            Assertions.assertEquals(expected.get(i).getCiphertextPAD(), actual.get(i).getCiphertextPAD());
            Assertions.assertEquals(expected.get(i).getCiphertextDATA(), actual.get(i).getCiphertextDATA());
        }
    }

    /**
     * It is assumed at some places that the tallies are sorted, at least tallies of the same contest to option index
     * Tallies are currently added into the database in order, but this might not always be so
     */
    @Test
    void testRobustOrderTally() {
        Tally tally1 = new Tally(electionId1, 0, 0, "A", "B");
        Tally tally2 = new Tally(electionId1, 1, 0, "C", "D");
        Tally tally3 = new Tally(electionId1, 1, 1, "E", "F");

        tallyService.addTally(tally1);
        tallyService.addTally(tally3);
        tallyService.addTally(tally2);
        deepEqual(List.of(tally1, tally2, tally3), tallyService.getAllTalliesOfElection(electionId1));
    }

    @Test
    void testSpecificTally() {
        Tally tally1 = new Tally(electionId1, 0, 0, "A", "B");
        Tally tally2 = new Tally(electionId1, 1, 0, "C", "D");
        Tally tally3 = new Tally(electionId1, 1, 1, "E", "F");

        Tally tally4 = new Tally(electionId2, 0, 0, "AA", "BB");
        Tally tally5 = new Tally(electionId2, 1, 0, "CC", "DD");
        Tally tally6 = new Tally(electionId2, 1, 1, "EE", "FF");

        tallyService.addTally(tally1);
        tallyService.addTally(tally2);
        tallyService.addTally(tally3);

        tallyService.addTally(tally4);
        tallyService.addTally(tally5);
        tallyService.addTally(tally6);

        deepEqual(List.of(tally1), List.of(tallyService.getSpecificTally(electionId1, 0, 0)));
        deepEqual(List.of(tally2), List.of(tallyService.getSpecificTally(electionId1, 1, 0)));
        deepEqual(List.of(tally3), List.of(tallyService.getSpecificTally(electionId1, 1, 1)));
        deepEqual(List.of(tally4), List.of(tallyService.getSpecificTally(electionId2, 0, 0)));
        deepEqual(List.of(tally5), List.of(tallyService.getSpecificTally(electionId2, 1, 0)));
        deepEqual(List.of(tally6), List.of(tallyService.getSpecificTally(electionId2, 1, 1)));
    }
}
