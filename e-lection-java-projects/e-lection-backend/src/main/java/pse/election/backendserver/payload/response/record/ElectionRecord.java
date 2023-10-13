package pse.election.backendserver.payload.response.record;

import java.io.File;
import java.util.zip.ZipOutputStream;

/**
 * This class wraps the election record.
 * */
public record ElectionRecord(ZipOutputStream zipOutputStream, File file) {

}
