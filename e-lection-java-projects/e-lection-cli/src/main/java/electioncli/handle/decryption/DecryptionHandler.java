package electioncli.handle.decryption;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.sunya.electionguard.ChaumPedersen;
import com.sunya.electionguard.ElGamal;
import com.sunya.electionguard.Group;
import electioncli.core.Commands;
import electioncli.core.Identifiers;
import electioncli.handle.CommandHandler;


import java.util.*;

public class DecryptionHandler extends CommandHandler {
    private static final Commands COMMAND = Commands.DECRYPTION;
    private static final String[] REQUIRED_FILE_NAMES = new String[] {"encrypted_tallies", "ceremony_private"};
    private static final String[] TARGET_FILE_NAMES = new String[]{"partial_decryption"};
    private static final String EXIT_MESSAGE = "";
    private static final String RSA = "RSA";
    private static final String MISSING_ARGUMENTS = "Error, no input provided";

    protected List<Map<Integer, List<ElGamal.Ciphertext>>> spoiledBallots;
    protected Map<Integer, List<ElGamal.Ciphertext>> tally;
    private Group.ElementModQ privateKey;
    protected Group.ElementModQ baseHash;
    protected List<String> ballotId = new ArrayList<>();

    protected void parseInput(JsonObject[] input) {
        JsonObject rawSpoiledBallots = input[0].get(Identifiers.ENCRYPTED_SPOILED_BALLOT.getName()).getAsJsonObject();
        JsonObject rawTally = input[0].get(Identifiers.ENCRYPTED_TALLY.getName()).getAsJsonObject();
        spoiledBallots = new ArrayList<>();
        for (String ballotIdString : rawSpoiledBallots.keySet()) {
            Map<Integer, List<ElGamal.Ciphertext>> spoiledBallot = new HashMap<>();
            JsonObject rawSpoiledBallot = rawSpoiledBallots.get(ballotIdString).getAsJsonObject();
            for (String questionIndex : rawSpoiledBallot.keySet()) {
                List<ElGamal.Ciphertext> question = new ArrayList<>();
                JsonArray rawSelections = rawSpoiledBallot.get(questionIndex).getAsJsonArray();
                for (JsonElement rawSelectionValue : rawSelections) {
                    JsonObject rawSelection = rawSelectionValue.getAsJsonObject();
                    Group.ElementModP pad = Group.hex_to_p_unchecked(rawSelection.get(Identifiers.PAD.getName()).getAsString());
                    Group.ElementModP data = Group.hex_to_p_unchecked(rawSelection.get(Identifiers.DATA.getName()).getAsString());
                    question.add(new ElGamal.Ciphertext(pad, data));
                }
                spoiledBallot.put(Integer.valueOf(questionIndex), question);
            }
            spoiledBallots.add(spoiledBallot);
            ballotId.add(ballotIdString);
        }
        tally = new HashMap<>();
        for (String questionIndex : rawTally.keySet()) {
            List<ElGamal.Ciphertext> question = new ArrayList<>();
            JsonArray rawQuestion = rawTally.get(questionIndex).getAsJsonArray();
            for (JsonElement rawSelectionValue : rawQuestion) {
                JsonObject rawSelection = rawSelectionValue.getAsJsonObject();
                Group.ElementModP pad = Group.hex_to_p_unchecked(rawSelection.get(Identifiers.PAD.getName()).getAsString());
                Group.ElementModP data = Group.hex_to_p_unchecked(rawSelection.get(Identifiers.DATA.getName()).getAsString());
                question.add(new ElGamal.Ciphertext(pad, data));
            }
            tally.put(Integer.valueOf(questionIndex), question);
        }
        baseHash = Group.hex_to_q(input[0].get(Identifiers.BASE_HASH.getName()).getAsString()).orElseThrow();
    }

    @Override
    public void initialize(JsonObject[] input, String[] args) {
        if (input.length < 2) {
            throw new IllegalArgumentException();
        }
        parseInput(input);
        JsonArray privateKeys = input[1].get(Identifiers.PRIVATE_KEY.getName()).getAsJsonArray();
        privateKey = Group.hex_to_q(privateKeys.get(0).getAsString()).orElseThrow();
    }

    protected JsonObject decryptTally(Map<Integer, List<ElGamal.Ciphertext>> tally, Group.ElementModQ key, Integer count) {
        JsonObject tallyDecryptions = new JsonObject();
        JsonObject rawProofs = new JsonObject();
        //Group.ElementModQ seed = Group.hex_to_q_unchecked("CC711D49CD19A861D7E359FA8CB09B75DC4114693795FA2B28817D64B3D2EA19");
        for (int questionIndex : tally.keySet()) {
            JsonArray questionDecryption = new JsonArray();
            JsonArray questionProof = new JsonArray();
            for (ElGamal.Ciphertext selection : tally.get(questionIndex)) {
                Group.ElementModP decryption = selection.partial_decrypt(key);
                questionDecryption.add(decryption.base16());
                ChaumPedersen.ChaumPedersenProof proof = ChaumPedersen.make_chaum_pedersen(
                        selection,
                        key,
                        decryption,
                        Group.rand_q(),
                        baseHash
                );
                JsonObject rawProof = new JsonObject();
                rawProof.addProperty(Identifiers.PAD.getName(), proof.pad.base16());
                rawProof.addProperty(Identifiers.DATA.getName(), proof.data.base16());
                rawProof.addProperty(Identifiers.PROOF_CHALLENGE.getName(), proof.challenge.base16());
                rawProof.addProperty(Identifiers.PROOF_RESPONSE.getName(), proof.response.base16());
                questionProof.add(rawProof);
            }
            tallyDecryptions.add(String.valueOf(questionIndex), questionDecryption);
            rawProofs.add(String.valueOf(questionIndex), questionProof);
        }
        JsonObject ballot = new JsonObject();
        ballot.add(Identifiers.DECRYPTION.getName(), tallyDecryptions);
        ballot.add(Identifiers.PROOF.getName(), rawProofs);
        if (count != null) ballot.addProperty(Identifiers.BALLOT_ID.getName(), ballotId.get(count));
        return ballot;
    }

    @Override
    public JsonObject[] execute() {
        JsonArray spoiledBallotDecryptions = new JsonArray();
        int count = 0;
        for (Map<Integer, List<ElGamal.Ciphertext>> spoiledBallot : spoiledBallots) {
            spoiledBallotDecryptions.add(decryptTally(spoiledBallot, privateKey, count++));
        }
        JsonObject allSpoiledBallots = new JsonObject();
        allSpoiledBallots.add("0", spoiledBallotDecryptions);
        JsonObject allTallyDecryptions = new JsonObject();
        allTallyDecryptions.add("0", decryptTally(tally, privateKey, null));
        JsonObject decryption = new JsonObject();
        decryption.add(Identifiers.DECRYPTED_SPOILED_BALLOT.getName(), allSpoiledBallots);
        decryption.add(Identifiers.DECRYPTED_TALLY.getName(), allTallyDecryptions);
        JsonObject[] out = {decryption};
        return out;
    }

    @Override
    public boolean matchesCommand(String command) {
        return command.matches(this.COMMAND.getRegex());
    }

    @Override
    public String[] getRequiredFiles() {
        return REQUIRED_FILE_NAMES;
    }

    @Override
    public String[] getTargetFiles() {
        return TARGET_FILE_NAMES;
    }

    @Override
    public String message() {
        return EXIT_MESSAGE;
    }
}
