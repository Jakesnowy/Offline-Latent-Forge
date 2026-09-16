# Changelog

All notable changes to Latent Forge (the fork) are documented here.
Upstream's own history lives in [xororz/local-dream](https://github.com/xororz/local-dream);
only fork-side changes are listed below.

## [Unreleased]

### Fixed
- Generation history recorded one more step than the slider value (txt2img
  steps+1, img2img +2): the engine's completed-step count included the
  pre-denoise CLIP/VAE-encode progress ticks.
- Generation progress card text showed the batch *setting* instead of the
  run-plan size — sweep displayed "1/1, 2/1, 3/1" and fixed-seed runs showed
  the slider count; it now shows progress against the actual plan.
- Stepping early exit (Finish) did not redirect to the results page; the
  completion redirect is now based on the run plan's last run.
- The prompt page scrolls to the top when a run starts so the progress card
  is fully visible.
- Stepping pause-point and sweep-interval sliders could not be set manually
  (value was read from non-observable preferences and snapped back on drag).

## [2.8.1] — fork baseline (upstream version at divergence)

### Added
- **Generation modes** (app-driven; the engine stays a single-run server):
  - *Stepping* — per-step CPU light previews with an interactive pause
    protocol (Continue / Finish / Cancel in-card), a user-configurable pause
    point (auto default derived from the step count), and a schedule that
    only runs real refinement steps past the pause. History records the exit
    step ("~23" display) and reproduces from the full schedule.
  - *Sweep* — three consecutive same-seed runs at S, S+i, S+2i through the
    batch machinery (one seed resolved up front when the field is blank).
- **Preview-quality selector** (Off / Fast / Full) replacing the "Show
  Generation Process" toggle; Fast uses CPU light previews and works in
  low-RAM where VAE decodes are unavailable.
- First-class Windows build for the native engine (`build.bat`), verified
  download integrity (SHA-256 from Hugging Face metadata, size validation,
  TOFU pinning for non-HF sources), history backup/restore with the fork's
  extended parameter set, finer CFG range for DMD2 checkpoints, and low-RAM
  defaults tuned to device memory.

### Security
- Host-mode authentication (pairing token; unauthenticated requests get 401)
  and a generation request payload limit.
- Backup exclusion for prompts, images, the history database, and per-model
  settings — export/import is the only intentional path out.
- Orphaned engine processes are reaped via a parent-death watchdog; engine
  teardown continues past failed frees to avoid QNN memory leaks.

### Changed
- Full rename to "Latent Forge" with a distinct `applicationId`
  (`io.github.jakesnowy.offlinelatentforge`) — fresh-install boundary vs
  upstream; repository README rewritten privacy-first.
- `ModelRunScreen` decomposition: 3,398 → 2,182 lines into region composables
  and state holders; model list and settings screens decomposed likewise.
- CI: lint/detekt on every push (assembly on PRs); releases are manual
  workflow dispatches (beta / debug lines), signed with a committed keystore
  so builds upgrade in place.

