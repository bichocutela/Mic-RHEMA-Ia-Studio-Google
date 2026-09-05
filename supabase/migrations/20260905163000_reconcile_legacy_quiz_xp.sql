create or replace function public.xp_ensure_account(p_member_id text, p_legacy_xp integer default 0)
returns table(member_id text, total_earned integer, total_spent integer, balance integer, migrated_legacy_xp integer, updated_at timestamptz)
language plpgsql
security definer
set search_path = public
as $$
declare
  v_legacy integer := greatest(coalesce(p_legacy_xp, 0), 0);
  v_delta integer := 0;
  v_account public.xp_accounts%rowtype;
begin
  insert into public.xp_accounts(member_id, total_earned, total_spent, balance, migrated_legacy_xp)
  values (p_member_id, v_legacy, 0, v_legacy, v_legacy)
  on conflict (member_id) do nothing;

  select * into v_account
  from public.xp_accounts
  where xp_accounts.member_id = p_member_id
  for update;

  if v_legacy > v_account.migrated_legacy_xp then
    v_delta := v_legacy - v_account.migrated_legacy_xp;

    update public.xp_accounts
    set total_earned = total_earned + v_delta,
        balance = balance + v_delta,
        migrated_legacy_xp = v_legacy,
        updated_at = now()
    where xp_accounts.member_id = p_member_id
    returning * into v_account;

    insert into public.xp_transactions(
      member_id,
      type,
      amount,
      activity,
      content_id,
      variant,
      receipt_id,
      description,
      date_key
    )
    values (
      p_member_id,
      'earn',
      v_delta,
      'legacy_sync',
      'journey_quiz',
      '',
      'legacy_sync:' || v_legacy::text,
      'XP sincronizado do progresso da Jornada Bíblica',
      (timezone('America/Recife', now()))::date
    )
    on conflict (member_id, receipt_id) do nothing;
  end if;

  return query
  select
    v_account.member_id,
    v_account.total_earned,
    v_account.total_spent,
    v_account.balance,
    v_account.migrated_legacy_xp,
    v_account.updated_at;
end;
$$;

revoke all on function public.xp_ensure_account(text, integer) from public, anon, authenticated;
