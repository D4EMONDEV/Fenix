package fr.d4emon.fenix.loader.metadata;

import fr.d4emon.fenix.api.ModInfo;
import fr.d4emon.fenix.api.Side;
import fr.d4emon.fenix.api.Version;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModMetadataReaderTest {

    private static final String SOURCE = "example-mod.jar";

    private static ModMetadata read(String json) {
        return ModMetadataReader.read(json, SOURCE);
    }

    private static InvalidMetadataException readExpectingFailure(String json) {
        return assertThrows(InvalidMetadataException.class, () -> read(json));
    }

    @Nested
    @DisplayName("a well-formed file")
    class Valid {

        @Test
        void readsTheMinimum() {
            ModMetadata metadata = read("""
                    {
                      "schema": 1,
                      "id": "example-mod",
                      "version": "1.0.0"
                    }
                    """);

            assertEquals("example-mod", metadata.id());
            assertEquals(new Version(1, 0, 0), metadata.version());
        }

        @Test
        @DisplayName("absent optional fields become empty rather than null")
        void fillsInDefaults() {
            ModMetadata metadata = read("""
                    {
                      "schema": 1,
                      "id": "example-mod",
                      "version": "1.0.0"
                    }
                    """);

            assertEquals("example-mod", metadata.name());
            assertEquals("", metadata.description());
            assertEquals("", metadata.license());
            assertEquals(List.of(), metadata.authors());
            assertEquals(Map.of(), metadata.contact());
            assertEquals(List.of(), metadata.depends());
            assertEquals(List.of(), metadata.mixins());
            assertEquals(ModSide.BOTH, metadata.side());
        }

        @Test
        void readsEveryField() {
            ModMetadata metadata = read("""
                    {
                      "schema": 1,
                      "id": "example-mod",
                      "version": "1.2.3-rc.1",
                      "name": "Example Mod",
                      "description": "A sample.",
                      "authors": ["D4EMON", "Somebody Else"],
                      "license": "Apache-2.0",
                      "contact": {
                        "homepage": "https://example.com",
                        "issues": "https://example.com/issues"
                      },
                      "side": "client",
                      "depends": {
                        "fenix": ">=0.1.0",
                        "minecraft": "~26.2"
                      },
                      "mixins": ["example-mod.mixins.json"]
                    }
                    """);

            assertEquals("Example Mod", metadata.name());
            assertEquals("A sample.", metadata.description());
            assertEquals(List.of("D4EMON", "Somebody Else"), metadata.authors());
            assertEquals("Apache-2.0", metadata.license());
            assertEquals("https://example.com", metadata.contact().get("homepage"));
            assertEquals(ModSide.CLIENT, metadata.side());
            assertEquals(List.of("example-mod.mixins.json"), metadata.mixins());

            assertEquals(2, metadata.depends().size());
            ModDependency fenix = metadata.depends().getFirst();
            assertEquals("fenix", fenix.id());
            assertTrue(fenix.isSatisfiedBy(new Version(0, 2, 0)));
            assertFalse(fenix.isSatisfiedBy(new Version(0, 0, 9)));
        }

        @Test
        @DisplayName("the declared side decides where the mod loads")
        void mapsSideOntoTheRunningProcess() {
            ModMetadata metadata = read("""
                    {"schema": 1, "id": "client-only", "version": "1.0.0", "side": "client"}
                    """);

            assertTrue(metadata.side().includes(Side.CLIENT));
            assertFalse(metadata.side().includes(Side.SERVER));
        }

        @Test
        @DisplayName("the public view drops what only the loader needs")
        void narrowsToModInfo() {
            ModInfo info = read("""
                    {
                      "schema": 1,
                      "id": "example-mod",
                      "version": "1.0.0",
                      "name": "Example Mod",
                      "depends": {"fenix": "*"}
                    }
                    """).toModInfo();

            assertEquals("example-mod", info.id());
            assertEquals("Example Mod", info.name());
        }
    }

    @Nested
    @DisplayName("a broken file names the jar and the field")
    class Invalid {

        @Test
        void reportsMalformedJson() {
            assertTrue(readExpectingFailure("{ not json").getMessage().contains(SOURCE));
        }

        @Test
        void reportsSomethingThatIsNotAnObject() {
            assertTrue(readExpectingFailure("[]").getMessage().contains("must contain a JSON object"));
            assertTrue(readExpectingFailure("").getMessage().contains("must contain a JSON object"));
        }

        @Test
        void reportsAMissingRequiredField() {
            assertTrue(readExpectingFailure("""
                    {"schema": 1, "version": "1.0.0"}
                    """).getMessage().contains("'id'"));

            assertTrue(readExpectingFailure("""
                    {"schema": 1, "id": "example-mod"}
                    """).getMessage().contains("'version'"));

            assertTrue(readExpectingFailure("""
                    {"id": "example-mod", "version": "1.0.0"}
                    """).getMessage().contains("'schema'"));
        }

        @Test
        @DisplayName("a future schema says which side has to be upgraded")
        void reportsAnUnsupportedSchema() {
            String message = readExpectingFailure("""
                    {"schema": 99, "id": "example-mod", "version": "1.0.0"}
                    """).getMessage();

            assertTrue(message.contains("99"), message);
            assertTrue(message.contains("newer version of Fenix"), message);
        }

        @Test
        void reportsAnInvalidId() {
            String message = readExpectingFailure("""
                    {"schema": 1, "id": "Example_Mod", "version": "1.0.0"}
                    """).getMessage();

            assertTrue(message.contains("Example_Mod"), message);
            assertTrue(message.contains("not a valid mod id"), message);
        }

        @Test
        void reportsAnUnparsableVersion() {
            String message = readExpectingFailure("""
                    {"schema": 1, "id": "example-mod", "version": "not-a-version"}
                    """).getMessage();

            assertTrue(message.contains("'version'"), message);
            assertTrue(message.contains("not-a-version"), message);
        }

        @Test
        void reportsAnUnknownSide() {
            String message = readExpectingFailure("""
                    {"schema": 1, "id": "example-mod", "version": "1.0.0", "side": "everywhere"}
                    """).getMessage();

            assertTrue(message.contains("'side'"), message);
            assertTrue(message.contains("everywhere"), message);
        }

        @Test
        void reportsTheOffendingDependency() {
            String message = readExpectingFailure("""
                    {
                      "schema": 1, "id": "example-mod", "version": "1.0.0",
                      "depends": {"fenix": ">=0.1.0", "minecraft": "not-a-constraint"}
                    }
                    """).getMessage();

            assertTrue(message.contains("depends.minecraft"), message);
            assertTrue(message.contains("not-a-constraint"), message);
        }

        @Test
        void reportsADependencyKeyThatIsNotAModId() {
            assertTrue(readExpectingFailure("""
                    {
                      "schema": 1, "id": "example-mod", "version": "1.0.0",
                      "depends": {"Fenix": "*"}
                    }
                    """).getMessage().contains("not a mod id"));
        }

        @Test
        void reportsFieldsOfTheWrongType() {
            assertTrue(readExpectingFailure("""
                    {"schema": 1, "id": 42, "version": "1.0.0"}
                    """).getMessage().contains("'id' must be a string"));

            assertTrue(readExpectingFailure("""
                    {"schema": "one", "id": "example-mod", "version": "1.0.0"}
                    """).getMessage().contains("'schema' must be a number"));

            assertTrue(readExpectingFailure("""
                    {"schema": 1, "id": "example-mod", "version": "1.0.0", "authors": "D4EMON"}
                    """).getMessage().contains("'authors' must be an array"));

            assertTrue(readExpectingFailure("""
                    {"schema": 1, "id": "example-mod", "version": "1.0.0", "authors": [1, 2]}
                    """).getMessage().contains("'authors[0]' must be a string"));

            assertTrue(readExpectingFailure("""
                    {"schema": 1, "id": "example-mod", "version": "1.0.0", "depends": []}
                    """).getMessage().contains("'depends' must be an object"));
        }

        @Test
        @DisplayName("the exception carries the source separately, for grouped reporting")
        void exposesTheSource() {
            assertEquals(SOURCE, readExpectingFailure("[]").source());
        }
    }
}
