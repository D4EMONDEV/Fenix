package fr.d4emon.fenix.ember;

import net.minecraft.resources.RegistryOps;
import net.minecraft.data.registries.VanillaRegistries;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.DynamicOps;
import com.google.gson.JsonElement;
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
     * The ops a generated file is read back with, built once for a whole run.
     *
     * <p>Not {@code JsonOps.INSTANCE}: holder sets and tags are resolved
     * through the ops rather than parsed out of the JSON, so plain ops fail on
     * every item in every predicate — and the message names the last branch
     * tried rather than the cause.
     *
     * <p>Building the lookup runs the datapack registries, which is slow
     * enough to be worth doing once rather than once per file.
     *
     * @return ops that can resolve registry references
     */
    protected static DynamicOps<JsonElement> registryOps() {
        if (ops == null) {
            ops = RegistryOps.create(JsonOps.INSTANCE, VanillaRegistries.createLookup());
        }
        return ops;
    }

    private static DynamicOps<JsonElement> ops;

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
