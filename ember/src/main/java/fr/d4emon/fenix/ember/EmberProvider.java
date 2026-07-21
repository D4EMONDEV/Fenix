package fr.d4emon.fenix.ember;

/**
 * The base every provider shares.
 *
 * <p>A provider is created by Ember through its no-argument constructor and
 * handed its output afterwards, which is why {@link #output()} is only valid
 * once generation has started.
 */
public abstract class EmberProvider implements EmberGenerator {

    private EmberOutput output;

    /** For subclasses. */
    protected EmberProvider() {
    }

    @Override
    public final void generate(EmberOutput target) {
        this.output = target;
        run();
    }

    /**
     * {@return where this provider's files go}
     */
    protected final EmberOutput output() {
        if (output == null) {
            throw new IllegalStateException("output is only available while generating");
        }
        return output;
    }

    /**
     * {@return the mod being generated for}
     */
    protected final String modId() {
        return output().modId();
    }

    /** Does the work. Each provider turns this into something domain-shaped. */
    protected abstract void run();
}
