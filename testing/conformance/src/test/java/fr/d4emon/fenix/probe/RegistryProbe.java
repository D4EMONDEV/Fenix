package fr.d4emon.fenix.probe;

import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import fr.d4emon.fenix.registry.CreativePages;
import fr.d4emon.fenix.registry.CreativeTabs;
import fr.d4emon.fenix.registry.Registrar;
import fr.d4emon.fenix.registry.VillagerJobSites;
import fr.d4emon.fenix.registry.attachment.AttachmentHolder;
import fr.d4emon.fenix.registry.attachment.Attachments;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.HoneycombItem;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.ComposterBlock;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.WeatheringCopper;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.Optional;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * Runs as the game: boots the registries, which fires onRegister, then checks
 * that every pass vanilla performs around its own registration also happened
 * for the mod's content.
 *
 * <p>Failure is an exception, which the loader propagates out of
 * {@code Launch.run} and into the test.
 */
public final class RegistryProbe {

    private RegistryProbe() {
    }

    public static void main(String[] args) {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();

        Identifier blockId = Identifier.parse("probemod:ruby_block");
        Identifier itemId = Identifier.parse("probemod:ruby");

        Block block = BuiltInRegistries.BLOCK.getValue(blockId);
        require(block != null && block != Blocks.AIR, "the block should be in the registry");
        require(BuiltInRegistries.ITEM.getValue(itemId) != null, "the item should be in the registry");

        // Vanilla assigns these in one pass in Blocks' static initialiser, long
        // finished by the time a mod registers. Missing them kicks the player
        // with "Can't find id for Block{...}" and breaks rendering.
        for (BlockState state : block.getStateDefinition().getPossibleStates()) {
            require(Block.BLOCK_STATE_REGISTRY.getId(state) >= 0,
                    "every block state should have a network id");
        }

        // Without the Item.BY_BLOCK mapping, asItem() answers air and caches it,
        // so ItemStacks of the block are empty and the creative tab dies.
        require(block.asItem() != Items.AIR, "asItem() should not be air");
        require(block.asItem() == BuiltInRegistries.ITEM.getValue(blockId),
                "asItem() should be the block's own item");

        checkCreativePage();
        checkBlockEntity();
        checkSound();
        checkEntity();
        checkMenu();
        checkSpawning();
        checkSmallRegistries();
        checkFluid();
        checkAttachments();
        checkRecipe();
        checkVillager();
        checkBlockInteractions();
        checkBrewing();
        checkCommandArgument();

        System.out.println("registry conformance: all checks passed");
    }

    /**
     * A custom command argument has to be describable to a joining client.
     *
     * <p>The registry entry is the easy half and proves little. Vanilla writes
     * the command tree for each player who connects, and does it through a
     * second table keyed by the Brigadier class — {@code ArgumentTypeInfos} —
     * which its own {@code byClass} throws on when a class is missing.
     *
     * <p>So a mod that registered only into the registry works perfectly in
     * single player, and the first person to join fails to, with
     * "Unrecognized argument type" naming a Brigadier class and no mod. This
     * calls the very method that throws.
     */
    private static void checkCommandArgument() {
        Identifier id = Identifier.parse("probemod:ore");
        require(BuiltInRegistries.COMMAND_ARGUMENT_TYPE.getValue(id) == ProbeContent.ORE_ARGUMENT.get(),
                "the argument type should be in the registry, bound to its handle");

        require(ArgumentTypeInfos.isClassRecognized(ProbeArgument.class),
                "vanilla should recognise the argument's own class — the table it reads while "
                        + "describing commands to a client is keyed by class, not by id");

        // The exact call the command tree makes for a joining player. It throws
        // when the class is absent, which is the whole failure being guarded.
        ArgumentTypeInfo.Template<ProbeArgument> packed =
                ArgumentTypeInfos.unpack(ProbeArgument.ore());
        require(packed != null, "the argument should pack for the network, or nobody can join");
    }

