package de.sprechbruecke;
import java.io.*;
import java.nio.file.Files;
import java.util.zip.*;
import org.junit.Test;
import static org.junit.Assert.*;
public class PolishModelTest {
    @Test public void rejectsTraversalAndCancelledExtraction() throws Exception {
        File root = Files.createTempDirectory("polish-model").toFile();
        try {
            File zip = new File(root, "input.zip");
            try (ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zip))) {
                out.putNextEntry(new ZipEntry("../escaped")); out.write(1); out.closeEntry();
            }
            try { PolishModel.extract(zip, new File(root, "stage"), bytes -> {}); fail(); } catch (IOException expected) {}
            assertFalse(new File(root, "escaped").exists());
            try (ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zip))) {
                out.putNextEntry(new ZipEntry("model/file")); out.write(1); out.closeEntry();
            }
            try { PolishModel.extract(zip, new File(root, "stage2"), bytes -> { throw new InterruptedIOException(); }); fail(); } catch (InterruptedIOException expected) {}
            assertFalse(PolishModel.installed(root));
            try { PolishModel.verify(zip); fail(); } catch (IOException expected) {}
        } finally { PolishModel.erase(root); }
    }
}
