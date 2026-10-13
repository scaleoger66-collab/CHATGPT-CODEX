package de.sprechbruecke.voices;

import android.app.*;
import android.os.*;
import android.graphics.Insets;
import android.view.*;
import android.widget.*;
import android.speech.tts.*;
import java.util.*;
import java.io.*;

/** Standalone voice manager, also opened through Android's INSTALL_TTS_DATA action. */
public class VoiceActivity extends Activity {
    private PiperModels models;
    private TextToSpeech speech;
    private boolean ready, destroyed;
    private TextView progress;
    private Button[] buttons;
    private Button all, cancel;
    private final PiperModels.Listener listener=this::refresh;
    @Override public void onCreate(Bundle state) {
        super.onCreate(state); models=PiperModels.get(this);
        ScrollView scroll=new ScrollView(this); LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL);
        int padding=Math.round(20*getResources().getDisplayMetrics().density); root.setPadding(padding,padding,padding,padding); scroll.addView(root);
        scroll.setOnApplyWindowInsetsListener((v,insets)->{ Insets bars=insets.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout()); scroll.setPadding(bars.left,bars.top,bars.right,bars.bottom); return insets; });
        TextView title=new TextView(this); title.setText("Deine Offline-Stimmen"); title.setTextSize(24); root.addView(title);
        TextView intro=new TextView(this); intro.setText("Gosia & Darkman für Polnisch. Thorsten High für Deutsch.\nEinmal herunterladen, danach ohne Internet. Alle drei Pakete: ca. 250 MB Download. Während des Downloads bitte diese App geöffnet lassen."); intro.setTextSize(16); root.addView(intro);
        buttons=new Button[PiperVoice.ALL.length];
        for(int i=0;i<buttons.length;i++) { PiperVoice voice=PiperVoice.ALL[i]; buttons[i]=button("",v->details(voice)); root.addView(buttons[i]); }
        progress=new TextView(this); progress.setTextSize(16); progress.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE); root.addView(progress);
        all=button("Alle drei Stimmen herunterladen",v->models.download(PiperVoice.ALL)); root.addView(all);
        cancel=button("Download abbrechen",v->models.cancel()); root.addView(cancel);
        root.addView(button("Hörprobe stoppen",v->{if(speech!=null)speech.stop();}));
        root.addView(button("Lizenzen & Quellen",v->licenses()));
        root.addView(button("Zurück",v->finish()));
        setContentView(scroll); scroll.requestApplyInsets(); refresh();
        speech=new TextToSpeech(this,result->new Handler(Looper.getMainLooper()).post(()->{ if(!destroyed)ready=result==TextToSpeech.SUCCESS; }),getPackageName());
    }
    private Button button(String text,View.OnClickListener listener) { Button b=new Button(this); b.setText(text); b.setAllCaps(false); b.setOnClickListener(listener); return b; }
    private void refresh() {
        if(destroyed)return;
        for(int i=0;i<buttons.length;i++) {
            PiperVoice voice=PiperVoice.ALL[i]; boolean installed=models.installed(voice);
            buttons[i].setText(voice.name+" · "+voice.languageName()+"\n"+(installed ? "Installiert · Hörprobe / Entfernen" : "Herunterladen · "+Math.round(voice.bytes/1_000_000f)+" MB"));
            buttons[i].setEnabled(installed||!models.busy);
        }
        progress.setText(models.message); all.setEnabled(!models.busy); cancel.setVisibility(models.busy?View.VISIBLE:View.GONE);
    }
    private void details(PiperVoice voice) {
        boolean installed=models.installed(voice);
        AlertDialog.Builder dialog=new AlertDialog.Builder(this).setTitle(voice.name).setMessage(installed ? "Offline-Hörprobe abspielen? In Sprechbrücke kannst du die gewünschte polnische Stimme unter Stimme & Hörprobe auswählen." : "Ca. "+Math.round(voice.bytes/1_000_000f)+" MB herunterladen? Das Paket wird anschließend geprüft und offline gespeichert.")
            .setPositiveButton(installed ? "Hörprobe" : "Herunterladen",(d,w)->{if(installed)preview(voice);else models.download(voice);}).setNegativeButton("Abbrechen",null);
        if(installed&&!models.busy)dialog.setNeutralButton("Entfernen",(d,w)->{if(speech!=null)speech.stop();models.remove(voice);});
        dialog.show();
    }
    private void preview(PiperVoice voice) {
        if(!ready) { progress.setText("Sprachausgabe startet noch. Bitte gleich erneut antippen."); return; }
        Set<Voice> voices=speech.getVoices();
        if(voices!=null)for(Voice available:voices)if(available.getName().equals(voice.id)) {
            if(speech.setVoice(available)!=TextToSpeech.SUCCESS)break;
            speech.setSpeechRate(1f);
            String text=voice.language.equals("pl") ? "Cześć! Jak się masz? Miło cię poznać. Czy możemy porozmawiać po polsku?" : "Hallo! Wie geht es dir? Schön, dich kennenzulernen. Können wir auf Deutsch sprechen?";
            if(speech.speak(text,TextToSpeech.QUEUE_FLUSH,null,voice.id)==TextToSpeech.SUCCESS) { progress.setText("Hörprobe · "+voice.name); return; }
        }
        progress.setText("Stimme noch nicht verfügbar. Paket prüfen oder App erneut öffnen.");
    }
    private String asset(String filename) throws IOException {
        try(InputStream input=getAssets().open(filename); ByteArrayOutputStream output=new ByteArrayOutputStream()) { byte[] b=new byte[4096]; int n; while((n=input.read(b))!=-1)output.write(b,0,n);return output.toString("UTF-8"); }
    }
    private void licenses() {
        try {
            TextView text=new TextView(this);text.setText(asset("VOICE_NOTICES.txt")+"\n\n"+asset("GPL-3.0.txt")+"\n\n"+asset("APACHE-2.0.txt")+"\n\n"+asset("ONNX-MIT.txt"));text.setTextIsSelectable(true);text.setPadding(24,16,24,16);
            ScrollView scroll=new ScrollView(this);scroll.addView(text);new AlertDialog.Builder(this).setTitle("Lizenzen & Quellen").setView(scroll).setPositiveButton("Schließen",null).show();
        }catch(IOException e){progress.setText("Quellen und Lizenzen stehen im GitHub-Projekt unter voices/.");}
    }
    @Override protected void onResume(){super.onResume();if(models!=null){models.listen(listener);refresh();}}
    @Override protected void onPause(){if(models!=null)models.unlisten(listener);if(speech!=null)speech.stop();super.onPause();}
    @Override protected void onDestroy(){destroyed=true;if(models!=null)models.unlisten(listener);if(speech!=null)speech.shutdown();super.onDestroy();}
}
