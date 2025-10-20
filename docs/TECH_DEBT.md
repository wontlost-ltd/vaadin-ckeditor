Technical debt audit for ckeditor-vaadin (Vaadin CKEditor 5 add-on)

Scope
- Repository: /home/engine/project
- Purpose: Identify technical debt that increases risk, cost, or friction for users and maintainers of this Vaadin Flow add-on that embeds CKEditor 5
- Result: Actionable list with severity, rationale, and suggested remediations

Executive summary (highest-impact items first)
- Incompatible dev runtime and servlet APIs (High): Jetty 9 (javax.*) is configured while Vaadin 24 uses Jakarta (jakarta.*). Default goal runs jetty:run with Jetty 9, which is not compatible with Flow 24.
- Over-bundling front-end assets (High): All CKEditor translations are statically imported via @JsModule, inflating bundle size and build time for every consumer app.
- Java baseline set to 21 (Medium/High): Compiling with release 21 limits consumers running Java 17 (common for Vaadin 24 LTS). Lowers adoptability.
- Outdated dependencies (Medium): Vaadin 24.5.0, jsoup 1.15.3, gson 2.10, plugin versions (compiler/jar/assembly/bnd) are behind current stable, missing fixes and security patches.
- API correctness issues in Config (High): Wrong keys or fields in multiple config writers (image resize uses "options" instead of "resizeOptions"; table properties assigned incorrectly; simpleUpload headers typed as list not key-values; media providers typed as strings, not objects). These produce silent misconfiguration at runtime.
- Vaadin Component ID semantics overridden (Medium): setId/getId are repurposed to manage an "editorId" custom property, diverging from Vaadin’s element ID semantics and risking integration surprises.
- Missing automated tests and CI (Medium): No unit/integration tests, no workflows. Regressions are likely and consumers have no compatibility guarantees.
- README and release hygiene (Low/Medium): Minimal README and misconfigured SCM entries; assembly expects LICENSE but repo has LICENSE.md. Lowers trust and discoverability.

Details and recommendations

1) Build and dependency management
- Jetty 9 vs Jakarta (High)
  - Where: pom.xml
  - jetty-maven-plugin version 9.4.27.v20200227 depends on javax.servlet.*. Vaadin 24 is Jakarta-based and requires Jetty 11+ (jakarta) when using Jetty.
  - Risks: Local dev/run won’t match consumer environment; runtime errors with jakarta vs javax namespaces.
  - Fix: Remove the jetty plugin entirely (this is a library, not an app), or upgrade to Jetty 11 and use jakarta.servlet-api 5+ for any dev/demo module. Also remove defaultGoal jetty:run.

- javax.servlet-api included (Medium)
  - Where: pom.xml dependency javax.servlet:javax.servlet-api:4.0.1 (scope provided)
  - Risk: Pulls javax.* into classpath, conflicting with Vaadin 24’s jakarta.*. Not used by the code.
  - Fix: Remove this dependency for the core add-on. If needed for a demo module, depend on jakarta.servlet-api 5+ there, not in the library.

- Java toolchain baseline 21 (Medium/High)
  - Where: pom.xml <maven.compiler.release>21</maven.compiler.release>
  - Risk: Consumers still on Java 17 (the standard for Vaadin 24 LTS) cannot use the artifact.
  - Fix: Compile to release 17 for broader compatibility. Test with 17 and 21 in CI.

- Outdated dependencies and plugins (Medium)
  - Vaadin BOM 24.5.0: update to the latest 24 LTS patch (security and bug fixes).
  - jsoup 1.15.3: update to 1.18.x.
  - gson 2.10: update to 2.11.x.
  - maven-compiler-plugin 3.10.1 -> 3.13.x; maven-jar-plugin 3.2.0 -> 3.4.x; maven-assembly-plugin 3.1.1 -> 3.7.x; bnd-maven-plugin 6.2.0 -> current stable.
  - Repositories: remove prereleases/snapshots unless truly needed to reduce resolution uncertainty.

