package electioncli;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.sunya.electionguard.Group;
import electioncli.core.Identifiers;
import electioncli.handle.CommandHandler;
import electioncli.handle.key_generation.ElectionKeyHandler;
import electioncli.utils.AdditionalVerifiers;
import electioncli.utils.RSA;
import org.junit.jupiter.api.Test;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ElectionKeyTest {
    @Test
    public void testRSAPrivate() throws NoSuchAlgorithmException, InvalidKeySpecException {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        KeyPair keys = generator.generateKeyPair();
        PrivateKey prkey = keys.getPrivate();
        assertEquals(prkey, RSA.stringToPrivateKey(RSA.keyToString(prkey)));

    }

    @Test
    public void testKeyHandler() throws NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, InvalidKeySpecException, BadPaddingException, InvalidKeyException {
        int n = 5; //TODO: Maybe make this parametrized?
        int k = 2;
        int num = 1;

        String func = Identifiers.DEFAULT_TYPE.getName();    //Maybe make this parametrized as well if we ever care to support more than one algorithm
        JsonObject auxKeys = new JsonObject();
        Map<Integer, String> privates = new HashMap<>();
        for (int i = 0; i <= n; i++) {
            JsonObject auxKey = new JsonObject();
            if (i != num) {
                KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
                KeyPair keys = generator.generateKeyPair();
                JsonObject rawKeyPair = new JsonObject();
                rawKeyPair.addProperty(Identifiers.PRIVATE_KEY.getName(), RSA.keyToString(keys.getPrivate()));
                rawKeyPair.addProperty(Identifiers.PUBLIC_KEY.getName(), RSA.keyToString(keys.getPublic()));
                //pairs.add(String.valueOf(i), rawKeyPair);
                privates.put(i + 1, RSA.keyToString(keys.getPrivate()));
                auxKey.addProperty(Identifiers.PUBLIC_KEY.getName(), RSA.keyToString(keys.getPublic()));
                auxKey.addProperty(Identifiers.ENCRYPTION_TYPE.getName(), "RSA");
                auxKeys.add(String.valueOf(i + 1), auxKey);
            }
        }
        JsonObject input = new JsonObject();
        input.add(Identifiers.AUX_KEY.getName(), auxKeys);
        input.addProperty(Identifiers.THRESHOLD.getName(), k);
        String[] args = {};
        CommandHandler handler = new ElectionKeyHandler();
        handler.initialize(new JsonObject[] {input}, args);
        JsonObject[] results = handler.execute();
        assertEquals(2, results.length);
        JsonArray rawPublicKeys = results[1].get(Identifiers.PROOF.getName()).getAsJsonArray();
        List<Group.ElementModP> publicKeys = new ArrayList<>();
        for (JsonElement proof : rawPublicKeys) {
            publicKeys.add(Group.hex_to_p_unchecked(proof.getAsJsonObject().get(Identifiers.PUBLIC_KEY.getName()).getAsString()));
        }
        JsonObject encBackups = results[1].get(Identifiers.BACKUP.getName()).getAsJsonObject();
        for (int i = 1; i <= n; i++) {
            if (i != num) {
                PrivateKey key = RSA.stringToPrivateKey(privates.get(i + 1));
                String pointString = RSA.decode(key, encBackups.get(String.valueOf(i + 1)).getAsString(), "RSA");
                assertTrue(AdditionalVerifiers.verifyEPKB(publicKeys, Group.hex_to_q_unchecked(pointString), i + 1));
            }
        }
        //TODO: learn how to use decryption mediator to verify the partial decryptions actually work?

    }
}
