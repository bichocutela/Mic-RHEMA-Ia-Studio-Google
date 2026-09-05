create table if not exists public.xp_legacy_quiz_receipts (
  member_id text not null references public.xp_accounts(member_id) on delete cascade,
  question_id text not null,
  migrated_at timestamptz not null default now(),
  primary key (member_id, question_id)
);

alter table public.xp_legacy_quiz_receipts enable row level security;
revoke all on public.xp_legacy_quiz_receipts from anon, authenticated;
grant select, insert on public.xp_legacy_quiz_receipts to service_role;

create index if not exists xp_legacy_quiz_receipts_member_idx
  on public.xp_legacy_quiz_receipts(member_id);
