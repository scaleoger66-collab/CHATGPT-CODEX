package de.sprechbruecke.voices;
import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import java.io.*;
import java.nio.file.Files;
import org.apache.commons.compress.archivers.tar.*;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;
import static org.junit.Assert.*;
public class ModelArchiveTest {
    @Rule public TemporaryFolder temp=new TemporaryFolder();
    private File archive(String path, boolean link) throws Exception {
        File archive=temp.newFile();
        try(TarArchiveOutputStream out=new TarArchiveOutputStream(new BZip2CompressorOutputStream(new FileOutputStream(archive)))) {
            TarArchiveEntry entry=link ? new TarArchiveEntry(path,TarConstants.LF_SYMLINK) : new TarArchiveEntry(path);
            if(link)entry.setLinkName("../../outside"); else entry.setSize(3);
            out.putArchiveEntry(entry); if(!link)out.write(new byte[]{1,2,3}); out.closeArchiveEntry();
        } return archive;
    }
    @Test public void validFilesExtractUnderExpectedDirectory() throws Exception {
        File stage=new File(temp.getRoot(),"stage"); ModelArchive.extract(archive("model/tokens.txt",false),stage,"model",()->false);
        assertArrayEquals(new byte[]{1,2,3},Files.readAllBytes(new File(stage,"model/tokens.txt").toPath()));
    }
    @Test public void traversalAndLinksAreRejected() throws Exception {
        for(String name:new String[]{"model/../../outside","/tmp/outside","other/tokens.txt"}) {
            try { ModelArchive.extract(archive(name,false),temp.newFolder(),"model",()->false); fail("Unsafe path accepted"); } catch(IOException expected) { }
        }
        try { ModelArchive.extract(archive("model/link",true),temp.newFolder(),"model",()->false); fail("Link accepted"); } catch(IOException expected) { }
    }
    @Test public void cancellationStopsExtractionAndHashing() throws Exception {
        File archive=archive("model/test",false);
        try { ModelArchive.extract(archive,temp.newFolder(),"model",()->true); fail(); } catch(InterruptedIOException expected) { }
        try { ModelArchive.sha256(archive,()->true); fail(); } catch(InterruptedIOException expected) { }
    }
    @Test public void checksumDetectsChangedDownload() throws Exception {
        File file=temp.newFile(); Files.write(file.toPath(),new byte[]{97,98,99});
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",ModelArchive.sha256(file,()->false));
        Files.write(file.toPath(),new byte[]{97,98});
        assertNotEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",ModelArchive.sha256(file,()->false));
    }
}
