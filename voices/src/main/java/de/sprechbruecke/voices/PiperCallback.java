package de.sprechbruecke.voices;

import kotlin.jvm.functions.Function1;

/** Sherpa JNI looks up invoke(float[]):Integer, not the erased invoke(Object) of a Java lambda. */
abstract class PiperCallback implements Function1<float[], Integer> {
    @Override public abstract Integer invoke(float[] samples);
}
