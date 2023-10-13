package pse.election.backendserver.core.electionguard;

import electioncli.core.App;
import java.io.File;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class VerificationFacadeTestWithCLI {

  @TempDir
  File workplace;

  @Test
  void setup() {
    App.main(new String[]{"aux", "targetdir=workplace", "sourcedir=workplace"});
  }

}
