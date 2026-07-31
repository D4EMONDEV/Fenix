package fr.d4emon.fenix.registry.attachment;

import java.util.Map;

/**
 * Something a mod can attach data to.
 *
 * <p>Implemented by a mixin on the game's own classes — {@code Entity} and
 * {@code BlockEntity} — so at run time every one of those already is an
 * {@code AttachmentHolder}, and {@link Attachments} reaches this interface by
 * casting to it. That is also why the interface lives here rather than beside
 * the mixin: Mixin owns every class in a package one of its configs declares,
 * so an interface a mixin implements has to be declared somewhere Mixin does
 * not, or loading it would fail as though it were a mixin template.
 *
 * <p>Not something a mod implements or calls directly; use {@link Attachments}.
 */
public interface AttachmentHolder {

    /**
     * {@return the live map of this holder's attachments}
     *
     * <p>Created on first use, so a holder that carries no attachments — most of
     * them — pays for nothing. Keyed by identity: two attachment types are the
     * same key only if they are the same object, which they are, being kept in
     * {@code static final} fields.
     */
    Map<AttachmentType<?>, Object> fenix$attachments();
}
