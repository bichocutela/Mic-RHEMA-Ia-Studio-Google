create or replace function public.xp_ensure_account(p_member_id text, p_legacy_xp integer default 0)
returns table(member_id text, total_earned integer, total_spent integer, balance integer, migrated_legacy_xp integer, updated_at timestamptz)
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_legacy integer := greatest(coalesce(p_legacy_xp, 0), 0);
  v_account public.xp_accounts%rowtype;
begin
  insert into public.xp_accounts as xa(member_id, total_earned, total_spent, balance, migrated_legacy_xp)
  values (p_member_id, v_legacy, 0, v_legacy, v_legacy)
  on conflict on constraint xp_accounts_pkey do nothing;

  select xa.* into v_account
  from public.xp_accounts as xa
  where xa.member_id = p_member_id
  for update;

  if not found then
    raise exception 'Conta XP não pôde ser inicializada';
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
grant execute on function public.xp_ensure_account(text, integer) to service_role;
