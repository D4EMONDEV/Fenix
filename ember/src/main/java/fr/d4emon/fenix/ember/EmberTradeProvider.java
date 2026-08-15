package fr.d4emon.fenix.ember;

import fr.d4emon.fenix.registry.Holder;

import java.util.ArrayList;
import java.util.List;

/**
 * Writes villager trades, and the sets a profession draws them from.
 *
 * <p>26.2 made trading data. A profession's offers are no longer a table in
 * code: each trade is a file, and a trade set says how many of them a villager
 * of that profession offers at a given level.
 *
 * <pre>{@code
 * @Generator
 * public final class ModTrades extends EmberTradeProvider {
 *     @Override
 *     protected void trades() {
 *         trade("buy_ruby").wants("minecraft:emerald", 1)
 *                 .gives(ModItems.RUBY).maxUses(12).save();
 *         trade("sell_ruby").wants(ModItems.RUBY, 1)
 *                 .gives("minecraft:emerald").maxUses(12).save();
 *
 *         tradeSet("jeweller").amount(2).trades("buy_ruby", "sell_ruby").save();
 *     }
 * }
 * }</pre>
 *
 * <p>The two halves fail differently, and both quietly. A trade nothing names
 * is a file the game loads and never offers; a set naming a trade that is not
 * there is a villager with fewer offers than intended, and no complaint about
 * the missing one.
 */
public abstract class EmberTradeProvider extends EmberProvider {

    /** For subclasses. */
    protected EmberTradeProvider() {
    }

    /** Describes the trades. */
    protected abstract void trades();

    @Override
    protected final void run() {
        trades();
    }

    /**
     * Starts one trade.
     *
     * @param name the path part of its id
     * @return a builder; call {@code save()} when done
     */
    protected final Trade trade(String name) {
        return new Trade(this, name);
    }

    /**
     * Starts a set: how many trades a profession offers, and which.
     *
     * @param name the path part of its id, usually the profession's name
     * @return a builder; call {@code save()} when done
     */
    protected final Set tradeSet(String name) {
        return new Set(this, name);
    }

    private void save(String directory, String name, String json) {
        output().data(directory + "/" + name + ".json", json);
    }

    /** Collects one trade. */
    public static final class Trade {

        private final EmberTradeProvider provider;
        private final String name;

        private String wantsId;
        private int wantsCount = 1;
        private String alsoWantsId;
        private int alsoWantsCount = 1;
        private String givesId;
        private int givesCount = 1;
        private int maxUses = 12;
        private int xp;
        private Float discount;

        private Trade(EmberTradeProvider provider, String name) {
            this.provider = provider;
            this.name = name;
        }

        /**
         * What the player hands over.
         *
         * @param item  the item
         * @param count how many
         * @return this builder
         */
        public Trade wants(Holder<?> item, int count) {
            return wants(EmberOutput.idOf(item.get()).toString(), count);
        }

        /**
         * What the player hands over, by id.
         *
         * @param id    the item's id, for vanilla content
         * @param count how many
         * @return this builder
         */
        public Trade wants(String id, int count) {
            this.wantsId = id;
            this.wantsCount = count;
            return this;
        }

        /**
         * A second thing the player hands over, as the librarian's book is.
         *
         * @param id    the item's id
         * @param count how many
         * @return this builder
         */
        public Trade alsoWants(String id, int count) {
            this.alsoWantsId = id;
            this.alsoWantsCount = count;
            return this;
        }

        /**
         * What the villager hands back.
         *
         * @param item the item
         * @return this builder
         */
        public Trade gives(Holder<?> item) {
            return gives(EmberOutput.idOf(item.get()).toString(), 1);
        }

        /**
         * What the villager hands back, by id.
         *
         * @param id the item's id, for vanilla content
         * @return this builder
         */
        public Trade gives(String id) {
            return gives(id, 1);
        }

