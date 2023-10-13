package pse.election.backendserver.responseUpdate.record;

import com.sunya.electionguard.Group;
import com.sunya.electionguard.Hash;
import com.sunya.electionguard.Rsa;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.Test;

public class Playground {

  @Test
  void test() throws NoSuchAlgorithmException {
    Group.ElementModQ p = Group.hex_to_q_unchecked("ABC");
    System.out.println(p.base16());
    KeyPairGenerator generator;
    generator = KeyPairGenerator.getInstance("RSA");
    KeyPair keys = generator.generateKeyPair();

    String x = Rsa.encrypt(Group.rand_q().base16(), keys.getPublic()).orElseThrow();
    System.out.println("penciu@online".matches(
        "\"^[a-zA-Z0-9]+(?:\\\\.[a-zA-Z0-9]+)*@[a-zA-Z0-9]+(?:\\\\.[a-zA-Z0-9]+)*$\""));
    System.out.println(Rsa.decrypt(x, keys.getPrivate()));
  }

  @Test
  void listHashTest() {
    String a1 = "AA6659AB27ADAB424711C1012158554B39FE6612F5CA3FAE1647588610B482A9";
    String a2 = "E60D4C4E3E6D64FA77CD98535CBD295C37A94B17DBC947448F7FBD1DE9818E97";
    String a3 = "5D860F83D3B3470525C9B195265A873E64E163D60C803CF5201BF09C765B3985";
    String a4 = "54EE343D5C3F543FAFB2AA8160C352BD7192CF0DDBA22055ABC8D788B3E7E7A7";
    String c = "F806F9AE4172D227E13536388EDDD9E87650AB9F871AFD01084EE0ED25E8891A";
    Timestamp now = Timestamp.from(Instant.now().truncatedTo(ChronoUnit.SECONDS));
    Date nowDate = Date.from(now.toInstant());
    System.out.println(now.toString());
    System.out.println(nowDate.toString());
    System.out.println(now.toInstant().toString());
  }

  @Test
  void testHash() {
    List<Integer> test = List.of(1, 2);
    int[] test2 = new int[]{1, 2};
    System.out.println(Hash.hash_elems(test.toArray()));
    System.out.println(Hash.hash_elems(1, 2));
  }
}
