# MICRHEMA migration checkpoint — 2026-11-08

Source: `cwphbkdtorfpgmnlafqb`. Destination: `sinzscjscvgzkvkimcbd`.

## Completed in destination

- 14 public tables, 226 rows, 20 database functions, indexes, constraints, triggers, comments and grants copied. All 14 table content checksums matched the source snapshot.
- RLS remains enabled on all 14 tables; access is through the backend service role, as in the source.
- All 30 application Edge Functions deployed. Six needed the authentication changes in this branch. The storage gateway has no built-in admin password fallback in the destination.
- Three buckets created with the source visibility, MIME and size settings.
- All 43 storage files (78,894,227 bytes) copied after the source quota restriction cleared. File paths, sizes and content ETags matched for all 43 files; the transfer also checked SHA-256 hashes between the two servers.
- All 42 historical database migration records copied and their combined checksum verified.
- Four cron definitions and the scheduler's Vault secret copied. The destination cron jobs remain inactive until cutover, avoiding duplicate notifications and publications.
- Temporary `migration-copy-20261108` transfer functions on both projects were replaced with inactive HTTP 410 handlers, with gateway JWT verification enabled.

## Authentication changes

Firebase ID tokens must have a valid RS256 signature, project audience, issuer and expiration. Administrator operations require the signed `isAdmin` claim; internal notification calls use the destination service-role secret. Public catalog reads remain available and cannot perform administrative mutations. Badge reconciliation is restricted to the signed member's own canonical ID, or an administrator.

Member recovery requires a bound, non-anonymous identity or a recent Firebase Phone Auth confirmation for the requested phone. It never deletes duplicate records. Administrator login validates the existing Firebase email/password account and its bound administrative record; an explicitly configured private password hash may override that path. There is no bundled default password hash. The PWA changes add SMS confirmation and send actual Firebase tokens to the protected endpoints.

## Blocking completion and cutover

- The original quota restriction has cleared and file transfer is complete.
- With the user's explicit approval, `FIREBASE_SERVICE_ACCOUNT_JSON`, its project configuration and `YOUTUBE_API_KEY` were copied directly between servers to destination Vault and verified. Nineteen Edge Functions now load the migrated credentials through the server-only `micrhema_runtime_configuration` RPC. Native environment variables take precedence; no credential values are in Git or the client. `anon` and `authenticated` cannot execute the configuration RPCs. Built-in Supabase credentials remain destination-specific.
- Administrative login now validates the existing Firebase email/password account and checks its bound administrative member record. It does not use a bundled fallback password. After explicit user approval, the real administrative login was tested successfully: destination pwa-auth returned HTTP 200 with an administrative identity, Firebase exchanged its custom token successfully (HTTP 200), and the signed session accessed the protected admin catalog (HTTP 200). Passwords and session tokens were not printed or persisted.
- Firebase's configuration API confirmed valid service-account authentication and enabled email authentication. Phone authentication is not enabled. Do not activate paid SMS service or publish the SMS-dependent login flow without a user decision on that service.
- Phone Auth must be configured and tested in Firebase before using SMS verification in production. Android and PWA client adaptations are prepared in this branch: the destination uses Firebase tokens, verified phone recovery and server-validated admin login. The original backend keeps its existing client contract until the URL changes.
- Application URLs, public keys, GitHub build secrets and existing Firebase-stored media links still need the destination configuration after file transfer and end-to-end validation.
- Fixed live-stream URLs now follow the configured backend. PWA legacy public media links and its media gateway preserve object paths when resolving them against the destination. Android prayer submissions use the server endpoint so pastoral notifications remain server-authenticated.
- Old cron jobs remain unchanged. Switch them only as part of the final cutover after destination validation. No source business data was erased.

## Validation

`deno check` passed for the six changed functions and shared tests. Eleven offline tests passed using locally generated RSA tokens and a mocked Google JWKS endpoint: signature, audience, expiry, role restrictions, phone ownership, all six unauthenticated endpoints, CORS preflight and cross-member rejection. All six live destination probes returned HTTP 401 for unauthenticated privileged requests. Initial PWA CI type checking passed. Its navigation test still expected the older hidden Discipulado behavior; the assertion now follows the existing public/visible product decision from commit `5b458b6`, without changing the product behavior. The new migration workflow checks Edge authentication and compiles Android without publishing an APK.

Commit `09aac2a` passed the full Android Kotlin compilation, Edge authentication tests and PWA type checking/build in GitHub Actions. Eighteen local navigation, publishing, notification and quota-resilience tests passed. Four additional local gateway tests verify legacy/current/relative media path resolution and rejection of unrelated hosts. Follow-up client changes are validated again by the same CI checks.

After credential transfer, 19 Deno authentication/configuration/media tests and 15 Node scheduler/notification/quota tests passed. The destination YouTube public read endpoint returned HTTP 200 with cached status. Forced YouTube refreshes now require a signed administrator or the scheduler credential; ordinary public reads cannot write configuration or force API refreshes. Its destination cron job uses authenticated POST and remains inactive until cutover. Vault changes require an Edge Function redeploy/cold start to refresh the per-worker configuration cache.

All seven GitHub workflows passed for code commit `abd59b99af05d803d3c08c9376cd2f475d45f7fb`, including Android compilation, PWA build, Edge authentication and document handling. No production cutover or release has been performed.

## Scope correction requested by the user

The user explicitly restricted this work to copying Supabase only. No Android/PWA changes, login changes, new SMS/email flows, production application URL changes or app releases are authorized. All draft client adaptations were removed; the application client files match the original base commit. No migration client code was merged or published.

The database/storage/credentials copies described above are retained. Destination cron jobs remain inactive because the original project remains in use and duplicate jobs would publish/send twice. Source data, source functions and source cron jobs remain intact.

Exact source authentication-function parity remains blocked: automatic review rejected redeploying the original pwa-auth code because it reintroduces the source hardcoded administrative password-hash fallback. That rejected deployment did not change the destination. The currently deployed destination protected login is not an exact copy of the original member login. Do not treat SMS configuration or app changes as migration prerequisites; they are outside the requested scope. Do not cut over the application while backend parity is incomplete.
