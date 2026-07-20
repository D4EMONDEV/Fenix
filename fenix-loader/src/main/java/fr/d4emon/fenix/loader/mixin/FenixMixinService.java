package fr.d4emon.fenix.loader.mixin;

import fr.d4emon.fenix.loader.classloader.FenixClassLoader;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.launch.platform.container.ContainerHandleVirtual;
import org.spongepowered.asm.launch.platform.container.IContainerHandle;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.MixinEnvironment.CompatibilityLevel;
import org.spongepowered.asm.mixin.transformer.IMixinTransformer;
import org.spongepowered.asm.mixin.transformer.IMixinTransformerFactory;
import org.spongepowered.asm.service.IAdviceProvider;
import org.spongepowered.asm.service.IClassBytecodeProvider;
import org.spongepowered.asm.service.IClassProvider;
import org.spongepowered.asm.service.IClassTracker;
import org.spongepowered.asm.service.IFeatureValidator;
import org.spongepowered.asm.service.IMixinAuditTrail;
import org.spongepowered.asm.service.IMixinInternal;
import org.spongepowered.asm.service.ITransformer;
import org.spongepowered.asm.service.ITransformerProvider;
import org.spongepowered.asm.service.MixinServiceAbstract;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Fenix's implementation of the Mixin service — the seam between the Mixin
 * library and the {@link FenixClassLoader}.
 *
 * <p>Mixin instantiates this itself, through a no-argument constructor it finds
 * from the {@code mixin.service} system property. It cannot be handed the
 * classloader as a constructor argument, so {@link MixinSetup} sets it
 * statically before Mixin is initialised. Everything the library needs — read a
 * mixin's bytecode, resolve a class, know whether a class has loaded — is then
 * answered against that loader.
 *
 * <p>{@link MixinServiceAbstract} supplies the bulk of the contract (phases,
 * logging, the re-entrance lock, container discovery); this class adds the four
 * providers Mixin routes real work through, mirroring what Fabric's own service
 * implements.
 */