    /**
     * A potion has to exist, and something has to brew into it.
     *
     * <p>The two halves fail differently and both quietly. A potion that is not
     * registered cannot be referred to at all; a potion that is registered but
     * that nothing brews into can be given by command and made by no brewing
     * stand in the world, which reads as a broken recipe rather than as a table
     * that was closed before the mod reached it.
     *
     * <p>Asked of a real {@code PotionBrewing}, built the way a server builds
     * its own, rather than of Fenix's own list.
     */
    private static void checkBrewing() {
        Potion potion =
                BuiltInRegistries.POTION.getValue(Identifier.parse("probemod:glimmering_potion"));
        require(potion == ProbeContent.GLIMMERING.get(),
                "the potion should be in the registry, bound to its handle");

        // Built exactly as a server builds it — the only way to know the mod's
        // mixes reached the builder before it closed.
        PotionBrewing brewing = PotionBrewing.bootstrap(FeatureFlags.VANILLA_SET);

        // Read out of the finished table rather than asked through isIngredient
        // or hasMix: both take an ItemStack, and a stack cannot exist until data
        // components are bound, which is a datapack load away. The table itself
        // is the thing that had to carry the entry, so the table is what is
        // asked.
        require(brewsInto(brewing, potion),
                "a declared mix should be in the table a server actually builds — vanilla throws "
                        + "the builder away, so a mod that misses it can brew nothing, ever");
    }

    /** {@return whether any mix in the finished table produces this potion} */
    private static boolean brewsInto(PotionBrewing brewing, Potion potion) {
        try {
            var mixes = PotionBrewing.class.getDeclaredField("potionMixes");
            mixes.setAccessible(true);
            for (Object mix : (List<?>) mixes.get(brewing)) {
                var to = mix.getClass().getDeclaredField("to");
                to.setAccessible(true);
                if (((net.minecraft.core.Holder<?>) to.get(mix)).value() == potion) {
                    return true;
                }
            }
            return false;
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("registry conformance failed: could not read the brewing table", e);
        }
    }

    /**
     * The behaviour vanilla keeps in tables rather than on the block.
     *
     * <p>Every one of these fails silently: a modded log that will not burn, an
     * axe that does nothing to it, a furnace that refuses a modded coal. Nothing
     * is thrown and nothing is logged, because from vanilla's side nothing is
     * wrong — the block is simply not in a table that was filled at bootstrap.
     *
     * <p>Asked of vanilla's own lookups rather than of Fenix's tables, since a
     * table Fenix agrees with itself about proves nothing.
     */
    private static void checkBlockInteractions() {
        Block log = ProbeContent.RUBY_LOG.get();

        // Fire keeps two private tables on the FIRE block instance.
        require(fireOdds("getIgniteOdds", log.defaultBlockState()) == 5,
                "a declared block should have its ignite odds, or it never catches fire");
        require(fireOdds("getBurnOdds", log.defaultBlockState()) == 5,
                "and its burn odds, or it never burns away");

        // The composter's table is vanilla's own, written into rather than
        // answered around — so this reads exactly what a composter reads.
        require(ComposterBlock.COMPOSTABLES.getFloat(ProbeContent.RUBY.get()) == 0.5f,
                "a declared item should be in the composter's table");

        // An axe strips through a private method reading an immutable map.
        Optional<BlockState> stripped = strippedByAxe(log.defaultBlockState());
        require(stripped.isPresent() && stripped.get().getBlock() == ProbeContent.STRIPPED_RUBY_LOG.get(),
                "an axe should strip a declared log into the block declared for it");

        // Waxing: vanilla's table is replaced wholesale, so both directions and
        // every reader of it should agree.
        Block copper = ProbeContent.RUBY_COPPER.get();
        Block waxed = ProbeContent.WAXED_RUBY_COPPER.get();
        require(HoneycombItem.getWaxed(copper.defaultBlockState())
                        .map(BlockState::getBlock).orElse(null) == waxed,
                "honeycomb should wax a declared block");
        require(HoneycombItem.WAX_OFF_BY_BLOCK.get().get(waxed) == copper,
                "and the inverse table should let an axe scrape it off again — that table is "
                        + "read inline in four places, so it has to carry the entry itself");

        // Oxidation, both ways, through the two static lookups every caller uses.
        require(WeatheringCopper.getNext(copper).orElse(null) == ProbeContent.EXPOSED_RUBY_COPPER.get(),
                "a declared block should weather into the stage declared for it");
        require(WeatheringCopper.getPrevious(ProbeContent.EXPOSED_RUBY_COPPER.get()).orElse(null) == copper,
                "and scrape back to the stage before it");

        // Fuel is not asked here. Every lookup on it takes an ItemStack, and a
        // stack cannot be built until data components are bound, which happens
        // while datapacks load — long after a probe stops. Vanilla's own
        // Items.STONE is equally unstackable at this point, so the limit is the
        // process rather than the mod. BlockInteractionMixinTest proves the
        // injection lands instead.
    }

