package de.sprechbruecke.voices;

import android.media.AudioFormat;
import android.speech.tts.*;
import com.k2fsa.sherpa.onnx.OfflineTts;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

/** Standard Android TTS service: independent of any translation application or SDK. */
public class PiperTtsService extends TextToSpeechService {
    private PiperModels models;
    private final AtomicLong epoch=new AtomicLong();
    private final Object engineLock=new Object();
    private OfflineTts engine;
    private String loaded="";
    @Override public void onCreate() { super.onCreate(); models=PiperModels.get(this); }
    private String language(String value) {
        if("deu".equals(value)||"ger".equals(value)||"de".equals(value))return "de";
        if("pol".equals(value)||"pl".equals(value))return "pl";
        return "";
    }
    @Override protected int onIsLanguageAvailable(String lang,String country,String variant) {
        String normalized=language(lang); if(normalized.isEmpty())return TextToSpeech.LANG_NOT_SUPPORTED;
        for(PiperVoice voice:PiperVoice.ALL) if(voice.language.equals(normalized)&&models.installed(voice))return TextToSpeech.LANG_AVAILABLE;
        return TextToSpeech.LANG_MISSING_DATA;
    }
    @Override protected int onLoadLanguage(String lang,String country,String variant) { return onIsLanguageAvailable(lang,country,variant); }
    @Override protected String[] onGetLanguage() { return new String[]{"deu","DEU",""}; }
    @Override public List<Voice> onGetVoices() {
        List<Voice> voices=new ArrayList<>();
        for(PiperVoice voice:PiperVoice.ALL) if(models.installed(voice)) voices.add(new Voice(voice.id,Locale.forLanguageTag(voice.language.equals("de") ? "de-DE" : "pl-PL"),Voice.QUALITY_HIGH,Voice.LATENCY_NORMAL,false,Collections.emptySet()));
        return voices;
    }
    @Override public int onIsValidVoiceName(String name) { for(PiperVoice voice:PiperVoice.ALL)if(voice.id.equals(name)&&models.installed(voice))return TextToSpeech.SUCCESS; return TextToSpeech.ERROR; }
    @Override public int onLoadVoice(String name) { return onIsValidVoiceName(name); }
    @Override public String onGetDefaultVoiceNameFor(String lang,String country,String variant) {
        String normalized=language(lang); if(normalized.isEmpty())return null;
        PiperVoice chosen=PiperVoice.forLanguage(normalized,getSharedPreferences("voices",MODE_PRIVATE).getString("pl","gosia"));
        if(models.installed(chosen))return chosen.id;
        for(PiperVoice voice:PiperVoice.ALL)if(voice.language.equals(normalized)&&models.installed(voice))return voice.id;
        return null;
    }
    @Override protected void onStop() { epoch.incrementAndGet(); }
    @Override protected void onSynthesizeText(SynthesisRequest request,SynthesisCallback callback) {
        long token=epoch.incrementAndGet();
        String name=request.getVoiceName(); String lang=language(request.getLanguage());
        if(name==null || name.isEmpty())name=onGetDefaultVoiceNameFor(request.getLanguage(),request.getCountry(),request.getVariant());
        PiperVoice selected=null;
        for(PiperVoice voice:PiperVoice.ALL) if(voice.id.equals(name)&&voice.language.equals(lang)&&models.installed(voice))selected=voice;
        if(selected==null) { callback.error(TextToSpeech.ERROR_NOT_INSTALLED_YET); return; }
        String text=request.getCharSequenceText().toString();
        if(text.length()>5000) { callback.error(TextToSpeech.ERROR_INVALID_REQUEST); return; }
        synchronized(engineLock) {
            try {
                if(!loaded.equals(selected.id)||engine==null) { if(engine!=null) { engine.release(); engine=null; } engine=PiperEngine.create(models.directory(selected),selected); loaded=selected.id; }
                if(epoch.get()!=token) { callback.error(); return; }
                if(callback.start(engine.sampleRate(),AudioFormat.ENCODING_PCM_16BIT,1)!=TextToSpeech.SUCCESS)return;
                float speed=Math.max(.5f,Math.min(2f,request.getSpeechRate()/100f));
                boolean[] failed={false};
                engine.generateWithCallback(text,0,speed,samples -> {
                    if(epoch.get()!=token)return 0;
                    int limit=Math.max(2,callback.getMaxBufferSize() & ~1);
                    byte[] bytes=new byte[Math.min(limit,samples.length*2)];
                    int index=0;
                    while(index<samples.length && epoch.get()==token) {
                        int count=Math.min(bytes.length/2,samples.length-index);
                        for(int i=0;i<count;i++) { int sample=Math.round(Math.max(-1,Math.min(1,samples[index+i]))*32767); bytes[2*i]=(byte)sample; bytes[2*i+1]=(byte)(sample>>8); }
                        if(callback.audioAvailable(bytes,0,count*2)!=TextToSpeech.SUCCESS) { failed[0]=true; return 0; }
                        index+=count;
                    }
                    return epoch.get()==token ? 1 : 0;
                });
                if(epoch.get()!=token || failed[0])callback.error(); else callback.done();
            } catch(Exception | LinkageError e) { callback.error(TextToSpeech.ERROR_SYNTHESIS); }
        }
    }
    @Override public void onDestroy() {
        onStop(); synchronized(engineLock) { if(engine!=null) { engine.release(); engine=null; } } super.onDestroy();
    }
}
