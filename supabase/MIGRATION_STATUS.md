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

Member recovery requires a bound, non-anonymous identity or a recent Firebase Phone Auth confirmation for the requested phone. It never deletes duplicate records. Administrator login requires a configured password hash; there is no default password hash. The PWA changes add SMS confirmation and send actual Firebase tokens to the protected endpoints.

## Blocking completion and cutover

- The original quota restriction has cleared and file transfer is complete.
- With the user's explicit approval, `FIREBASE_SERVICE_ACCOUNT_JSON`, its project configuration and `YOUTUBE_API_KEY` were copied directly between servers to destination Vault and verified. Nineteen Edge Functions now load the migrated credentials through the server-only `micrhema_runtime_configuration` RPC. Native environment variables take precedence; no credential values are in Git or the client. `anon` and `authenticated` cannot execute the configuration RPCs. Built-in Supabase credentials remain destination-specific.
- Administrative login now validates the existing Firebase email/password account and checks its bound administrative member record. It does not use a bundled fallback password. Automatic review blocked a real-password login test pending explicit authorization to send the password found in local app configuration to the new authentication service. No such test was executed.
- Firebase's configuration API confirmed valid service-account authentication and enabled email authentication. Phone authentication is not enabled. Do not activate paid SMS service or publish the SMS-dependent login flow without a user decision on that service.
- Phone Auth must be configured and tested in Firebase before using SMS verification in production. Android and PWA client adaptations are prepared in this branch: the destination uses Firebase tokens, verified phone recovery and server-validated admin login. The original backend keeps its existing client contract until the URL changes.
- Application URLs, public keys, GitHub build secrets and existing Firebase-stored media links still need the destination configuration after file transfer and end-to-end validation.
- Fixed live-stream URLs now follow the configured backend. PWA legacy public media links and its media gateway preserve object paths when resolving them against the destination. Android prayer submissions use the server endpoint so pastoral notifications remain server-authenticated.
- Old cron jobs remain unchanged. Switch them only as part of the final cutover after destination validation. No source business data was erased.

## Validation

`deno check` passed for the six changed functions and shared tests. Eleven offline tests passed using locally generated RSA tokens and a mocked Google JWKS endpoint: signature, audience, expiry, role restrictions, phone ownership, all six unauthenticated endpoints, CORS preflight and cross-member rejection. All six live destination probes returned HTTP 401 for unauthenticated privileged requests. Initial PWA CI type checking passed. Its navigation test still expected the older hidden Discipulado behavior; the assertion now follows the existing public/visible product decision from commit `5b458b6`, without changing the product behavior. The new migration workflow checks Edge authentication and compiles Android without publishing an APK.

Commit `09aac2a` passed the full Android Kotlin compilation, Edge authentication tests and PWA type checking/build in GitHub Actions. Eighteen local navigation, publishing, notification and quota-resilience tests passed. Four additional local gateway tests verify legacy/current/relative media path resolution and rejection of unrelated hosts. Follow-up client changes are validated again by the same CI checks.

After credential transfer, 19 Deno authentication/configuration/media tests and 15 Node scheduler/notification/quota tests passed. The destination YouTube public read endpoint returned HTTP 200 with cached status. Forced YouTube refreshes now require a signed administrator or the scheduler credential; ordinary public reads cannot write configuration or force API refreshes. Its destination cron job uses authenticated POST and remains inactive until cutover. Vault changes require an Edge Function redeploy/cold start to refresh the per-worker configuration cache.
