package de.sprechbruecke.voices;
import com.k2fsa.sherpa.onnx.*;
import java.io.File;
final class PiperEngine {
    static OfflineTts create(File directory, PiperVoice voice) {
        OfflineTtsVitsModelConfig vits=new OfflineTtsVitsModelConfig();
        vits.setModel(new File(directory,voice.model+".onnx").getAbsolutePath());
        vits.setTokens(new File(directory,"tokens.txt").getAbsolutePath());
        vits.setDataDir(new File(directory,"espeak-ng-data").getAbsolutePath());
        OfflineTtsModelConfig model=new OfflineTtsModelConfig(); model.setVits(vits); model.setNumThreads(2); model.setDebug(false); model.setProvider("cpu");
        OfflineTtsConfig config=new OfflineTtsConfig(); config.setModel(model); config.setMaxNumSentences(1);
        return new OfflineTts(null,config);
    }
}
