# Contributing to Pigeon

# Repository Structure

Here's a rundown of the structure for Pigeon
repositories.

* BetaMappings
  * `client.tsrg` contains TSRG mappings for
    Minecraft Beta 1.7.3
* Pigeon
  * `buildSrc` contains a Gradle Plugin to convert
    `client.tsrg` to SRG and apply the mappings
    to a Vanilla Minecraft Beta 1.7.3 jar
  * `src` contains the source for the Launcher &
    Mod Loader, with Mixins to implement the API.
    * `pigeon/launcher` contains the code to
      launch Minecraft using LegacyLauncher with
      the Mixin environment.
