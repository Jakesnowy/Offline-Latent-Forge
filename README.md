<div align="center">

# Latent Forge <img src="./assets/icon.png" width="32" alt="Latent Forge">

**Android Stable Diffusion with Snapdragon NPU acceleration**
_Local inference, no cloud, no accounts. CPU/GPU inference also supported._

<img src="./assets/demo1.jpg" alt="App Demo" width="800">

</div>

## About This Fork

**Latent Forge** (repo: Offline-Latent-Forge) is a community fork of
[Local Dream](https://github.com/xororz/local-dream) by
[xororz](https://github.com/xororz). All of upstream's capabilities are here —
plus the fork's own additions, kept in sync with upstream where practical:

- **Privacy & security first.** Everything runs locally — no cloud, no
  accounts, no telemetry. Remote generation is authenticated: host mode
  issues a pairing token and unauthenticated requests are rejected with 401.
  Sensitive data (prompts, images, history database, per-model settings) is
  excluded from cloud backup and device transfer — the only intentional path
  out is the History screen's explicit export/import.
- **Verified downloads.** Model and asset downloads are integrity-checked:
  SHA-256 digests taken from Hugging Face's `X-Linked-ETag` metadata,
  size validation, and TOFU pinning for non-HF sources, so a corrupted or
  tampered artifact fails loudly instead of silently loading.
- **Refined generation controls** — collapsible advanced-settings card,
  batch/seed interplay (batch is locked to 1 while a seed is set, with an
  explanatory dialog), live UltraFix denoise-strength feedback, finer CFG
  support for DMD2 checkpoints.
- **Generation modes** — **stepping** (watch the image form step by step with
  live CPU previews, then keep, continue for higher quality, or discard —
  powered by an adjustable pause point) and **sweep** (the same seed rendered
  at three step counts for direct comparison), plus a 3-way preview-quality
  selector (Off / Fast / Full).
- **First-class "upscale" mode** — upscaled copies are recorded and filterable
  as their own mode in history instead of inheriting the source mode.
- **Windows build support** — a first-class `build.bat` native-engine build
  route (NDK r28+), alongside upstream's Linux flow.
- **Install-friendly builds** — repo-committed debug keystore so CI builds
  upgrade in place, a `.debug` build that coexists with the official app, and
  manually-dispatched beta/debug releases.

> [!IMPORTANT]
> This fork uses a **different applicationId**
> (`io.github.jakesnowy.offlinelatentforge`) than upstream. Switching between
> the two is a fresh install: app-private data (downloaded models, history)
> does not carry over. Use the History screen's export/import to move prompts
> and images.

## Supported Models

- **SD1.5** models on Snapdragon NPUs (Hexagon V68+) and CPU/GPU.
- **SDXL** models on Snapdragon 8 Gen 3 and newer (NPU), plus CPU/GPU.
- SD2.1 is not maintained (upstream decision, kept here).

Remote generation is also supported: one device can host its models for other
devices on the same network (host mode / LAN access).

## Install

Grab the latest APK from the
[Releases](https://github.com/Jakesnowy/Offline-Latent-Forge/releases) page:

- `basic` — standard build.
- `filter` — adds the NSFW checker (enforces the threshold).
- `beta` — the primary distribution line, signed with a stable key so betas
  upgrade in place.

NPU acceleration requires the Qualcomm QNN runtime; the app downloads or
bundles what it needs per device.

## Building From Source

Requirements: Android Studio (or command-line SDK), NDK **r28** recommended,
JDK 17+ for Gradle, and the Qualcomm [QNN
SDK](https://www.qualcomm.com/developer/software/qualcomm-ai-engine-direct-sdk)
for the native engine.

- **Windows:** `app/src/main/cpp/build.bat` builds the native engine, then
  `gradlew assembleBasicDebug` (or `assembleFilterDebug`).
- **Linux/macOS:** upstream's CMake flow applies; see upstream's docs.
- **CI:** every push runs lint/detekt; releases are cut manually via workflow
  dispatch (beta or per-commit debug builds).

Set `CI=1` in your environment before a local `gradlew` build to sign debug
APKs with the repo keystore (identical to CI builds, so they upgrade in
place).

## User Guide

Upstream's guide site applies to this fork as well:
[Guide Site](https://ld-guide.chino.icu).

## Credits & Acknowledgments

This fork exists thanks to **[xororz](https://github.com/xororz)** and the
original **Local Dream** project — all core engineering (the NPU engine, model
pipeline, and app architecture) is their work. Please support upstream.

Offline Latent Forge is built on top of many excellent open-source projects.
Sincere thanks to all the authors and contributors whose work made this
project possible.

### C++ Libraries

- **[Qualcomm QNN SDK](https://www.qualcomm.com/developer/software/qualcomm-ai-engine-direct-sdk)** - NPU model execution
- **[alibaba/MNN](https://github.com/alibaba/MNN/)** - CPU model execution
- **[xtensor-stack](https://github.com/xtensor-stack)** - Tensor operations & scheduling
- **[mlc-ai/tokenizers-cpp](https://github.com/mlc-ai/tokenizers-cpp)** - Text tokenization
- **[yhirose/cpp-httplib](https://github.com/yhirose/cpp-httplib)** - HTTP server
- **[nothings/stb](https://github.com/nothings/stb)** - Image processing
- **[facebook/zstd](https://github.com/facebook/zstd)** - Model compression
- **[nlohmann/json](https://github.com/nlohmann/json)** - JSON processing

### Android Libraries

- **[square/okhttp](https://github.com/square/okhttp)** - HTTP client
- **[coil-kt/coil](https://github.com/coil-kt/coil)** - Image loading & processing
- **[MoyuruAizawa/Cropify](https://github.com/MoyuruAizawa/Cropify)** - Image cropping
- **AOSP, Material Design, Jetpack Compose** - UI framework

### Models

- **[CompVis/stable-diffusion](https://github.com/CompVis/stable-diffusion)** and all other model creators
- **[xinntao/Real-ESRGAN](https://github.com/xinntao/Real-ESRGAN)** - Image upscaling
- **[Kim2091/UltraSharpV2](https://huggingface.co/Kim2091/UltraSharpV2)** - Image upscaling
- **[bhky/opennsfw2](https://github.com/bhky/opennsfw2)** - NSFW content filtering

---

## 💖 Support Upstream

If you find this fork useful, the engineering behind it belongs to upstream
**Local Dream** — please consider supporting xororz directly:

<a href="https://ko-fi.com/xororz">
    <img height="36" style="border:0px;height:36px;" src="https://storage.ko-fi.com/cdn/kofi2.png?v=3" border="0" alt="Buy Me a Coffee at ko-fi.com" />
</a>
<a href="https://afdian.com/a/xororz">
    <img height="36" style="border-radius:12px;height:36px;" src="https://pic1.afdiancdn.com/static/img/welcome/button-sponsorme.jpg" alt="在爱发电支持我" />
</a>