    /** Fire's odds live behind private methods on the FIRE block instance. */
    private static int fireOdds(String method, BlockState state) {
        try {
            var declared = FireBlock.class.getDeclaredMethod(method, BlockState.class);
            declared.setAccessible(true);
            return (int) declared.invoke(Blocks.FIRE, state);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("registry conformance failed: could not read " + method, e);
        }
    }

    /** So is the axe's stripping table. */
    @SuppressWarnings("unchecked")
    private static Optional<BlockState> strippedByAxe(BlockState state) {
        try {
            var declared = AxeItem.class.getDeclaredMethod("getStripped", BlockState.class);
            declared.setAccessible(true);
            return (Optional<BlockState>) declared.invoke(Items.IRON_AXE, state);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("registry conformance failed: could not read getStripped", e);
        }
    }

    /**
     * A custom recipe type has to reach the game, and its serializer has to read
     * what it wrote.
     *
     * <p>The full datapack path cannot run here — datapacks are not loaded, and
     * a probe cannot even make an {@code ItemStack} until they are. But a recipe
     * is built from an {@code Ingredient} and an {@code ItemStackTemplate}, both
     * of which are descriptions rather than live stacks, so a recipe can be
     * built and put through its own serializer's codec — which is exactly the
     * step a datapack load performs, and exactly where a serializer mistake
     * would drop the recipe without a word.
     */
    private static void checkRecipe() {
        RecipeType<?> type =
                BuiltInRegistries.RECIPE_TYPE.getValue(Identifier.parse("probemod:reforging"));
        require(type == ProbeContent.REFORGING_TYPE.get(),
                "the recipe type should be in the registry, bound to its handle");
        RecipeSerializer<?> serializer =
                BuiltInRegistries.RECIPE_SERIALIZER.getValue(Identifier.parse("probemod:reforging"));
        require(serializer == ProbeContent.REFORGING_SERIALIZER.get(),
                "the recipe serializer should be in the registry, bound to its handle");

        ProbeReforgingRecipe recipe = new ProbeReforgingRecipe(
                new Recipe.CommonInfo(true), Ingredient.of(Items.DIAMOND), new ItemStackTemplate(Items.EMERALD));
        require(recipe.getType() == type, "a recipe should carry the registered type");
        require(recipe.getSerializer() == serializer, "and the registered serializer");

        // Registry-aware ops, because a recipe names its items by registry id:
        // plain JsonOps has no registry to resolve them against, and the round
        // trip stops being symmetric. This wraps the builtin registries, which
        // are all a recipe of items needs.
        DynamicOps<JsonElement> ops = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY)
                .createSerializationContext(JsonOps.INSTANCE);
        Codec<ProbeReforgingRecipe> codec = ProbeContent.REFORGING_SERIALIZER.get().codec().codec();