public final class FenixMixinService extends MixinServiceAbstract
        implements IClassProvider, IClassBytecodeProvider, IClassTracker, ITransformerProvider {

    /** The one active service instance, so {@link MixinSetup} can reach the transformer. */
    private static volatile FenixMixinService instance;

    /** Set before Mixin boots; the loader every provider answers against. */
    private static volatile FenixClassLoader classLoader;

    private final Set<String> loadedClasses = ConcurrentHashMap.newKeySet();
    private final Set<String> invalidClasses = ConcurrentHashMap.newKeySet();

    private volatile IMixinTransformerFactory transformerFactory;
    private volatile IMixinTransformer transformer;

    /** Instantiated by Mixin via the {@code mixin.service} system property. */
    public FenixMixinService() {
        instance = this;
    }

    /**
     * Binds the classloader Mixin works against. Called by {@link MixinSetup}
     * before {@code MixinBootstrap.init()}.
     *
     * @param loader the loader the game and mods live in
     */
    static void bindClassLoader(FenixClassLoader loader) {
        classLoader = loader;
    }

    /**
     * {@return the active service instance}
     *
     * @throws IllegalStateException if Mixin has not created it yet
     */
    static FenixMixinService active() {
        FenixMixinService active = instance;
        if (active == null) {
            throw new IllegalStateException("the Fenix Mixin service has not been created — "
                    + "was mixin.service set before MixinBootstrap.init()?");
        }
        return active;
    }

    private static FenixClassLoader loader() {
        FenixClassLoader loader = classLoader;
        if (loader == null) {
            throw new IllegalStateException("the Fenix Mixin service was used before its classloader was bound");
        }
        return loader;
    }

    // ------------------------------------------------------------------
    // The transformation bridge, driven from FenixClassLoader
    // ------------------------------------------------------------------

    /**
     * Applies the mixins to one class. Wired into the loader as a transformer.
     *
     * <p>Before the transformer exists (the window between adding jars and
     * finishing Mixin setup) this returns the bytes untouched — nothing that
     * loads that early is a mixin target.
     *
     * @param name  the class being defined
     * @param bytes its current bytes
     * @return the transformed bytes, or the input when no mixin applies
     */
    byte[] transformClass(String name, byte[] bytes) {
        IMixinTransformer active = transformer();
        byte[] result = active == null ? bytes : active.transformClassBytes(name, name, bytes);
        // Marked loaded only now, after transforming — the class is not defined
        // until this returns. Marking it earlier makes Mixin believe a target
        // being transformed for the first time "was loaded too early", because
        // Mixin prepares a config lazily inside this very call and then consults
        // the tracker for its own target.
        loadedClasses.add(name);
        return result;
    }

    /**
     * Generates a Mixin synthetic class. Wired into the loader as its
     * {@link FenixClassLoader.ClassGenerator}.
     *
     * @param name the synthetic class being defined
     * @return its bytes, or {@code null} if Mixin has nothing to generate
     */
    byte[] generateClass(String name) {
        IMixinTransformer active = transformer();
        return active == null ? null : active.generateClass(MixinEnvironment.getCurrentEnvironment(), name);
    }

    /** Forces the transformer to exist, so setup fails loudly rather than at first game class. */
    void primeTransformer() {
        transformer();
    }

    private IMixinTransformer transformer() {
        IMixinTransformer active = transformer;
        if (active == null) {
            IMixinTransformerFactory factory = transformerFactory;
            if (factory != null) {
                synchronized (this) {
                    if (transformer == null) {
                        transformer = factory.createTransformer();
                    }
                    active = transformer;
                }
            }
        }
        return active;
    }

    @Override
    public void offer(IMixinInternal internal) {
        if (internal instanceof IMixinTransformerFactory factory) {
            this.transformerFactory = factory;
        }
        super.offer(internal);
    }

    // ------------------------------------------------------------------
    // IMixinService
    // ------------------------------------------------------------------

    @Override
    public String getName() {
        return "Fenix";
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public IClassProvider getClassProvider() {
        return this;
    }

    @Override
    public IClassBytecodeProvider getBytecodeProvider() {
        return this;
    }

    @Override
    public ITransformerProvider getTransformerProvider() {
        return this;
    }

    @Override
    public IClassTracker getClassTracker() {
        return this;
    }

    @Override
    public IMixinAuditTrail getAuditTrail() {
        return null;
    }

    @Override
    public IFeatureValidator getFeatureValidator() {
        return IFeatureValidator.ALLOW_ALL;
    }

    @Override
    public IAdviceProvider getAdviceProvider() {
        return IAdviceProvider.GENERIC;
    }

    @Override
    public Collection<String> getPlatformAgents() {
        return List.of();
    }

    @Override
    public IContainerHandle getPrimaryContainer() {
        return new ContainerHandleVirtual(getName());
    }

    @Override
    public InputStream getResourceAsStream(String name) {
        return loader().getResourceAsStream(name);
    }

    @Override
    public CompatibilityLevel getMaxCompatibilityLevel() {
        // The game is compiled for Java 25; without this Mixin refuses its classes.
        return CompatibilityLevel.JAVA_25;
    }

    // ------------------------------------------------------------------
    // IClassProvider
    // ------------------------------------------------------------------

    @Override
    @SuppressWarnings("deprecation") // required by the interface; unused by the modern pipeline
    public java.net.URL[] getClassPath() {
        return new java.net.URL[0];
    }

    @Override
    public Class<?> findClass(String name) throws ClassNotFoundException {
        return loader().loadClass(name);
    }

    @Override
    public Class<?> findClass(String name, boolean initialize) throws ClassNotFoundException {
        return Class.forName(name, initialize, loader());
    }

    @Override
    public Class<?> findAgentClass(String name, boolean initialize) throws ClassNotFoundException {
        // Agent classes live on the system classpath, not in the game scope.
        return Class.forName(name, initialize, ClassLoader.getSystemClassLoader());
    }

    // ------------------------------------------------------------------
    // IClassBytecodeProvider
    // ------------------------------------------------------------------

    @Override
    public ClassNode getClassNode(String name) throws ClassNotFoundException, IOException {
        return getClassNode(name, false, 0);
    }

    @Override
    public ClassNode getClassNode(String name, boolean runTransformers) throws ClassNotFoundException, IOException {
        return getClassNode(name, runTransformers, 0);
    }

    @Override
    public ClassNode getClassNode(String name, boolean runTransformers, int readerFlags)
            throws ClassNotFoundException, IOException {
        // Mixin reads mixin classes and target-class hierarchies through here,
        // and it wants the original bytecode — never the post-mixin result — so
        // runTransformers is intentionally not honoured.
        byte[] bytes = loader().readClassBytes(name.replace('/', '.'));
        if (bytes == null) {
            throw new ClassNotFoundException(name);
        }
        ClassNode node = new ClassNode();
        new ClassReader(bytes).accept(node, readerFlags);
        return node;
    }

    // ------------------------------------------------------------------
    // IClassTracker
    // ------------------------------------------------------------------

    @Override
    public void registerInvalidClass(String className) {
        invalidClasses.add(className);
    }

    @Override
    public boolean isClassLoaded(String className) {
        return loadedClasses.contains(className);
    }

    @Override
    public String getClassRestrictions(String className) {
        // Fenix imposes none; the empty string is Mixin's "no restrictions".
        return "";
    }

    // ------------------------------------------------------------------
    // ITransformerProvider — Fenix runs no transformers other than Mixin
    // ------------------------------------------------------------------

    @Override
    public Collection<ITransformer> getTransformers() {
        return List.of();
    }

    @Override
    public Collection<ITransformer> getDelegatedTransformers() {
        return List.of();
    }

    @Override
    public void addTransformerExclusion(String name) {
        // No delegated transformers to exclude anything from.
    }
}
