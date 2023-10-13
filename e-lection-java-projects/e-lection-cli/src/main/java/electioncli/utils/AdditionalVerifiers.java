package electioncli.utils;

import com.sunya.electionguard.Group;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;


/**
 * This class includes additional verifiers for the application.
 * */
public class AdditionalVerifiers {
    /**
    * Checks if a provieded ElectionPartialKeyBackup is valid.
     *
    * @param publicKeys public commitments to the coefficients
    * @param backup provieded backup key
    * @param id id of the current trustee
    * */
    @SuppressWarnings("checkstyle:Indentation")
    public static boolean verifyEPKB(List<Group.ElementModP> publicKeys, Group.ElementModQ backup, int id) {
        List<Group.ElementModP> mults = new ArrayList<>();
        Group.ElementModP idMod = Group.int_to_p_unchecked(BigInteger.valueOf(id));
        for (int j = 0; j < publicKeys.size(); j++) {
            Group.ElementModP jmod = Group.int_to_p_unchecked(BigInteger.valueOf(j));
            Group.ElementModP exponent = Group.pow_p(idMod, jmod);
            Group.ElementModP publicKey = publicKeys.get(j);
            mults.add(Group.pow_p(publicKey, exponent));
        }
        Group.ElementModP rightPart = Group.mult_p(mults);
        Group.ElementModP leftPart = Group.g_pow_p(backup);
        return rightPart.equals(leftPart);
    }

}
