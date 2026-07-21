package fr.d4emon.fenix.registry;

import net.minecraft.resources.Identifier;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * A handle on something a mod registers, usable before it exists.
 *
 * <p>This is what lets content be declared in {@code static final} fields and
 * referred to from anywhere, even though registration itself cannot happen
 * until the game opens its registries:
 *
 * <pre>{@code
 * public static final Holder<Block> RUBY_BLOCK = REGISTRAR.block("ruby_block");
 * }</pre>
 *
 * <p>Reading it before registration is a mistake with a clear message rather
 * than a {@code null} that surfaces somewhere else entirely.
 *
 * @param <T> what was registered
 */
public final class Holder<T> implements Supplier<T> {

    private final Identifier id;
    private volatile T value;

    Holder(Identifier id) {
        this.id = Objects.requireNonNull(id, "id");
    }

    /**
     * {@return the registered object}
     *
     * @throws IllegalStateException if registration has not happened yet
     */
    @Override
    public T get() {
        T current = value;
        if (current == null) {
            throw new IllegalStateException(id + " is not registered yet — content exists from onRegister "
                    + "onwards, so this was read too early (a static initialiser, or onPreLaunch)");
        }
        return current;
    }

    /**
     * {@return the id this was registered under}
     */
    public Identifier id() {
        return id;
    }

    /**
     * {@return whether registration has happened}
     */
    public boolean isBound() {
        return value != null;
    }

    void bind(T registered) {
        this.value = registered;
    }

    @Override
    public String toString() {
        return "Holder[" + id + (value == null ? ", unbound]" : "]");
    }
}
