package electioncli.handle.key_generation;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.sunya.electionguard.*;
import electioncli.core.Commands;
import electioncli.core.ICommand;
import electioncli.core.Identifiers;
import electioncli.handle.CommandHandler;
import electioncli.utils.RSA;

import java.math.BigInteger;
import java.security.*;
import java.util.*;

public class ElectionKeyHandler extends CommandHandler {
    private static final ICommand COMMAND = Commands.ELECTIONKEY;
    private static final String[] REQUIRED_FILE_NAMES = new String[] {"aux_keys"};
    private static final String[] TARGET_FILE_NAMES = new String[]{"ceremony_private", "ceremony_public"};
    private static final String EXIT_MESSAGE = "";

    private int k;
    private Map<Integer, PublicKey> auxKeys;
    private Map<Integer, String> keySort;

    /*
    * Required input Json:
    * */
    @Override
    public void initialize(JsonObject[] input, String[] args) {
        JsonObject auxKeyInput = input[0];
        k = auxKeyInput.get(Identifiers.THRESHOLD.getName()).getAsInt();
        JsonObject keys = auxKeyInput.get(Identifiers.AUX_KEY.getName()).getAsJsonObject();
        auxKeys = new HashMap<>();
        keySort = new HashMap<>();
        for (String trustee : keys.keySet()) {
            String rawKey = keys.get(trustee).getAsJsonObject().get(Identifiers.PUBLIC_KEY.getName()).getAsString();
            auxKeys.put(Integer.valueOf(trustee), RSA.stringToPublicKey(rawKey));
            keySort.put(Integer.valueOf(trustee), keys.get(trustee).getAsJsonObject().get(Identifiers.ENCRYPTION_TYPE.getName()).getAsString());
        }

    }

    @Override
    public JsonObject[] execute() {
        JsonObject publicKey = new JsonObject();
        JsonObject privateKey = new JsonObject();

        List<Group.ElementModP> publicKeys = new ArrayList<>();
        List<Group.ElementModQ> privateKeys = new ArrayList<>();
        JsonArray rawPrivateKeys = new JsonArray();
        JsonArray rawProofs = new JsonArray();
        List<SchnorrProof> proofs = new ArrayList<>();
        for (int i = 0; i < k; i++) {
            Group.ElementModQ rand_q = Group.rand_range_q(Group.TWO_MOD_Q);
            Group.ElementModQ proof_nonce = Group.rand_range_q(Group.TWO_MOD_Q);
            ElGamal.KeyPair pair = ElGamal.elgamal_keypair_from_secret(rand_q).orElseThrow();
            Group.ElementModP rand_p = pair.public_key();
            SchnorrProof proof = SchnorrProof.make_schnorr_proof(pair, proof_nonce);
            privateKeys.add(rand_q);
            publicKeys.add(rand_p);
            rawPrivateKeys.add(rand_q.base16());
            JsonObject rawProof = new JsonObject();
            rawProof.addProperty(Identifiers.PUBLIC_KEY.getName(), proof.publicKey.base16());
            rawProof.addProperty(Identifiers.SCNORR_COMMITMENT.getName(), proof.commitment.base16());
            rawProof.addProperty(Identifiers.PROOF_CHALLENGE.getName(), proof.challenge.base16());
            rawProof.addProperty(Identifiers.PROOF_RESPONSE.getName(), proof.response.base16());
            rawProofs.add(rawProof);
            proofs.add(proof);
        }
        publicKey.add(Identifiers.PROOF.getName(), rawProofs);
        privateKey.add(Identifiers.PRIVATE_KEY.getName(), rawPrivateKeys);
        privateKey.add(Identifiers.PROOF.getName(), rawProofs);

        ElectionPolynomial polynomial = new ElectionPolynomial(privateKeys, publicKeys, proofs);
        JsonObject backupSecrets = new JsonObject();
        JsonObject backupPublics = new JsonObject();
        for (int trustee : auxKeys.keySet()) {
            Group.ElementModQ coord = ElectionPolynomial.compute_polynomial_coordinate(BigInteger.valueOf(trustee), polynomial);
            backupSecrets.addProperty(String.valueOf(trustee), coord.base16());
            String encryption;
            encryption = RSA.encode(auxKeys.get(trustee), coord.getBigInt().toString(16), keySort.get(trustee));
            backupPublics.addProperty(String.valueOf(trustee), encryption);
        }
        privateKey.add(Identifiers.BACKUP_SECRET.getName(), backupSecrets);
        publicKey.add(Identifiers.BACKUP.getName(), backupPublics);
        privateKey.add(Identifiers.BACKUP.getName(), backupPublics);
        JsonObject[] out = {privateKey, publicKey};
        return out;
    }

    @Override
    public boolean matchesCommand(String command) {
        return command.matches(COMMAND.getRegex());
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
