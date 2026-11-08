# MICRHEMA migration checkpoint — 2026-11-08

Source: `cwphbkdtorfpgmnlafqb`. Destination: `sinzscjscvgzkvkimcbd`.

## Completed in destination

- 14 public tables, 226 rows, 20 database functions, indexes, constraints, triggers, comments and grants copied. All 14 table content checksums matched the source snapshot.
- RLS remains enabled on all 14 tables; access is through the backend service role, as in the source.
- All 30 application Edge Functions deployed. Six needed the authentication changes in this branch. The storage gateway has no built-in admin password fallback in the destination.
- Three buckets created with the source visibility, MIME and size settings.
- Four cron definitions and the scheduler's Vault secret copied. The destination cron jobs remain inactive until cutover, avoiding duplicate notifications and publications.
- Temporary `migration-copy-20261108` transfer functions on both projects were replaced with inactive HTTP 410 handlers, with gateway JWT verification enabled.

## Authentication changes

Firebase ID tokens must have a valid RS256 signature, project audience, issuer and expiration. Administrator operations require the signed `isAdmin` claim; internal notification calls use the destination service-role secret. Public catalog reads remain available and cannot perform administrative mutations. Badge reconciliation is restricted to the signed member's own canonical ID, or an administrator.

Member recovery requires a bound, non-anonymous identity or a recent Firebase Phone Auth confirmation for the requested phone. It never deletes duplicate records. Administrator login requires a configured password hash; there is no default password hash. The PWA changes add SMS confirmation and send actual Firebase tokens to the protected endpoints.

## Blocking completion and cutover

- The source currently returns HTTP 402 with `exceed_egress_quota`, preventing download of all 43 objects (78,894,227 bytes). No file bytes have been copied yet.
- Configure the destination's Firebase service-account credential, administrative password configuration, and other used integrations such as the YouTube API. Built-in Supabase credentials are destination-specific and must not be copied from the source. Do not put secret values into this repository.
- Phone Auth must be configured and tested in Firebase before publishing the PWA SMS flow. Android callers still need their token/phone-confirmation changes before switching an APK to the new backend.
- Application URLs, public keys, GitHub build secrets and existing Firebase-stored media links still need the destination configuration after file transfer and end-to-end validation.
- Old cron jobs remain unchanged. Switch them only as part of the final cutover after destination validation. No source business data was erased.

## Validation

`deno check` passed for the six changed functions and shared tests. Eleven offline tests passed using locally generated RSA tokens and a mocked Google JWKS endpoint: signature, audience, expiry, role restrictions, phone ownership, all six unauthenticated endpoints, CORS preflight and cross-member rejection. Live destination probes also check HTTP 401 for unauthenticated privileged requests. PWA edits passed TypeScript syntax transpilation; full PWA and Android integration validation is pending.
