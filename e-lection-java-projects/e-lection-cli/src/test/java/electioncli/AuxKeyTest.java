package electioncli;

import com.google.gson.JsonObject;
import electioncli.core.Identifiers;
import electioncli.handle.key_generation.AuxKeyHandler;
import electioncli.utils.RSA;
import org.junit.jupiter.api.Test;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.security.*;
import java.security.spec.InvalidKeySpecException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;


public class AuxKeyTest {
    @Test
    public void testRsaAux() throws NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, InvalidKeySpecException, BadPaddingException, InvalidKeyException {
        String message = "Hello world";
        String[] args = new String[] {"aux", "encryption=RSA"};
        AuxKeyHandler handler = new AuxKeyHandler();
        handler.initialize(null, args);
        JsonObject[] output = handler.execute();
        assertEquals(2, output.length);
        assertEquals("RSA", output[0].get(Identifiers.ENCRYPTION_TYPE.getName()).getAsString());
        PublicKey publicKey1 = RSA.stringToPublicKey(output[0].get(Identifiers.PUBLIC_KEY.getName()).getAsString());
        PublicKey publicKey2 = RSA.stringToPublicKey(output[1].get(Identifiers.PUBLIC_KEY.getName()).getAsString());
        PrivateKey privateKey = RSA.stringToPrivateKey(output[0].get(Identifiers.PRIVATE_KEY.getName()).getAsString());
        String cipher1 = RSA.encode(publicKey1, message, "RSA");
        String cipher2 = RSA.encode(publicKey1, message, "RSA");
        assertNotSame(message, cipher1);
        assertEquals(message, RSA.decode(privateKey, cipher1, "RSA"));
        assertEquals(message, RSA.decode(privateKey, cipher2, "RSA"));
    }



}
