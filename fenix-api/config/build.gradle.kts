plugins {
    id("fenix.api-module")
}

description = "Typed configuration, backed by records."

dependencies {
    // Provided by the game: the vanilla classpath already carries Gson, and a
    // second copy inside the API would be one more thing to keep in step.
    compileOnly(libs.gson)

    // Reading and writing records is pure logic, so unlike the rest of the API
    // it can be tested without starting a game -- but the tests need the JSON
    // the game would otherwise have provided.
    testImplementation(libs.gson)
}
