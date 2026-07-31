package fr.d4emon.fenix.mixin.event;

import net.minecraft.resources.Identifier;
import net.minecraft.util.context.ContextKeySet;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;
import java.util.Optional;

/**
 * Reads what a loot table is made of.
 *
 * <p>A loot table keeps its pools, its functions, its parameter set and its
 * random sequence private, and its builder only accepts pools that have not been
 * built yet. So adding a pool to a table that already exists means reading the
 * four out and constructing a new one — which is what {@code LootTables} does,
 * and this is how it reads them.
 */
@Mixin(LootTable.class)
public interface LootTableAccessor {

    /** {@return the pools this table rolls} */
    @Accessor("pools")
    List<LootPool> fenix$pools();

    /** {@return the functions applied to everything it produces} */
    @Accessor("functions")
    List<LootItemFunction> fenix$functions();

    /** {@return which context a roll of this table needs} */
    @Accessor("paramSet")
    ContextKeySet fenix$paramSet();

    /** {@return the sequence its randomness is drawn from, if it names one} */
    @Accessor("randomSequence")
    Optional<Identifier> fenix$randomSequence();
}
