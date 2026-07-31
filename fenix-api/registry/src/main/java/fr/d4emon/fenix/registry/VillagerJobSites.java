package fr.d4emon.fenix.registry;

import com.mojang.logging.LogUtils;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.PoiTypeTags;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * The job sites mods have claimed, and the one check that says whether a
 * villager can ever reach them.
 *
 * <p>Registering a profession and its point of interest is not enough, and the
 * missing piece is invisible: an <em>unemployed</em> villager searches with the
 * {@code none} profession's predicate, which is not "any registered job site"
 * but "anything in the {@code minecraft:acquirable_job_site} tag". A point of
 * interest outside that tag is never looked for, so the profession is registered,
 * correct, reachable by command — and no villager in the world ever takes it.
 *
 * <p>Nothing about that fails, logs or throws. The mod author sees villagers
 * ignoring a block and has no thread to pull. So this records what a mod claimed
 * and, the moment the tags are known, says plainly what is missing and which
 * file fixes it.
 */
public final class VillagerJobSites {

    private static final Logger LOG = LogUtils.getLogger();

    /** Profession id → the job site it is taken at, in declaration order. */
    private static final List<Map.Entry<Identifier, ResourceKey<PoiType>>> CLAIMED =
            new CopyOnWriteArrayList<>();

    /** Reported once each: tags rebind on every datapack reload. */
    private static final Map<Identifier, Boolean> REPORTED = new ConcurrentHashMap<>();

    private VillagerJobSites() {
    }

    /**
     * Records that a profession is taken at a job site. Called by
     * {@link Registrar#villagerProfession}.
     *
     * @param profession the profession's id
     * @param jobSite    the point of interest a villager claims to take it
     */
    static void claim(Identifier profession, ResourceKey<PoiType> jobSite) {
        CLAIMED.add(Map.entry(profession, jobSite));
    }

    /**
     * Checks every claimed job site against the tag that makes it findable, and
     * logs the ones that are missing.
     *
     * <p>Called from the mixin on tag binding — the first moment the answer is
     * knowable, and still early enough to act on before a player wonders why
     * their villagers are idle.
     *
     * @param registry the point-of-interest registry, with its tags bound
     */
    public static void verify(Registry<PoiType> registry) {
        for (Identifier profession : unreachable(registry)) {
            ResourceKey<PoiType> jobSite = jobSiteOf(profession);
            // Reported once each: tags rebind on every datapack reload, and the
            // same complaint every time trains the reader to skip it.
            if (jobSite == null || REPORTED.putIfAbsent(jobSite.identifier(), true) != null) {
                continue;
            }
            LOG.error("Fenix: no villager will ever take {} — its job site {} is not in {}. "
                            + "An unemployed villager only looks for job sites in that tag, so the "
                            + "profession is registered, correct, and unreachable. Add it with "
                            + "data/minecraft/tags/point_of_interest_type/acquirable_job_site.json "
                            + "containing {{\"values\": [\"{}\"]}} — tags merge, so vanilla's entries stay.",
                    profession, jobSite.identifier(), PoiTypeTags.ACQUIRABLE_JOB_SITE.location(),
                    jobSite.identifier());
        }
    }

    /**
     * {@return the professions no villager could ever take, by id}
     *
     * <p>A profession lands here when its job site is missing from
     * {@code minecraft:acquirable_job_site} — which is the whole of the failure.
     * A question with an answer rather than only a log line, so the conformance
     * suite can ask it directly.
     *
     * @param registry the point-of-interest registry, with its tags bound
     */
    public static List<Identifier> unreachable(Registry<PoiType> registry) {
        List<Identifier> missing = new ArrayList<>();
        for (Map.Entry<Identifier, ResourceKey<PoiType>> claim : CLAIMED) {
            boolean acquirable = registry.getOptional(claim.getValue())
                    .map(poi -> registry.wrapAsHolder(poi).is(PoiTypeTags.ACQUIRABLE_JOB_SITE))
                    .orElse(false);
            if (!acquirable) {
                missing.add(claim.getKey());
            }
        }
        return List.copyOf(missing);
    }

    /**
     * {@return the job site a profession is taken at, or {@code null} if the
     * profession was not registered through Fenix}
     *
     * @param profession the profession's id
     */
    public static ResourceKey<PoiType> jobSiteOf(Identifier profession) {
        for (Map.Entry<Identifier, ResourceKey<PoiType>> claim : CLAIMED) {
            if (claim.getKey().equals(profession)) {
                return claim.getValue();
            }
        }
        return null;
    }

    /** {@return the registry key the check applies to} */
    public static ResourceKey<? extends Registry<PoiType>> registryKey() {
        return Registries.POINT_OF_INTEREST_TYPE;
    }
}