        // Encode through the serializer's own codec and read it back — the round
        // trip a datapack load and a network sync both depend on.
        JsonElement json = codec.encodeStart(ops, recipe)
                .getOrThrow(message -> new AssertionError(
                        "registry conformance failed: the recipe did not encode: " + message));
        ProbeReforgingRecipe back = codec.parse(ops, json)
                .getOrThrow(message -> new AssertionError(
                        "registry conformance failed: the recipe did not read back: " + message));
        require(back.getType() == type, "a recipe should survive its own serializer's codec");
    }

    /**
     * A profession has to reach the game, and its job site has to be recognised.
     *
     * <p>The quiet failure is the second half: a point of interest registered
     * after bootstrap is in the registry but not in the block-state map the
     * villager AI reads, so the job site exists and no villager ever claims it —
     * the profession is real and unreachable. So the check is not only that the
     * two are registered, but that the job-site block resolves to the point of
     * interest, and the profession accepts it.
     */
    private static void checkVillager() {
        PoiType poi =
                BuiltInRegistries.POINT_OF_INTEREST_TYPE.getValue(Identifier.parse("probemod:ruby_stall"));
        require(poi == ProbeContent.RUBY_STALL_POI.get(),
                "the point of interest should be in the registry, bound to its handle");

        BlockState jobSite = ProbeContent.RUBY_BLOCK.get().defaultBlockState();
        require(PoiTypes.hasPoi(jobSite),
                "the job-site block's state should be a point of interest — without the bookkeeping "
                        + "pass it is not, and no villager ever claims it");
        require(PoiTypes.forState(jobSite).orElseThrow().value() == poi,
                "and it should resolve to the registered type");

        VillagerProfession profession =
                BuiltInRegistries.VILLAGER_PROFESSION.getValue(Identifier.parse("probemod:jeweller"));
        require(profession == ProbeContent.JEWELLER.get(),
                "the profession should be in the registry, bound to its handle");
        require(profession.acquirableJobSite()
                        .test(BuiltInRegistries.POINT_OF_INTEREST_TYPE.wrapAsHolder(poi)),
                "the profession should accept its own job site, or no villager takes it");

        // The part every check above missed, and a real bug found in game: all
        // of the above can hold and no villager ever takes the profession,
        // because an unemployed one searches with the `none` profession's
        // predicate — which is not "any registered job site" but "anything in
        // minecraft:acquirable_job_site". A job site outside that tag is never
        // looked for.
        //
        // No tags are bound in this process, so every claimed job site should be
        // reported unreachable here. That is the guard proving it can see the
        // failure at all; that the tag is actually shipped is a question about a
        // mod's files, which VillagerTagConformanceTest asks of example-mod.
        require(VillagerJobSites.unreachable(BuiltInRegistries.POINT_OF_INTEREST_TYPE)
                        .contains(Identifier.parse("probemod:jeweller")),
                "with no tags bound, the guard should report the profession as unreachable — "
                        + "otherwise it cannot warn anyone about the one thing that silently "
                        + "stops a villager taking a job");
    }

    /**
     * A fluid is four registrations that have to agree.
     *
     * <p>Each of the four can be present and still leave the fluid broken if it
     * points at the wrong one of the others: a block whose fluid is not the one
     * registered places nothing, a bucket that names the wrong fluid empties
     * nothing. None of that shows up as a missing-registry error — the registry
     * is fine, the wiring is not — so the check is that the four name each
     * other, not merely that they exist.
     */
    private static void checkFluid() {
        FlowingFluid source = ProbeContent.ACID.source().get();
        FlowingFluid flowing = ProbeContent.ACID.flowing().get();
        Block block = ProbeContent.ACID.block().get();

        require(BuiltInRegistries.FLUID.getValue(Identifier.parse("probemod:acid")) == source,
                "the still fluid should be in the registry, bound to its handle");
        require(BuiltInRegistries.FLUID.getValue(Identifier.parse("probemod:flowing_acid")) == flowing,
                "the flowing fluid should be in the registry, bound to its handle");
        require(BuiltInRegistries.BLOCK.getValue(Identifier.parse("probemod:acid")) == block,
                "the fluid's block should be in the registry");
        require(block instanceof LiquidBlock, "the fluid's block should be a LiquidBlock");

        // The still and flowing forms have to name themselves and each other, or
        // the spread logic — which asks a fluid for its own two forms every tick
        // — sends it nowhere.
        require(source.getSource() == source && source.getFlowing() == flowing,
                "the still fluid should name itself as source and its flowing form as flowing");
        require(source.isSource(source.defaultFluidState()),
                "the still fluid's default state should be a source");
        require(!flowing.isSource(flowing.defaultFluidState()),
                "the flowing fluid's default state should not be a source");

        // The block the fluid becomes in the world has to be the one that was
        // registered: a LiquidBlock built around some other fluid would place
        // that other fluid instead. The block's default state carries a full
        // source of exactly this fluid, which is the link that has to hold.
        require(block.defaultBlockState().getFluidState().getType() == source,
                "the fluid's block should carry the fluid, not some other");

        // The bucket names the fluid, or right-clicking it places nothing.
        Item bucket = ProbeContent.ACID.bucket().orElseThrow().get();
        require(BuiltInRegistries.ITEM.getValue(Identifier.parse("probemod:acid_bucket")) == bucket,
                "the bucket should be in the registry");
        require(bucket instanceof BucketItem, "and it should be a bucket");
        require(source.getBucket() == bucket, "the fluid should name its bucket");
    }

    /**
     * An attachment has to reach the game's classes, and survive a save.
     *
     * <p>Two failures, both silent. The mixin not applying means an entity is
     * not an {@link AttachmentHolder}, and the first cast throws far from here.
     * The save hook not firing means a value set in one session is simply gone
     * the next, which reads as data loss with no cause. So the check is the
     * interface on both target classes, and a real round-trip through a block
     * entity's own save path — the one the mixin injects into.
     *
     * <p>The block entity carries the round-trip because it is the target that
     * builds without a live world; the entity shares the very same storage and
     * the very same injection, so proving the interface reached {@code Entity}
     * is enough for that half.
     */
    private static void checkAttachments() {
        require(AttachmentHolder.class.isAssignableFrom(Entity.class),
                "every entity should be an attachment holder — the mixin adds the interface");
        require(AttachmentHolder.class.isAssignableFrom(BlockEntity.class),
                "and so should every block entity");

        ProblemReporter reporter = ProblemReporter.DISCARDING;
        RegistryAccess.Frozen registries = RegistryAccess.EMPTY;
        BlockState state = ProbeContent.MACHINE.get().defaultBlockState();

        ProbeBlockEntity source = new ProbeBlockEntity(BlockPos.ZERO, state);
        Attachments.set(source, ProbeContent.MANA, 42);
        Attachments.set(source, ProbeContent.WARMED, true);

        TagValueOutput output = TagValueOutput.createWithContext(reporter, registries);
        source.probeSave(output);
        CompoundTag saved = output.buildResult();

        ProbeBlockEntity loaded = new ProbeBlockEntity(BlockPos.ZERO, state);
        loaded.probeLoad(TagValueInput.create(reporter, registries, saved));

        require(Attachments.get(loaded, ProbeContent.MANA) == 42,
                "a persistent attachment should survive save and load");
        require(!Attachments.get(loaded, ProbeContent.WARMED),
                "a transient attachment should not be written, and should read back as its default");
    }

    /**
     * The mod's tab has to land somewhere reachable.
     *
     * <p>Getting this far already proves the harder half: vanilla's bootstrap
     * refuses to start when two tabs share a square, and a mod tab always does
     * — every one of vanilla's fourteen slots is taken.
     */
    private static void checkCreativePage() {
        CreativeModeTab tab = BuiltInRegistries.CREATIVE_MODE_TAB.getValue(ProbeContent.TAB);
        require(tab != null, "the mod's creative tab should be in the registry");

        require(CreativePages.pageOf(tab) == 1,
                "the first mod tab belongs on page 1, since vanilla fills page 0");
        require(CreativePages.count() == 2, "one mod tab means one page beyond vanilla's");

        // Nothing may share a square with a tab on the same page, which is the
        // rule vanilla enforces at bootstrap and Fenix widens rather than drops.
        require(tab.column() < 5,
                "mod tabs take columns 0 to 4; 5 and 6 belong to the tabs that follow the player");

        List<CreativeModeTab> all = BuiltInRegistries.CREATIVE_MODE_TAB.stream().toList();
        require(!CreativePages.onCurrentPage(all).contains(tab),
                "page 0 is vanilla's alone — a mod tab there would push one of vanilla's out");

        CreativePages.turn(1);
        List<CreativeModeTab> page = CreativePages.onCurrentPage(all);
        require(page.contains(tab), "page 1 should hold the mod tab");
        require(page.contains(BuiltInRegistries.CREATIVE_MODE_TAB.getValue(CreativeTabs.SEARCH)),
                "search follows the player to every page — losing it to reach a mod's blocks "
                        + "is the whole reason paging feels bad elsewhere");
        require(page.contains(BuiltInRegistries.CREATIVE_MODE_TAB.getValue(CreativeTabs.INVENTORY)),
                "so does the inventory");
        require(page.size() == 5, "page 1 is the mod's tab plus the four that always travel");
        CreativePages.turn(-1);
    }

    /**
     * A block entity type has to know which blocks carry it.
     *
     * <p>Get that set wrong and nothing complains: the type registers, the
     * block places, and the game silently never creates the block entity — so
     * whatever it was meant to store is simply never there.
     */
    private static void checkBlockEntity() {
        BlockEntityType<?> type =
                BuiltInRegistries.BLOCK_ENTITY_TYPE.getValue(Identifier.parse("probemod:machine"));
        require(type != null, "the block entity type should be in the registry");
        require(type == ProbeContent.MACHINE_TYPE.get(), "the handle should be bound to it");

        require(type.isValid(ProbeContent.MACHINE.get().defaultBlockState()),
                "the type should accept its own block — otherwise the block entity is never created");

        require(type.create(BlockPos.ZERO, ProbeContent.MACHINE.get().defaultBlockState()) != null,
                "the type should be able to build one");

        // A block that does not implement EntityBlock never creates its block
        // entity. That has to be refused out loud, at startup, rather than
        // discovered hours later by a player whose machine forgot everything.
        Registrar spare = Registrar.of("probemod");
        spare.blockEntity("nope", ProbeBlockEntity::new, ProbeContent.RUBY_BLOCK);
        boolean refused = false;
        try {
            spare.apply();
        } catch (IllegalArgumentException expected) {
            refused = true;
        }
        require(refused, "a block that is not an EntityBlock should be refused, loudly, "
                + "rather than never creating its block entity");
    }

    /**
     * A living entity has to have attributes.
     *
     * <p>LivingEntity's constructor asks vanilla's table for them, and an
     * entity that is not in it dies right there with a null map — inside
     * vanilla, nowhere near the mod that registered it. So the check is not
     * that the table contains an entry but that one can actually be built.
     */
    private static void checkEntity() {
        EntityType<?> type =
                BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.parse("probemod:critter"));
        require(type != null, "the entity type should be in the registry");
        require(type == ProbeContent.CRITTER.get(), "the handle should be bound to it");

        require(DefaultAttributes.hasSupplier(type),
                "a living entity without attributes cannot be constructed at all");
        require(DefaultAttributes.getSupplier(ProbeContent.CRITTER.get())
                        .getValue(Attributes.MAX_HEALTH) == 8,
                "the attributes registered should be the ones asked for");
    }

    /**
     * A menu type has to be registered, and reaching this far is most of it.
     *
     * <p>{@code MenuType}'s constructor is private and so is the interface it
     * takes, so a mod cannot build one — Fenix widens both, in the jar the game
     * actually loads. If that transformation ever stops firing, the failure is
     * an {@code IllegalAccessError} thrown out of the mod's field initialiser
     * during bootstrap, which is exactly the kind of thing that is easy to
     * break and impossible to notice until a player opens a chest.
     */
    private static void checkMenu() {
        MenuType<?> type = BuiltInRegistries.MENU.getValue(Identifier.parse("probemod:chest"));
        require(type != null, "the menu type should be in the registry");
        require(type == ProbeContent.CHEST_MENU.get(), "the handle should be bound to it");
    }

    /**
     * A spawn egg has to name its entity, and a mob has to have a placement.
     *
     * <p>Both fail quietly. An egg whose component is missing spawns nothing
     * when right-clicked; a mob with no placement can be summoned and hatched
     * and simply never appears in the world, which reads as a wrong spawn
     * weight rather than as a missing registration.
     */
    private static void checkSpawning() {
        Item egg = BuiltInRegistries.ITEM.getValue(Identifier.parse("probemod:critter_spawn_egg"));
        require(egg != null, "the spawn egg should be in the registry");
        require(egg == ProbeContent.CRITTER_EGG.get(), "the handle should be bound to it");
        require(egg instanceof SpawnEggItem, "and it should be a spawn egg");

        // Getting this far is the check that matters. An egg names its entity
        // by holding the type itself, not a promise of one, so it can only be
        // built once the entity exists — and the registrar's late pass is what
        // arranges that. Declared the other way round and without it, this line
        // is never reached: apply() throws while the handle is still unbound.
        //
        // What the stack actually carries cannot be read here. Components are
        // bound while the game loads datapacks, and a probe stops long before
        // that: until then vanilla's own Items.STONE cannot be made into a
        // stack either.

        // Vanilla answers NO_RESTRICTIONS for a type it has never heard of, so
        // asking for the placement is not enough — it has to be the one asked
        // for.
        require(SpawnPlacements.getPlacementType(ProbeContent.CRITTER.get())
                        == SpawnPlacementTypes.ON_GROUND,
                "the placement registered should be the one asked for");
        require(SpawnPlacements.getHeightmapType(ProbeContent.CRITTER.get())
                        == Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                "and so should the heightmap");
    }

    /** The registries that are one line each, and silent when missed. */
    private static void checkSmallRegistries() {
        require(BuiltInRegistries.PARTICLE_TYPE.getValue(Identifier.parse("probemod:spark"))
                        == ProbeContent.SPARK.get(),
                "the particle type should be in the registry, bound to its handle");

        require(BuiltInRegistries.MOB_EFFECT.getValue(Identifier.parse("probemod:glimmer"))
                        == ProbeContent.GLIMMER.get(),
                "the status effect should be in the registry, bound to its handle");

        DataComponentType<Integer> charge = ProbeContent.CHARGE.get();
        require(BuiltInRegistries.DATA_COMPONENT_TYPE.getValue(Identifier.parse("probemod:charge"))
                        == charge,
                "the data component type should be in the registry, bound to its handle");

        // A component registered without a codec cannot be saved, and that is
        // invisible until a world is reloaded and the state is simply gone.
        require(charge.codec() != null, "a persistent component should have a codec, or it never saves");
    }

    private static void checkSound() {
        SoundEvent sound = BuiltInRegistries.SOUND_EVENT.getValue(Identifier.parse("probemod:chime"));
        require(sound != null, "the sound event should be in the registry");
        require(sound == ProbeContent.CHIME.get(), "the handle should be bound to it");

        // A block set type has to be in vanilla's own table, not merely
        // constructed: BlockSetType.CODEC resolves by name out of that table,
        // and the method that writes to it is private. So this proves the
        // accessible widening reached the game as well as the compiler.
        require(BlockSetType.values().anyMatch(type -> type == ProbeContent.PROBE_SET),
                "the mod's block set type was built but never registered");
        require(ProbeContent.PROBE_SET.canOpenByHand(),
                "the set was asked to open by hand and does not");
        require(BlockSetType.CODEC
                        .parse(com.mojang.serialization.JsonOps.INSTANCE,
                                new com.google.gson.JsonPrimitive("probemod:probe"))
                        .result().orElse(null) == ProbeContent.PROBE_SET,
                "a registered set type should read back from its own name");
    }

    private static void require(boolean condition, String what) {
        if (!condition) {
            throw new AssertionError("registry conformance failed: " + what);
        }
    }
}
