package de.sprechbruecke.voices;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;

/** Process-owned downloads survive Activity recreation. No text is sent to a server. */
final class PiperModels {
    interface Listener { void changed(); }
    private static PiperModels instance;
    static synchronized PiperModels get(Context context) {
        if(instance==null) instance=new PiperModels(context.getApplicationContext()); return instance;
    }
    final File root;
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private final Handler main=new Handler(Looper.getMainLooper());
    private final AtomicBoolean cancelled=new AtomicBoolean();
    private volatile Listener listener;
    private volatile HttpURLConnection connection;
    volatile boolean busy;
    volatile String message="";
    private PiperModels(Context context) {
        root=new File(context.getFilesDir(),"piper"); root.mkdirs();
        // A killed download is never treated as installed; discard only its private staging files.
        for(PiperVoice voice:PiperVoice.ALL) { ModelArchive.delete(new File(root,voice.id+".part")); ModelArchive.delete(new File(root,voice.id+".staging")); }
    }
    void listen(Listener listener) { this.listener=listener; }
    void unlisten(Listener listener) { if(this.listener==listener) this.listener=null; }
    private void publish(String value) { message=value; main.post(() -> { Listener l=listener; if(l!=null) l.changed(); }); }
    File directory(PiperVoice voice) { return new File(root,voice.directory()); }
    boolean installed(PiperVoice voice) {
        File folder=directory(voice);
        try { return voice.sha256.equals(new String(Files.readAllBytes(new File(folder,"verified.sha256").toPath()),StandardCharsets.UTF_8).trim()) && validContents(folder,voice); }
        catch(IOException e) { return false; }
    }
    static boolean validContents(File folder, PiperVoice voice) {
        return new File(folder,voice.model+".onnx").length()>1000 && new File(folder,"tokens.txt").length()>0
            && new File(folder,"espeak-ng-data/phontab").length()>0;
    }
    synchronized void download(PiperVoice... voices) {
        if(busy) return;
        busy=true; cancelled.set(false); publish("Download wird vorbereitet … App bitte geöffnet lassen.");
        worker.execute(() -> {
            String result="Stimmen offline bereit.";
            try {
                for(PiperVoice voice:voices) {
                    ModelArchive.checkCancelled(cancelled::get);
                    if(installed(voice)) continue;
                    if(root.getUsableSpace()<voice.bytes*4+50_000_000L) throw new IOException("Zu wenig Speicher. Bitte mindestens 600 MB freimachen.");
                    File archive=new File(root,voice.id+".part");
                    try { fetch(voice,archive); publish(voice.name+" · Paket wird geprüft und installiert …"); installArchive(voice,archive,cancelled::get); }
                    finally { ModelArchive.delete(archive); }
                }
            } catch(InterruptedIOException e) { result="Download abgebrochen. Bereits installierte Stimmen bleiben erhalten."; }
              catch(Exception e) { result=cancelled.get() ? "Download abgebrochen. Bereits installierte Stimmen bleiben erhalten." : "Stimmendownload fehlgeschlagen: "+(e.getMessage()==null ? "Bitte erneut versuchen." : e.getMessage()); }
            finally { connection=null; busy=false; publish(result); }
        });
    }
    void cancel() { cancelled.set(true); HttpURLConnection c=connection; if(c!=null)c.disconnect(); }
    private void fetch(PiperVoice voice, File destination) throws Exception {
        String address=voice.url(); HttpURLConnection c=null;
        try {
            for(int redirects=0;redirects<6;redirects++) {
                ModelArchive.checkCancelled(cancelled::get);
                URL url=new URL(address); if(!url.getProtocol().equals("https")) throw new IOException("Unsicherer Download-Link.");
                c=(HttpURLConnection)url.openConnection(); connection=c; c.setInstanceFollowRedirects(false);
                c.setConnectTimeout(20000); c.setReadTimeout(20000); c.setRequestProperty("User-Agent","Sprechbruecke/0.5");
                int code=c.getResponseCode();
                if(code>=300 && code<400) { String location=c.getHeaderField("Location"); if(location==null)throw new IOException("Download-Weiterleitung fehlt."); address=new URL(url,location).toString(); c.disconnect(); c=null; continue; }
                if(code!=200) throw new IOException("Server meldet HTTP "+code+". Bitte später erneut versuchen.");
                long count=0,last=0;
                try(InputStream input=new BufferedInputStream(c.getInputStream()); OutputStream output=new BufferedOutputStream(new FileOutputStream(destination))) {
                    byte[] buffer=new byte[65536]; int n;
                    while((n=input.read(buffer))!=-1) {
                        ModelArchive.checkCancelled(cancelled::get); count+=n;
                        if(count>voice.bytes) throw new IOException("Unerwartete Downloadgröße.");
                        output.write(buffer,0,n);
                        long now=System.currentTimeMillis(); if(now-last>500) { publish(voice.name+" · "+(count*100/voice.bytes)+" % · App bitte geöffnet lassen"); last=now; }
                    }
                }
                if(count!=voice.bytes)throw new IOException("Download unvollständig. Bitte erneut laden.");
                return;
            }
            throw new IOException("Zu viele Weiterleitungen.");
        } finally { if(c!=null)c.disconnect(); connection=null; }
    }
    // Shared by the real downloader and the offline integration test.
    void installArchive(PiperVoice voice, File archive, BooleanSupplier cancelled) throws Exception {
        File stage=new File(root,voice.id+".staging"); ModelArchive.delete(stage);
        try {
            if(archive.length()!=voice.bytes || !voice.sha256.equals(ModelArchive.sha256(archive,cancelled))) throw new IOException("Prüfsumme stimmt nicht. Bitte erneut herunterladen.");
            ModelArchive.extract(archive,stage,voice.directory(),cancelled);
            File model=new File(stage,voice.directory());
            if(!validContents(model,voice))throw new IOException("Stimmenpaket ist unvollständig.");
            ModelArchive.checkCancelled(cancelled);
            Files.write(new File(model,"verified.sha256").toPath(),voice.sha256.getBytes(StandardCharsets.UTF_8));
            File destination=directory(voice);
            if(destination.exists()) { if(installed(voice)) return; ModelArchive.delete(destination); }
            if(!model.renameTo(destination))throw new IOException("Stimme konnte nicht gespeichert werden.");
        } finally { ModelArchive.delete(stage); }
    }
    synchronized void remove(PiperVoice voice) { if(!busy) { ModelArchive.delete(directory(voice)); publish(voice.name+" entfernt."); } }
}