- Default goal is a server run (Medium)
  - Where: pom.xml <defaultGoal>jetty:run</defaultGoal>
  - Risk: Unexpected behavior for contributors; breaks standard Maven flows for a library.
  - Fix: Remove defaultGoal; if you want a demo, place it in a separate demo module with its own run profile.

- SCM metadata incorrect (Low)
  - Where: pom.xml <scm>
  - Values like scm:git:git://https://github.com/... are malformed.
  - Impact: Release automation and tooling (Maven Release, site) won’t work properly.
  - Fix: Use standard values, e.g. connection scm:git:https://github.com/eroself/vaadin-litelement-ckeditor.git and developerConnection scm:git:git@github.com:eroself/vaadin-litelement-ckeditor.git

- build.sh hardcodes JAVA_HOME (Low)
  - Risk: Non-portable; conflicts with toolchains.
  - Fix: Prefer Maven Toolchains or CI configuration instead of shell exporting JAVA_HOME.

2) Front-end assets and bundling
- Massive translation imports (High)
  - Where: VaadinCKEditor.java annotations
  - Every translation file under META-INF/frontend/translations/*.js is imported via @JsModule. This forces all languages into the bundle and slows down app builds and startup.
  - Fix: Load only the needed language dynamically (e.g., on first render use dynamic import() based on config.language). Provide a property to opt-in additional locales. Remove static @JsModule imports for all translations.

- Lit version pin may conflict with consumer (Medium)
  - Where: @NpmPackage(value = "lit", version = "^3.2.1")
  - Vaadin 24 apps may still be on Lit 2.x. Forcing ^3 can cause duplicate copies or conflicts.
  - Fix: Allow both major versions or avoid pinning if Vaadin BOM provides Lit. Options: widen to >=2.8 <4; or drop and rely on Vaadin’s lit dependency. Document the requirement clearly.

- Global window state with no cleanup (Medium)
  - Where: vaadin-ckeditor.js, vaadin-ckeditor-utils.js
  - serverMap, editorMap, MutationObservers, etc. are stored on window without removing on component detach. Leads to memory leaks when components are created/destroyed.
  - Fix: Implement disconnectedCallback() in VaadinCKEditor web component to unregister observers, delete maps entries, and detach listeners.

- Unscoped DOM queries (Medium)
  - Where: vaadin-ckeditor.js uses document.querySelector("#toolbar-container") and other global selectors
  - Risk: Multiple editors on one page clobber each other.
  - Fix: Scope queries to the component’s DOM (or use unique containers per instance) and avoid globals.

- Debug logs left in production (Low)
  - Where: vaadin-ckeditor-utils.js (e.g., console.log("=============>")) and vaadin-ckeditor.js
  - Fix: Remove or guard behind a debug flag.

3) Java API design and correctness
- setId()/getId() repurposed (Medium)
  - Where: VaadinCKEditor.java
  - Behavior diverges from Vaadin’s Component id semantics by writing a custom property editorId and returning that from getId(). This can break theming, testing, and interoperability.
  - Fix: Do not override setId/getId for custom identifiers. Introduce setEditorId/getEditorId, keep standard id semantics by calling super.setId(). Maintain backward compatibility by deprecating the overload and forwarding to new API.

- clear() does not clear the server model (Medium)
  - Where: VaadinCKEditor.java clear()
  - It only sends updateValue(null) to the client; it does not update editorData or model value.
  - Fix: Call super.clear() or setModelValue("", false) and synchronize editorData accordingly.

- Null handling and typos (Low/Medium)
  - getContentText() will NPE if editorData is null; guard with Optional.ofNullable.
  - Method name typo: isSynchronizd(Boolean) should be isSynchronized(Boolean). Keep the old method as deprecated alias.
  - Logger names typos (vaddinCKEditorLog) and inconsistent logging levels.

- HasConfig#getConfig robustness (Low)
  - Uses getElement().getProperty("config") and Json.parse without null checks. Also, setPropertyJson is used to write; use getPropertyJson to read.

- Config writer bugs (High)
  - setImage: uses key "options" for resize options. CKEditor expects "resizeOptions" under image config.
  - setTable: table.put("tableProperties", tableCellProperties) — wrong variable. Should use tableProperties.
  - setSimpleUpload: headers should be an object map of headerName -> value, not a list.
  - setMediaEmbed: providers/extraProviders/removeProviders should be arrays of provider objects (pattern/matcher/url/HTML) rather than string literals.
  - setFontBackgroundColor/setFontColor: TODO left; colors array is not implemented — features do nothing.
  - Recommendation: Add unit tests for each config method to assert produced JSON matches CKEditor docs.

- Eventing and autosave
  - VaadinCKEditorAction stores actions in a static map with no unregister and no concurrency protection. Consider ConcurrentHashMap and provide unregisterAction().

4) Testing and quality
- No unit tests or integration tests (Medium)
  - Add tests for Config serialization (simple, fast wins) and basic component lifecycle tests (e.g., CustomField contract, clear(), read-only toggling). Consider Playwright + Vaadin testbench alternatives only if needed.

- No CI workflows (Medium)
  - Add GitHub Actions matrix for JDK 17 and 21, Maven verify, and a job to build the Directory ZIP profile.

5) Documentation, examples, and release hygiene
- README is minimal (Low/Medium)
  - Missing quick-start, compatibility matrix (Vaadin version, Java baseline), API highlights and code snippets. The Vaadin Directory badge URL still has placeholders.

- License packaging mismatch (Low)
  - assembly/assembly.xml includes LICENSE, but repo has LICENSE.md; license is not packaged.
  - Fix: Rename to LICENSE or update the assembly descriptor include to LICENSE.md.

- SCM and versioning (Low)
  - Fix malformed scm entries.
  - Add CHANGELOG.md with notable changes, breaking changes, and upgrade notes.

6) OSGi packaging (Low)
- bnd.bnd exports no packages (Export-Package: !*). If OSGi consumption is intended, revisit exports. If not, consider removing the OSGi bits to reduce maintenance surface, or confirm minimal metadata is correct.

7) Security and compliance
- Dependency updates (jsoup, gson, Vaadin LTS patch) for CVEs.
- CKEditor licensing: ensure bundled CKEditor build and translations comply with their license, include notices in the assembled ZIP if required.

Quick wins (suggested order)
1) Remove defaultGoal and the Jetty 9 plugin from the library POM; or move to a separate demo module with Jetty 11 (Jakarta).
2) Lower Java release to 17 for the library artifact. Validate on 17 and 21 in CI.
3) Upgrade Vaadin BOM to latest 24.x LTS patch; bump jsoup and gson to current stable.
4) Fix Config bugs: setImage("resizeOptions"), setTable(tableProperties), simpleUpload headers as map, and add tests that assert produced JSON.
5) Stop importing all translations statically. Load only the selected UI language at runtime via dynamic import.
6) Restore Vaadin ID semantics: add setEditorId/getEditorId and deprecate current setId/getId override.
7) Add basic CI (mvn -B -ntp verify on JDK 17 and 21) and unit tests for Config JSON generation.

Longer-term improvements
- Front-end refactor to support multiple editor instances safely: local state, disconnectedCallback cleanup, and scoping DOM queries.
- TypeScript source and build pipeline for the web component, rather than committing large prebuilt JS bundles.
- Expand documentation: examples for classic/balloon/inline/decoupled, autosave hooks, HTML sanitization, and CSS overrides.
- Consider not pinning Lit or pin to a range compatible with Vaadin 24; document compatibility with Vaadin 24.x and 24 LTS.

References (files/lines)
- pom.xml: jetty plugin, servlet-api, Java release, scm
- src/main/java/com/wontlost/ckeditor/Config.java: setImage, setTable, setSimpleUpload, setMediaEmbed, setFont*Color
- src/main/java/com/wontlost/ckeditor/VaadinCKEditor.java: setId/getId override, clear(), getContentText(), isSynchronizd()
- src/main/resources/META-INF/frontend: translation files imported via @JsModule, window global maps, global selectors
- assembly/assembly.xml and LICENSE.md mismatch
