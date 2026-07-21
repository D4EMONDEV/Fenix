package fr.d4emon.fenix.network;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * What a side has registered, small enough to send on every join.
 *
 * <p>Sending the ids themselves would be honest and useless: a large modpack
 * has tens of thousands, which is megabytes on a channel capped at one, on
 * every connection. So each registry is reduced to a digest, and the mod
 * namespaces are sent alongside in full.
 *
 * <p>That pairing is what lets the refusal be specific. A digest alone can only
 * say "these differ"; the namespaces turn the common case — someone is missing
 * a mod — into a sentence naming it. When the namespaces match and the digests
 * do not, the mods are the same and their versions are not, which is a
 * different sentence and just as useful.
 *
 * @param digests    registry name to a digest of its modded ids
 * @param namespaces every mod namespace that registered anything
 */
public record RegistrySummary(Map<String, String> digests, Set<String> namespaces) {

    /** The registries a mismatch actually breaks. */
    private static final Map<String, Registry<?>> WATCHED = watched();

    /** How a summary is written and read. */
    public static final StreamCodec<FriendlyByteBuf, RegistrySummary> CODEC =
            StreamCodec.of((buffer, summary) -> summary.write(buffer), RegistrySummary::read);

    /**
     * Copies the collections so a summary cannot change under its reader.
     */
    public RegistrySummary {
        digests = Map.copyOf(digests);
        namespaces = Set.copyOf(namespaces);
    }

    /**
     * {@return what this side has registered}
     */
    public static RegistrySummary local() {
        Map<String, String> digests = new TreeMap<>();
        Set<String> namespaces = new TreeSet<>();

        WATCHED.forEach((name, registry) -> {
            List<String> modded = new ArrayList<>();
            for (Identifier id : registry.keySet()) {
                if (!id.getNamespace().equals("minecraft")) {
                    modded.add(id.toString());
                    namespaces.add(id.getNamespace());
                }
            }
            // Sorted, or two sides that agree entirely would still disagree
            // because their registries were filled in a different order.
            modded.sort(null);
            digests.put(name, digest(modded));
        });
        return new RegistrySummary(digests, namespaces);
    }

    /**
     * {@return what is wrong, in sentences, or empty when the two agree}
     *
     * @param theirs what the other side sent
     */
    public List<String> differencesFrom(RegistrySummary theirs) {
        List<String> problems = new ArrayList<>();

        Set<String> missingHere = new TreeSet<>(theirs.namespaces());
        missingHere.removeAll(namespaces);
        if (!missingHere.isEmpty()) {
            problems.add("missing: " + String.join(", ", missingHere));
        }

        Set<String> extraHere = new TreeSet<>(namespaces);
        extraHere.removeAll(theirs.namespaces());
        if (!extraHere.isEmpty()) {
            problems.add("not on the other side: " + String.join(", ", extraHere));
        }

        if (problems.isEmpty()) {
            // Same mods by name, so whatever differs is inside one of them.
            List<String> differing = new ArrayList<>();
            digests.forEach((name, digest) -> {
                if (!digest.equals(theirs.digests().get(name))) {
                    differing.add(name);
                }
            });
            if (!differing.isEmpty()) {
                differing.sort(null);
                problems.add("the same mods registered different content in "
                        + String.join(", ", differing) + " — the versions differ");
            }
        }
        return problems;
    }

    private static Map<String, Registry<?>> watched() {
        Map<String, Registry<?>> registries = new LinkedHashMap<>();
        registries.put("blocks", BuiltInRegistries.BLOCK);
        registries.put("items", BuiltInRegistries.ITEM);
        registries.put("entities", BuiltInRegistries.ENTITY_TYPE);
        registries.put("block entities", BuiltInRegistries.BLOCK_ENTITY_TYPE);
        return registries;
    }

    private static String digest(List<String> ids) {
        MessageDigest sha256;
        try {
            sha256 = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("every JVM has SHA-256", e);
        }
        for (String id : ids) {
            sha256.update(id.getBytes(StandardCharsets.UTF_8));
            // Separated, or "ab" + "c" and "a" + "bc" would digest the same.
            sha256.update((byte) 0);
        }
        byte[] hash = sha256.digest();
        StringBuilder hex = new StringBuilder(16);
        for (int i = 0; i < 8; i++) {
            hex.append("%02x".formatted(hash[i]));
        }
        return hex.toString();
    }

    private void write(FriendlyByteBuf out) {
        out.writeVarInt(digests.size());
        new TreeMap<>(digests).forEach((name, digest) -> {
            out.writeUtf(name);
            out.writeUtf(digest);
        });
        out.writeVarInt(namespaces.size());
        new TreeSet<>(namespaces).forEach(out::writeUtf);
    }

    private static RegistrySummary read(FriendlyByteBuf in) {
        Map<String, String> digests = new TreeMap<>();
        for (int i = in.readVarInt(); i > 0; i--) {
            digests.put(in.readUtf(), in.readUtf());
        }
        Set<String> namespaces = new TreeSet<>();
        for (int i = in.readVarInt(); i > 0; i--) {
            namespaces.add(in.readUtf());
        }
        return new RegistrySummary(digests, namespaces);
    }
}
