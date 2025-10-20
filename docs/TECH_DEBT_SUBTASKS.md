Tech debt remediation plan – Subtasks

Scope
- This plan breaks down the remediation items listed in docs/TECH_DEBT.md into actionable subtasks.
- Each subtask includes priority, acceptance criteria, and notes on dependencies.

Legend
- Priority: P1 (highest), P2, P3

EPIC A: Build and dependency modernization

A1 Remove Jetty 9 plugin and defaultGoal from library POM (P1)
- Context: Vaadin 24 is Jakarta-based; Jetty 9 (javax.*) is incompatible. Library POM should not run a server.
- Steps:
  - Remove <defaultGoal>jetty:run</defaultGoal>.
  - Remove org.eclipse.jetty:jetty-maven-plugin from pom.xml.
- Acceptance criteria:
  - mvn -q -DskipTests package completes without attempting to start Jetty.
  - No javax.* servlet classes are brought by plugins.
- Dependencies: None.

A2 Remove javax.servlet-api from dependencies (P1)
- Steps:
  - Delete javax.servlet:javax.servlet-api from pom.xml (library should be container-agnostic).
- Acceptance criteria:
  - Project compiles and packages without javax.* on the classpath.
- Dependencies: None.

A3 Set Java release compatibility to 17 (P1)
- Steps:
  - Change <maven.compiler.release> and/or maven-compiler-plugin <release> to 17.
- Acceptance criteria:
  - Artifact builds on JDK 17 and 21; classfile target is 17.
- Dependencies: None.

A4 Update dependency and plugin versions to current stable (P2)
- Steps:
  - Update Vaadin BOM to latest 24.x LTS patch.
  - Update jsoup to 1.18.x; gson to 2.11.x.
  - Update maven-compiler-plugin, maven-jar-plugin, maven-assembly-plugin, bnd-maven-plugin to latest stable.
- Acceptance criteria:
  - mvn -q -DskipTests verify succeeds; no dependency convergence errors.
- Dependencies: A3.

A5 Clean up repositories (P2)
- Steps:
  - Remove prereleases and snapshot repositories unless required.
- Acceptance criteria:
  - Build succeeds using Maven Central and Vaadin Directory only (unless specific artifacts require others).
- Dependencies: A4.

A6 Fix SCM metadata (P3)
- Steps:
  - Correct <scm> connection, developerConnection, and url to valid Git URLs.
- Acceptance criteria:
  - mvn help:effective-pom shows correct SCM URLs.
- Dependencies: None.

A7 Make build.sh portable or remove (P3)
- Steps:
  - Remove hardcoded JAVA_HOME export; add a note to use Maven Toolchains or CI config.
- Acceptance criteria:
  - build.sh (if kept) does not mutate JAVA_HOME; or the file is removed.
- Dependencies: None.

A8 Fix assembly LICENSE packaging (P2)
- Steps:
  - Either rename LICENSE.md to LICENSE, or update assembly/assembly.xml to include LICENSE.md.
- Acceptance criteria:
  - Directory ZIP contains the license file.
- Dependencies: None.

EPIC B: Front-end asset loading and lifecycle

