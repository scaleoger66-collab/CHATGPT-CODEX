package de.sprechbruecke;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.util.zip.*;

/** A verified, bounded model install. A cancelled download never becomes an installed model. */
final class PolishModel {
    static final String NAME = "vosk-model-small-pl-0.22";
    static final String URL = "https://alphacephei.com/vosk/models/" + NAME + ".zip";
    static final String SHA256 = "c4cd16498ea544f446f9e9a55cbd602b71cfe5a2b6f2b0834d81e1b6fce15f0d";
    interface Progress { void update(long bytes) throws IOException; }
    static File directory(File files) { return new File(files, NAME); }
    static boolean installed(File files) { return new File(directory(files), ".verified").isFile(); }
    static void install(File files, File cache, Progress progress) throws Exception {
        File zip = new File(cache, NAME + ".part");
        File staging = new File(files, NAME + ".staging");
        HttpURLConnection connection = (HttpURLConnection)new URL(URL).openConnection();
        connection.setConnectTimeout(15000); connection.setReadTimeout(15000);
        try {
            if (connection.getResponseCode() != 200) throw new IOException("Download nicht erreichbar");
            try (InputStream in = connection.getInputStream(); OutputStream out = new FileOutputStream(zip)) {
                byte[] b = new byte[32768]; long total = 0; int n;
                while ((n = in.read(b)) != -1) {
                    total += n; if (total > 65000000) throw new IOException("Download zu groß");
                    progress.update(total); out.write(b, 0, n);
                }
            }
            verify(zip);
            erase(staging);
            extract(zip, staging, progress);
            File root = new File(staging, NAME);
            for (String required : new String[]{"am/final.mdl", "conf/model.conf", "graph/HCLr.fst", "graph/Gr.fst"})
                if (!new File(root, required).isFile()) throw new IOException("Modell unvollständig");
            progress.update(zip.length());
            try (OutputStream marker = new FileOutputStream(new File(root, ".verified"))) { marker.write(SHA256.getBytes(java.nio.charset.StandardCharsets.US_ASCII)); }
            File destination = directory(files);
            if (installed(files)) return;
            erase(destination);
            if (!root.renameTo(destination)) throw new IOException("Modell konnte nicht gespeichert werden");
        } finally { connection.disconnect(); zip.delete(); erase(staging); }
    }
    static void verify(File file) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream in = new FileInputStream(file)) {
            byte[] b = new byte[32768]; int n;
            while ((n = in.read(b)) != -1) digest.update(b, 0, n);
        }
        StringBuilder hex = new StringBuilder();
        for (byte b : digest.digest()) hex.append(String.format(java.util.Locale.ROOT, "%02x", b & 255));
        if (!SHA256.contentEquals(hex)) throw new IOException("Prüfsumme stimmt nicht. Bitte erneut laden.");
    }
    static void extract(File zip, File staging, Progress progress) throws IOException {
        if (!staging.mkdirs() && !staging.isDirectory()) throw new IOException("Speicher nicht verfügbar");
        String prefix = staging.getCanonicalPath() + File.separator;
        long total = 0; int entries = 0;
        try (ZipInputStream in = new ZipInputStream(new FileInputStream(zip))) {
            ZipEntry entry; byte[] buffer = new byte[32768];
            while ((entry = in.getNextEntry()) != null) {
                if (++entries > 1000) throw new IOException("Zu viele Dateien");
                File target = new File(staging, entry.getName());
                if (!target.getCanonicalPath().startsWith(prefix)) throw new IOException("Ungültiger Dateipfad");
                if (entry.isDirectory()) { if (!target.mkdirs() && !target.isDirectory()) throw new IOException("Speicherfehler"); continue; }
                File parent = target.getParentFile();
                if (!parent.mkdirs() && !parent.isDirectory()) throw new IOException("Speicherfehler");
                try (OutputStream out = new FileOutputStream(target)) {
                    int n;
                    while ((n = in.read(buffer)) != -1) {
                        total += n; if (total > 250000000) throw new IOException("Entpacktes Modell zu groß");
                        progress.update(zip.length()); out.write(buffer, 0, n);
                    }
                }
            }
        }
    }
    static void erase(File file) throws IOException {
        File[] children = file.listFiles();
        if (children != null) for (File child : children) erase(child);
        if (file.exists() && !file.delete()) throw new IOException("Datei konnte nicht entfernt werden");
    }
}
