package de.sprechbruecke.voices;

import java.io.*;
import java.security.MessageDigest;
import java.util.function.BooleanSupplier;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;

/** Private staging directory + integrity check + bounded, traversal-safe extraction. */
final class ModelArchive {
    static void checkCancelled(BooleanSupplier cancelled) throws IOException {
        if (cancelled.getAsBoolean()) throw new InterruptedIOException("Download abgebrochen.");
    }
    static String sha256(File file, BooleanSupplier cancelled) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream input = new BufferedInputStream(new FileInputStream(file))) {
            byte[] buffer = new byte[65536]; int n;
            while ((n=input.read(buffer))!=-1) { checkCancelled(cancelled); digest.update(buffer,0,n); }
        }
        StringBuilder hex = new StringBuilder(); for(byte b:digest.digest()) hex.append(String.format(java.util.Locale.ROOT,"%02x",b & 255));
        return hex.toString();
    }
    static void extract(File archive, File stage, String expectedDirectory, BooleanSupplier cancelled) throws IOException {
        if (!stage.mkdirs() && !stage.isDirectory()) throw new IOException("Speicher nicht verfügbar.");
        String root = stage.getCanonicalPath() + File.separator;
        long total=0; int entries=0;
        try (TarArchiveInputStream tar = new TarArchiveInputStream(new BZip2CompressorInputStream(new BufferedInputStream(new FileInputStream(archive))))) {
            TarArchiveEntry entry;
            byte[] buffer=new byte[65536];
            while ((entry=tar.getNextTarEntry())!=null) {
                checkCancelled(cancelled);
                String name=entry.getName();
                if (++entries>10000 || (!name.equals(expectedDirectory+"/") && !name.startsWith(expectedDirectory+"/"))) throw new IOException("Ungültiges Stimmenpaket.");
                File file=new File(stage,name);
                if (!file.getCanonicalPath().startsWith(root) || entry.isSymbolicLink() || entry.isLink() || (!entry.isDirectory() && !entry.isFile())) throw new IOException("Unzulässiger Archivpfad.");
                if (entry.isDirectory()) { if (!file.mkdirs() && !file.isDirectory()) throw new IOException("Ordner nicht anlegbar."); continue; }
                total+=entry.getSize(); if(entry.getSize()<0 || total>600_000_000L) throw new IOException("Stimmenpaket ist zu groß.");
                File parent=file.getParentFile(); if(!parent.mkdirs() && !parent.isDirectory()) throw new IOException("Ordner nicht anlegbar.");
                try(OutputStream output=new BufferedOutputStream(new FileOutputStream(file))) {
                    int n; while((n=tar.read(buffer))!=-1) { checkCancelled(cancelled); output.write(buffer,0,n); }
                }
            }
        }
    }
    static void delete(File path) {
        File[] files=path.listFiles(); if(files!=null) for(File file:files) delete(file);
        if(path.exists() && !path.delete()) path.deleteOnExit();
    }
}