B1 Load translations dynamically instead of static @JsModule imports (P1)
- Steps:
  - Remove @JsModule imports for META-INF/frontend/translations/*.js from VaadinCKEditor.java.
  - In web component (vaadin-ckeditor.js), when config.language is set, dynamically import the corresponding translation module (e.g., import('./translations/${lang}.js')).
  - Provide fallback to en if missing.
- Acceptance criteria:
  - Only the selected language file is loaded at runtime; bundle size is reduced (no static inclusion of all translations).
  - Editor UI renders in selected language.
- Dependencies: None.

B2 Implement disconnectedCallback cleanup (P1)
- Steps:
  - In vaadin-ckeditor.js, add disconnectedCallback to: remove event listeners, disconnect MutationObservers, delete entries in window.vaadinCKEditor.serverMap/editorMap/sourceDataObserverMap for this editorId.
- Acceptance criteria:
  - Creating and removing editors repeatedly does not grow window maps or observer counts; no memory leaks observed in dev tools.
- Dependencies: None.

B3 Scope DOM queries to component instance (P2)
- Steps:
  - Avoid document.querySelector for shared IDs (e.g., '#toolbar-container'); ensure the container is within the component template and selected relative to the component root.
  - Ensure multiple decoupled editors can coexist.
- Acceptance criteria:
  - Two editors on the same view do not interfere with each other’s toolbars/minimaps.
- Dependencies: B2.

B4 Remove or guard debug logging (P3)
- Steps:
  - Remove console.log statements from utils and component, or guard behind a debug flag property.
- Acceptance criteria:
  - No unexpected console output in production usage.
- Dependencies: None.

B5 Revisit @NpmPackage lit pin (P3)
- Steps:
  - Consider removing @NpmPackage("lit") or widening the version range to be compatible with Vaadin 24 (Lit 2.x) as well as Lit 3 when appropriate.
- Acceptance criteria:
  - No duplicate Lit versions in the consuming app; documentation states compatibility expectations.
- Dependencies: Coordination with Vaadin BOM.

EPIC C: Java API correctness and ergonomics

C1 Restore Vaadin ID semantics and add editorId API (P1)
- Steps:
  - Do not override setId/getId to manage "editorId". Add setEditorId(String)/getEditorId() that set a custom property while calling super.setId() for real component id.
  - Keep current behavior temporarily with @Deprecated methods or internal mapping for backward compatibility.
  - Update VaadinCKEditorBuilder to use setEditorId instead of setId.
- Acceptance criteria:
  - Component id behaves like standard Vaadin components; custom editorId is still used by the client element.
  - Backward compatibility: existing apps still work.
- Dependencies: B2 if client relies on editorId mapping.

C2 clear() should clear model and client state (P1)
- Steps:
  - Implement clear() to setModelValue("", false), update editorData, and call updateValue("").
- Acceptance criteria:
  - After clear(), getValue() is empty and client UI is cleared.
- Dependencies: None.

C3 Null-safety and typo fixes (P2)
- Steps:
  - getContentText(): handle null editorData safely (return empty string).
  - Rename isSynchronizd(Boolean) -> isSynchronized(Boolean), keep deprecated alias delegating to the new method.
  - Fix logger variable names for consistency.
- Acceptance criteria:
  - No NPE from getContentText() when value is null.
  - isSynchronized(Boolean) available; old method still functions but is annotated @Deprecated.
- Dependencies: None.

C4 HasConfig#getConfig robustness (P2)
- Steps:
  - Read config via getElement().getPropertyJson("config") and null-check before parsing; default to new Config() if absent.
- Acceptance criteria:
  - getConfig() never throws due to null JSON property.
- Dependencies: None.

C5 VaadinCKEditorAction concurrency and lifecycle (P3)
- Steps:
  - Use ConcurrentHashMap for actionRegister; add unregisterAction(name).
- Acceptance criteria:
  - Thread-safe registration/unregistration; default autosave action still present when missing.
- Dependencies: None.

EPIC D: Config JSON generation correctness

D1 setImage: use correct keys (P1)
- Steps:
  - Change image.put("options", ...) to image.put("resizeOptions", ...).
- Acceptance criteria:
  - Generated config matches CKEditor docs for image resize options.
- Dependencies: Add unit test D7.

D2 setTable: fix tableProperties assignment (P1)
- Steps:
  - Use the method parameter tableProperties instead of tableCellProperties when putting "tableProperties" key.
- Acceptance criteria:
  - Both table cell and table properties objects are correctly serialized.
- Dependencies: Add unit test D7.

D3 setSimpleUpload: headers should be a map (P2)
- Steps:
  - Change method signature to accept Map<String,String> headers and serialize as an object.
- Acceptance criteria:
  - Headers appear as JSON object, not array, in resulting config.
- Dependencies: API change; document in CHANGELOG; add unit test D7.

D4 setMediaEmbed: provider structures (P3)
- Steps:
  - Consider extending API to accept provider objects (pattern/url/html callbacks) per CKEditor docs; keep current simple list as minimal for now.
- Acceptance criteria:
  - At least no invalid structures are produced; document limitations.
- Dependencies: Documentation update.

D5 setFontBackgroundColor/setFontColor: implement colors array (P2)
- Steps:
  - Accept List<Map<String,String>> or typed model and serialize to array of objects.
- Acceptance criteria:
  - Colors are correctly reflected in the config.
- Dependencies: Add unit test D7.

D6 Add licenseKey configuration passthrough validation (P3)
- Steps:
  - Ensure setLicenseKey trims and rejects blanks; already present; add test.
- Acceptance criteria:
  - Unit test ensures empty/blank values are ignored.
- Dependencies: D7.

D7 Unit tests for Config serialization (P1)
- Steps:
  - Add JUnit tests asserting JSON for: setImage, setTable, setSimpleUpload, setFont*Color, setAlignment, setLanguage, pagination.
- Acceptance criteria:
  - mvn -q test passes; tests cover the above methods.
- Dependencies: Add test framework (EPIC E).

EPIC E: Testing and CI

E1 Add test framework (P1)
- Steps:
  - Add JUnit 5 and AssertJ (or Hamcrest) dependencies in test scope; configure surefire plugin.
- Acceptance criteria:
  - mvn -q test runs tests.
- Dependencies: None.

E2 Basic GitHub Actions CI (P2)
- Steps:
  - Add workflow to run mvn -B -ntp verify on JDK 17 and 21, cache ~/.m2.
- Acceptance criteria:
  - PRs and pushes run CI matrix and report status.
- Dependencies: A3, E1. Note: Only implement if project allows CI workflow changes.

EPIC F: Documentation and release hygiene

F1 Improve README (P2)
- Steps:
  - Add quick start, compatibility matrix (Vaadin 24, Java 17+), examples for classic/balloon/inline/decoupled, autosave, sanitization, CSS override.
  - Fix badges (Vaadin Directory, FOSSA) and links.
- Acceptance criteria:
  - README answers "How to install?", "How to use?", "What versions?".
- Dependencies: B1, C1 for any API name changes.

F2 Add CHANGELOG.md (P2)
- Steps:
  - Document changes, especially API adjustments (e.g., isSynchronized, simpleUpload headers type).
- Acceptance criteria:
  - Changelog exists and is updated for the next release.
- Dependencies: D3, C1, C3.

F3 Add UPGRADE.md (P3)
- Steps:
  - Provide guidance on migrating from previous versions (ID semantics, image config key changes).
- Acceptance criteria:
  - Users can upgrade with minimal friction.
- Dependencies: F2.

EPIC G: OSGi packaging

G1 Clarify OSGi intent (P3)
- Steps:
  - If OSGi is not a target, remove bnd.bnd and bnd plugin; otherwise, define proper Export-Package entries for public APIs.
- Acceptance criteria:
  - Either no OSGi clutter in the artifact, or correct exports are present.
- Dependencies: A4.

Proposed execution order (by priority and coupling)
- Wave 1 (P1): A1, A2, A3, D1, D2, C1, C2, E1, D7, B2, B1
- Wave 2 (P2): A4, A8, C3, C4, D3, D5, F1, F2, B3
- Wave 3 (P3): A5, A6, A7, B4, B5, C5, D4, D6, E2, F3, G1

Notes
- API changes (C1, D3) require clear documentation (F2/F3) and a minor version bump following SemVer.
- Front-end translation loading (B1) reduces bundle size for all consumers; ensure lazy import path matches META-INF/frontend/translations/*.
