# Flux Launcher — Release & Install Guide

Everything you need to install `flux.apk` on your phone, cut a new release, and
understand why the **first** install after this guide requires a one-time uninstall.

---

## 1. Download and install `flux.apk`

1. Open the repo's **Releases** page:
   <https://github.com/5r1n1dh1/niagara_clone/releases>
2. Open the release you want (latest is **v1.0.1**) and download the single asset
   named **`flux.apk`**.
3. Copy it to your phone (or download directly with the phone's browser) and open it.
4. Android will ask permission to install apps from an unknown source — allow it for
   the app you are installing from (Files / Chrome / etc.).
5. Tap **Install**.

> The asset has always been one fixed name: `flux.apk`. Earlier releases used
> `app-release.apk`; the v1.0.1 asset has been renamed so the download link stays
> stable (`.../releases/download/v1.0.1/flux.apk`).

### APK details

| Property | Value |
| --- | --- |
| Asset name | `flux.apk` |
| applicationId | `com.aistudio.fluxlauncher.vknwre` (**never change this**) |
| Signing alias | `flux-release` (RSA 2048) |
| minSdk / targetSdk | 24 / 36 |

---

## 2. ⚠️ ONE-TIME UNINSTALL (required the first time only)

The v1.0.1 APK you may already have installed was signed with an **old signing key
that no longer exists** — the keystore was ephemeral and was lost when the machine
that built it was wiped. Since Android refuses to install an APK whose signing
certificate differs from the installed one, you must do the following **once**:

1. **Uninstall** the currently installed Flux Launcher
   (Settings → Apps → Flux Launcher → Uninstall, or long-press the icon → Uninstall).
2. Install `flux.apk` normally (see §1).
3. **Done.** From this point on, all future releases **update in place**.

You will need to repeat this uninstall **only** if the release key changes again —
for example if `release-key.jks` is deleted or regenerated, or if the
`RELEASE_KEYSTORE_BASE64` secret is replaced with a different keystore. Keep
`release-key.jks` safe and back it up outside the repo.

---

## 3. In-place updates (how they are guaranteed)

Three conditions must hold for Android to update an app without uninstalling. All
three are enforced by this repo:

| Condition | How it is satisfied |
| --- | --- |
| **Same signing key** | `release-key.jks` is a persistent, gitignored keystore committed **once** into the repo root. Every release build — local or CI — resolves the same key, so the certificate never changes. |
| **`versionCode` strictly increases** | `version.properties` ships with `versionCode` **commented out** ("auto mode"). `app/build.gradle.kts` then derives a timestamp-based `versionCode`, floored by `coerceAtLeast(2)`, which is always higher than the pinned `versionCode=2` that shipped in v1.0.1. Every new build is therefore newer than the previous one. |
| **Unchanged `applicationId`** | Fixed at `com.aistudio.fluxlauncher.vknwre`. Never rename the package. |

### Version resolution

```
versionCode   = version.properties value, else ((epochSeconds - 1700000000) / 10)
               coalesced with a floor of 2
versionName   = version.properties value, else "1.0.<versionCode>"
```

The resolved values are printed on every compile/assemble run:

```
RESOLVED versionCode=... versionName=...
```

To pin a version manually, uncomment `versionCode` / `versionName` in
`version.properties` — but the values **must always be higher** than the highest
version already installed on any device, otherwise the install is rejected.

### Local signing requirements

The release `signingConfig` resolves its keystore in this order:

1. `KEYSTORE_PATH` environment variable (CI decodes the base64 secret here)
2. `<repoRoot>/release-key.jks` (the normal local path)
3. `<repoRoot>/my-upload-key.jks` (legacy path, still supported)

Passwords and alias come from the environment, defaulting to the values the repo's
own `release-key.jks` was generated with:

| Variable | Default |
| --- | --- |
| `KEYSTORE_PATH` / `KEYSTORE_PASSWORD` / `STORE_PASSWORD` | *(unset)* / `fluxstorepass` |
| `KEY_PASSWORD` / `KEY_PASS` | `fluxkeypass` |
| `KEY_ALIAS` | `flux-release` |

If no valid keystore is found the build still succeeds but prints
`RELEASE SIGNING WARNING: ... FALLING BACK TO THE DEBUG KEYSTORE.` — an APK built
this way **cannot be updated in place** and must be uninstalled before installing a
properly signed one. Never publish such an APK.

---

## 4. CI (quota-safe)

GitHub Actions quota is exhausted, so the pipelines are deliberately cheap:

* **`.github/workflows/build.yml`** — **Validate**. Triggered by pushes to `main`
  (ignoring `**/*.md`, `design/**`, `docs/**`) and by PRs. Single 15-minute job that
  only compiles, tests and lints — no release build, no keystore decoding. Tests
  gate the build; lint/detekt are advisory (`continue-on-error`). Test and lint
  reports are uploaded as the `test-and-lint-reports` artifact (retention 7 days).
  A release build runs **only** on `workflow_dispatch` with `build_type: release|both`.
* **`.github/workflows/release.yml`** — **Release APK**. Triggered **only** on `v*`
  tag pushes and manual dispatch. 20-minute job that builds `assembleRelease`,
  copies the APK to `flux.apk`, runs the unit tests, and uploads `flux.apk` to the
  **already existing** GitHub release with `gh release upload ... --clobber`. This
  workflow never creates a release — the release must exist first (created by the
  release manager with notes).

Both workflows use `concurrency` with `cancel-in-progress` so a new run cancels a
superseded one for the same ref, and Gradle caching via `gradle/actions/setup-gradle`.

Required repository secrets (all four are configured):

| Secret | Contents |
| --- | --- |
| `RELEASE_KEYSTORE_BASE64` | `base64 -w0 release-key.jks` |
| `KEYSTORE_PASSWORD` | `fluxstorepass` |
| `KEY_PASSWORD` | `fluxkeypass` |
| `KEY_ALIAS` | `flux-release` |

---

## 5. Cutting a release

```bash
# 1. Make sure the working tree is clean and the version will auto-increment
cat version.properties          # versionCode must be commented out

# 2. Optional: build and smoke-test locally (SDK required)
./gradlew :app:assembleRelease --no-daemon

# 3. Push main and create/publish the GitHub release for the new tag
gh release create v2.0.0 --title "v2.0.0" --notes "..." --repo 5r1n1dh1/niagara_clone

# 4. Push the tag — release.yml builds the APK, names it flux.apk and uploads it
git tag v2.0.0
git push origin v2.0.0
```

The tag push starts `release.yml`, which builds the release APK, copies it to
`flux.apk`, runs the unit tests, and attaches `flux.apk` to the release created in
step 3 (`gh release upload <tag> flux.apk --clobber`).

> The release must exist **before** the tag is pushed — `release.yml` only uploads
> the asset, it does not create the release.

### Checklist before tagging

- [ ] `version.properties` — `versionCode` still commented out (auto-increment)
- [ ] `release-key.jks` present and unchanged (signing key must not rotate)
- [ ] `applicationId` unchanged
- [ ] Local `:app:assembleRelease` succeeds and prints `RESOLVED versionCode=...`
  with a value higher than the previous release
- [ ] Release created on GitHub with release notes
