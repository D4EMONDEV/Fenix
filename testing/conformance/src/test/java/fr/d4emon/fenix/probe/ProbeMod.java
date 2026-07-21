package fr.d4emon.fenix.probe;

import fr.d4emon.fenix.api.Fenix;
import fr.d4emon.fenix.api.FenixMod;
import fr.d4emon.fenix.api.Mod;

/** A real mod, registering real content at the real moment. */
@Mod("probemod")
public final class ProbeMod implements FenixMod {

    public ProbeMod() {
    }

    @Override
    public void onRegister(Fenix fenix) {
        ProbeContent.REGISTRAR.apply();
    }
}