        /**
         * What the villager hands back, in a quantity.
         *
         * @param id    the item's id
         * @param count how many
         * @return this builder
         */
        public Trade gives(String id, int count) {
            this.givesId = id;
            this.givesCount = count;
            return this;
        }

        /**
         * How many times it can be used before the villager restocks.
         *
         * @param uses the count; vanilla is usually 12 or 16
         * @return this builder
         */
        public Trade maxUses(int uses) {
            this.maxUses = uses;
            return this;
        }

        /**
         * How much the villager levels up for each use.
         *
         * @param amount the experience
         * @return this builder
         */
        public Trade xp(int amount) {
            this.xp = amount;
            return this;
        }

        /**
         * How much a good reputation lowers the price.
         *
         * @param fraction the discount, as a fraction; vanilla uses 0.05 to 0.2
         * @return this builder
         */
        public Trade reputationDiscount(float fraction) {
            this.discount = fraction;
            return this;
        }

        /** Writes the trade. */
        public void save() {
            if (wantsId == null || givesId == null) {
                throw new IllegalStateException(
                        name + " needs both something wanted and something given");
            }

            StringBuilder json = new StringBuilder("{\n");
            if (alsoWantsId != null) {
                json.append("  \"additional_wants\": {\n")
                        .append("    \"count\": ").append(EmberOutput.decimal(alsoWantsCount))
                        .append(",\n    \"id\": ").append(EmberOutput.quote(alsoWantsId))
                        .append("\n  },\n");
            }
            json.append("  \"gives\": {\n");
            if (givesCount != 1) {
                json.append("    \"count\": ").append(EmberOutput.decimal(givesCount))
                        .append(",\n");
            }
            json.append("    \"id\": ").append(EmberOutput.quote(givesId)).append("\n  },\n")
                    .append("  \"max_uses\": ").append(EmberOutput.decimal(maxUses)).append(",\n");
            if (discount != null) {
                json.append("  \"reputation_discount\": ").append(EmberOutput.decimal(discount))
                        .append(",\n");
            }
            json.append("  \"wants\": {\n")
                    .append("    \"count\": ").append(EmberOutput.decimal(wantsCount))
                    .append(",\n    \"id\": ").append(EmberOutput.quote(wantsId))
                    .append("\n  }");
            if (xp > 0) {
                json.append(",\n  \"xp\": ").append(EmberOutput.decimal(xp));
            }
            json.append("\n}\n");

            provider.save("villager_trade", name, json.toString());
        }
    }

    /** Collects one trade set. */
    public static final class Set {

        private final EmberTradeProvider provider;
        private final String name;
        private final List<String> trades = new ArrayList<>();
        private int amount = 2;

        private Set(EmberTradeProvider provider, String name) {
            this.provider = provider;
            this.name = name;
        }

        /**
         * How many of the trades a villager offers.
         *
         * @param count the number drawn from the list
         * @return this builder
         */
        public Set amount(int count) {
            this.amount = count;
            return this;
        }

        /**
         * Which trades it draws from.
         *
         * <p>Names in the mod's own namespace unless they already carry one.
         *
         * @param names the trades' names
         * @return this builder
         */
        public Set trades(String... names) {
            trades.addAll(List.of(names));
            return this;
        }

        /** Writes the set. */
        public void save() {
            if (trades.isEmpty()) {
                throw new IllegalStateException(
                        name + " draws from no trades, so the villager would offer none");
            }

            StringBuilder json = new StringBuilder("{\n")
                    .append("  \"amount\": ").append(EmberOutput.decimal(amount)).append(",\n")
                    .append("  \"trades\": [");
            String between = "\n    ";
            for (String trade : trades) {
                json.append(between).append(EmberOutput.quote(
                        trade.contains(":") ? trade : provider.modId() + ":" + trade));
                between = ",\n    ";
            }
            json.append("\n  ]\n}\n");

            provider.save("trade_set", name, json.toString());
        }
    }
}
