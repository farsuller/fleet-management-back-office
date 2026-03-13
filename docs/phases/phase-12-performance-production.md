# Phase 12 — Performance & Production Build

> **Status**: `NOT STARTED`
> **Prerequisite**: Phase 11
> [← Back to master plan](../web-backoffice-implementation-plan.md)

---

## Bundle Targets

| Metric | Target |
|---|---|
| Total JS bundle | < 500KB gzipped |
| Time to interactive | < 3s on 4G |
| First contentful paint | < 1.5s |

## Optimisation Checklist

- [ ] Dead code elimination via Kotlin/JS IR `--mode=PRODUCTION`
- [ ] Webpack tree-shaking enabled
- [ ] Google Fonts (Inter) loaded with `display=swap` + Latin charset subset only
- [ ] Fleet tracking screen lazy-loaded (dynamic import — heaviest SVG computation deferred)
- [ ] `FrontendMetrics` calls stripped in production via `BuildConfig` flag

## Production Build

```powershell
# Run all tests first
.\gradlew.bat :composeApp:jsTest

# Production webpack bundle
.\gradlew.bat :composeApp:jsBrowserProductionWebpack

# Check bundle size
.\gradlew.bat :composeApp:jsBrowserProductionWebpack --info | Select-String "bundle size"
```

## Security Checklist (OWASP)

- [ ] JWT in `sessionStorage` only — never `localStorage`, cookies, or URL params
- [ ] All API calls over HTTPS; HTTP base URL blocked at `PlatformConfig`
- [ ] `Idempotency-Key` (UUID v4) generated before every payment POST
- [ ] Role guard enforced at both sidebar (hidden items) and route level (redirect)
- [ ] 401 handler: clears token + navigates to `/login`
- [ ] Content Security Policy header set on hosting server
- [ ] No vehicle coordinates or user PII logged to browser console in production
