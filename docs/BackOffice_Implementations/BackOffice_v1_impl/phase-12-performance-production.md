# Phase 12 — Performance & Production Build

> **Status**: `NOT STARTED`
> **Prerequisite**: Phase 11
> **Target**: `wasmJs` (Kotlin/Wasm — replaces Kotlin/JS)
> [← Back to master plan](../web-backoffice-implementation-plan.md)

---

## Bundle Targets

| Metric | Target |
|---|---|
| Total Wasm+JS bundle | < 2MB gzipped (wasmJs bundles are larger than pure JS) |
| Time to interactive | < 3s on 4G |
| First contentful paint | < 1.5s |

## Optimisation Checklist

- [ ] Dead code elimination via Kotlin/Wasm `PRODUCTION` mode
- [ ] Webpack tree-shaking enabled (webpack 5 bundler — already configured)
- [ ] Google Fonts (Inter) loaded with `display=swap` + Latin charset subset only
- [ ] Fleet tracking screen lazy-loaded (heaviest canvas computation deferred)
- [ ] `FrontendMetrics` calls stripped in production via `BuildConfig` flag
- [ ] K2D compiler restricted to `kspCommonMainMetadata` only (do not add `kspWasmJs`)

## Build Commands (wasmJs)

```powershell
# Development run (dev server on port 8080)
.\gradlew.bat :composeApp:wasmJsBrowserDevelopmentRun

# Production webpack bundle
.\gradlew.bat :composeApp:wasmJsBrowserProductionWebpack

# Run KSP annotation processor (K2D diagram generation)
.\gradlew.bat :composeApp:kspCommonMainKotlinMetadata

# Run all tests
.\gradlew.bat :composeApp:allTests

# Full clean build
.\gradlew.bat clean :composeApp:wasmJsBrowserProductionWebpack
```

> **Note**: `jsTest` and `jsBrowserProductionWebpack` are for Kotlin/JS targets and do NOT apply to this project. All production tasks use `wasmJs*` variants.

## Security Checklist (OWASP)

- [ ] JWT in `sessionStorage` only — never `localStorage`, cookies, or URL params (KSafe enforces this)
- [ ] All API calls over HTTPS; HTTP base URL blocked at `PlatformConfig`
- [ ] `Idempotency-Key` (UUID v4) generated before every payment POST (done inside `PayInvoiceUseCase`)
- [ ] Role guard enforced at both sidebar (hidden items) and route level (redirect)
- [ ] 401 handler: clears token + navigates to `/login`
- [ ] Content Security Policy header set on hosting server
- [ ] No vehicle coordinates or user PII logged to browser console in production
