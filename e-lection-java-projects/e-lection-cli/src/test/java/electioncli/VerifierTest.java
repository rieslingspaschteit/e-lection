package electioncli;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.sunya.electionguard.ElGamal;
import com.sunya.electionguard.ElectionPolynomial;
import com.sunya.electionguard.Group;
import com.sunya.electionguard.SchnorrProof;
import electioncli.core.Identifiers;
import electioncli.handle.CommandHandler;
import electioncli.handle.key_generation.BackupHandler;
import electioncli.utils.RSA;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class VerifierTest {
    private static JsonObject backup;
    private static JsonObject key;
    @BeforeEach
    public void initialize() throws NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, InvalidKeySpecException, BadPaddingException, InvalidKeyException {
        backup = new JsonObject();
        key = new JsonObject();
        int n = 5;
        int id = 1;
        int k = 3;
        JsonObject publics = new JsonObject();
        JsonObject backups = new JsonObject();
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");;
        KeyPair keyPair = generator.generateKeyPair();

        key.addProperty(Identifiers.PRIVATE_KEY.getName(), RSA.keyToString(keyPair.getPrivate()));
        key.addProperty(Identifiers.ENCRYPTION_TYPE.getName(), "RSA");
        for (int j = 0; j < n; j++) {
            if (j + 1 != id) {
                List<Group.ElementModP> publicKeyElems = new ArrayList<>();
                List<Group.ElementModQ> privateKeyElems = new ArrayList<>();
                List<SchnorrProof> proofElems = new ArrayList<>();
                JsonArray publicKeys = new JsonArray();
                for (int i = 0; i < k; i++) {
                    Group.ElementModQ rand_q = Group.rand_range_q(Group.TWO_MOD_Q);
                    ElGamal.KeyPair pair = ElGamal.elgamal_keypair_from_secret(rand_q).orElseThrow();
                    SchnorrProof proof = SchnorrProof.make_schnorr_proof(pair, rand_q);
                    publicKeyElems.add(pair.public_key());
                    privateKeyElems.add(pair.secret_key());
                    publicKeys.add(pair.public_key().base16());
                    proofElems.add(proof);
                }

                ElectionPolynomial polynomial = new ElectionPolynomial(privateKeyElems, publicKeyElems, proofElems);
                Group.ElementModQ coord = ElectionPolynomial.compute_polynomial_coordinate(BigInteger.valueOf(id), polynomial);
                String encryption = RSA.encode(keyPair.getPublic(), coord.getBigInt().toString(16), "RSA");
                publics.add(String.valueOf(j + 1), publicKeys);
                backups.addProperty(String.valueOf(j + 1), encryption);
            }
            backup.add(Identifiers.BACKUP.getName(), backups);
            backup.add(Identifiers.PUBLIC_KEYS.getName(), publics);
            backup.addProperty(Identifiers.ID.getName(), id);
        }
    }

    @Test
    public void vaildTest() {
        backup.addProperty(Identifiers.ID.getName(), 1);
        CommandHandler handler = new BackupHandler();
        handler.initialize(new JsonObject[] {backup, key}, new String[0]);
        handler.execute();
        System.out.println(handler.message());
        assertFalse(handler.message().contains("incorrect"));
    }

    @Test
    public void invalidTest() {
        backup.addProperty(Identifiers.ID.getName(), 2);
        CommandHandler handler = new BackupHandler();
        handler.initialize(new JsonObject[] {backup, key}, new String[0]);
        handler.execute();
        assertEquals(handler.message(), "Backups are incorrect" + System.lineSeparator() + "2,3,4,5,");
    }
}
