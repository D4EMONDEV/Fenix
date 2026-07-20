package fr.d4emon.fenix.loader.mixin;

import org.spongepowered.asm.service.IMixinServiceBootstrap;

/**
 * Names the Fenix service to Mixin's bootstrap.
 *
 * <p>Discovered through {@code META-INF/services}. Mixin also honours the
 * {@code mixin.service} system property, which {@link MixinSetup} sets — either
 * path selects {@link FenixMixinService}; the fork otherwise ships only its
 * LaunchWrapper and ModLauncher services, neither of which is valid here.
 */
public final class FenixMixinServiceBootstrap implements IMixinServiceBootstrap {

    /** Instantiated by Mixin through {@code META-INF/services}. */
    public FenixMixinServiceBootstrap() {
    }

    @Override
    public String getName() {
        return "Fenix";
    }

    @Override
    public String getServiceClassName() {
        return FenixMixinService.class.getName();
    }

    @Override
    public void bootstrap() {
        // Nothing to prepare: the classloader is bound by MixinSetup, and there
        // is no environment to touch before the service is created.
    }
}
