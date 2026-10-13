package de.sprechbruecke.voices;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import java.util.ArrayList;
public class CheckVoiceData extends Activity {
    @Override public void onCreate(Bundle state) {
        super.onCreate(state); PiperModels models=PiperModels.get(this);
        ArrayList<String> available=new ArrayList<>(),missing=new ArrayList<>();
        if(models.installed(PiperVoice.THORSTEN))available.add("deu-DEU"); else missing.add("deu-DEU");
        if(models.installed(PiperVoice.GOSIA)||models.installed(PiperVoice.DARKMAN))available.add("pol-POL"); else missing.add("pol-POL");
        Intent result=new Intent().putStringArrayListExtra(TextToSpeech.Engine.EXTRA_AVAILABLE_VOICES,available).putStringArrayListExtra(TextToSpeech.Engine.EXTRA_UNAVAILABLE_VOICES,missing);
        setResult(available.isEmpty() ? TextToSpeech.Engine.CHECK_VOICE_DATA_FAIL : TextToSpeech.Engine.CHECK_VOICE_DATA_PASS,result); finish();
    }
}
