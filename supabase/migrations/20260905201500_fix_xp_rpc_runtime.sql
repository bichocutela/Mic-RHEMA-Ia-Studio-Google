create or replace function public.xp_ensure_account(p_member_id text, p_legacy_xp integer default 0)
returns table(member_id text, total_earned integer, total_spent integer, balance integer, migrated_legacy_xp integer, updated_at timestamptz)
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_legacy integer := greatest(coalesce(p_legacy_xp, 0), 0);
  v_delta integer := 0;
  v_account public.xp_accounts%rowtype;
begin
  insert into public.xp_accounts as xa(member_id, total_earned, total_spent, balance, migrated_legacy_xp)
  values (p_member_id, v_legacy, 0, v_legacy, v_legacy)
  on conflict on constraint xp_accounts_pkey do nothing;

  select xa.* into v_account
  from public.xp_accounts as xa
  where xa.member_id = p_member_id
  for update;

  if v_legacy > v_account.migrated_legacy_xp then
    v_delta := v_legacy - v_account.migrated_legacy_xp;

    update public.xp_accounts as xa
    set total_earned = xa.total_earned + v_delta,
        balance = xa.balance + v_delta,
        migrated_legacy_xp = v_legacy,
        updated_at = now()
    where xa.member_id = p_member_id
    returning xa.* into v_account;

    insert into public.xp_transactions(
      member_id, type, amount, activity, content_id, variant, receipt_id, description, date_key
    ) values (
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
    on conflict on constraint xp_transactions_member_id_receipt_id_key do nothing;
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

create or replace function public.xp_award(
  p_member_id text,
  p_activity text,
  p_content_id text,
  p_variant text,
  p_receipt_id text,
  p_amount integer,
  p_description text,
  p_daily_cap integer default 0
)
returns table(granted integer, duplicate boolean, cap_reached boolean, total_earned integer, total_spent integer, balance integer)
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_today date := (timezone('America/Recife', now()))::date;
  v_today_xp integer := 0;
  v_account public.xp_accounts%rowtype;
begin
  if coalesce(p_amount, 0) <= 0 then
    raise exception 'XP inválido';
  end if;

  perform 1
  from public.xp_accounts as xa
  where xa.member_id = p_member_id
  for update;
  if not found then
    raise exception 'Conta XP não inicializada';
  end if;

  if exists(
    select 1
    from public.xp_transactions as xt
    where xt.member_id = p_member_id and xt.receipt_id = p_receipt_id
  ) then
    select xa.* into v_account
    from public.xp_accounts as xa
    where xa.member_id = p_member_id;
    return query select 0, true, false, v_account.total_earned, v_account.total_spent, v_account.balance;
    return;
  end if;

  if coalesce(p_daily_cap, 0) > 0 then
    select coalesce(sum(xt.amount), 0)::integer into v_today_xp
    from public.xp_transactions as xt
    where xt.member_id = p_member_id
      and xt.type = 'earn'
      and xt.activity = p_activity
      and xt.date_key = v_today;

    if v_today_xp + p_amount > p_daily_cap then
      select xa.* into v_account
      from public.xp_accounts as xa
      where xa.member_id = p_member_id;
      return query select 0, false, true, v_account.total_earned, v_account.total_spent, v_account.balance;
      return;
    end if;
  end if;

  begin
    insert into public.xp_transactions(member_id, type, amount, activity, content_id, variant, receipt_id, description, date_key)
    values (p_member_id, 'earn', p_amount, p_activity, coalesce(p_content_id, ''), coalesce(p_variant, ''), p_receipt_id, coalesce(p_description, ''), v_today);
  exception when unique_violation then
    select xa.* into v_account
    from public.xp_accounts as xa
    where xa.member_id = p_member_id;
    return query select 0, true, false, v_account.total_earned, v_account.total_spent, v_account.balance;
    return;
  end;

  update public.xp_accounts as xa
  set total_earned = xa.total_earned + p_amount,
      balance = xa.balance + p_amount,
      updated_at = now()
  where xa.member_id = p_member_id
  returning xa.* into v_account;

  return query select p_amount, false, false, v_account.total_earned, v_account.total_spent, v_account.balance;
end;
$$;

create or replace function public.xp_redeem(p_member_id text, p_item_id text)
returns table(
  redemption_id uuid,
  redemption_code text,
  redemption_status text,
  item_name text,
  item_cost integer,
  total_earned integer,
  total_spent integer,
  balance integer
)
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_account public.xp_accounts%rowtype;
  v_item public.xp_shop_items%rowtype;
  v_redemption public.xp_redemptions%rowtype;
  v_count integer := 0;
begin
  select xa.* into v_account
  from public.xp_accounts as xa
  where xa.member_id = p_member_id
  for update;
  if not found then raise exception 'Conta XP não encontrada'; end if;

  select xsi.* into v_item
  from public.xp_shop_items as xsi
  where xsi.id = p_item_id
  for update;
  if not found or not v_item.active then raise exception 'Recompensa indisponível'; end if;
  if v_item.available_from is not null and now() < v_item.available_from then raise exception 'Recompensa ainda não disponível'; end if;
  if v_item.available_until is not null and now() > v_item.available_until then raise exception 'Período da recompensa encerrado'; end if;

  if v_account.balance < v_item.cost then raise exception 'Saldo XP insuficiente'; end if;
  if v_item.stock is not null and v_item.stock <= 0 then raise exception 'Recompensa esgotada'; end if;

  select count(*)::integer into v_count
  from public.xp_redemptions as xr
  where xr.member_id = p_member_id
    and xr.item_id = p_item_id
    and xr.status <> 'cancelado';
  if v_count >= v_item.limit_per_member then raise exception 'Limite de resgate atingido para esta recompensa'; end if;

  insert into public.xp_redemptions(member_id, item_id, item_name, cost, status, redemption_code)
  values (
    p_member_id,
    v_item.id,
    v_item.name,
    v_item.cost,
    case when v_item.kind = 'physical' then 'pendente' else 'liberado' end,
    'RHEMA-' || upper(substr(replace(gen_random_uuid()::text, '-', ''), 1, 8))
  ) returning * into v_redemption;

  if v_item.stock is not null then
    update public.xp_shop_items as xsi
    set stock = xsi.stock - 1,
        updated_at = now()
    where xsi.id = v_item.id;
  end if;

  update public.xp_accounts as xa
  set total_spent = xa.total_spent + v_item.cost,
      balance = xa.balance - v_item.cost,
      updated_at = now()
  where xa.member_id = p_member_id
  returning xa.* into v_account;

  insert into public.xp_transactions(member_id, type, amount, activity, content_id, variant, receipt_id, description)
  values (
    p_member_id,
    'spend',
    v_item.cost,
    'shop_redeem',
    v_item.id,
    v_item.kind,
    'shop:' || v_redemption.id::text,
    'Resgate: ' || v_item.name
  );

  return query select
    v_redemption.id,
    v_redemption.redemption_code,
    v_redemption.status,
    v_item.name,
    v_item.cost,
    v_account.total_earned,
    v_account.total_spent,
    v_account.balance;
end;
$$;

create or replace function public.xp_admin_update_redemption(
  p_redemption_id uuid,
  p_status text
)
returns table(
  redemption_id uuid,
  redemption_status text,
  delivered_at timestamptz,
  member_id text,
  item_id text,
  item_name text,
  cost integer,
  total_earned integer,
  total_spent integer,
  balance integer
)
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_redemption public.xp_redemptions%rowtype;
  v_account public.xp_accounts%rowtype;
  v_item public.xp_shop_items%rowtype;
begin
  if p_status not in ('pendente', 'entregue', 'cancelado') then
    raise exception 'Status de resgate inválido';
  end if;

  select xr.* into v_redemption
  from public.xp_redemptions as xr
  where xr.id = p_redemption_id
  for update;
  if not found then raise exception 'Resgate não encontrado'; end if;

  select xa.* into v_account
  from public.xp_accounts as xa
  where xa.member_id = v_redemption.member_id
  for update;
  if not found then raise exception 'Conta XP não encontrada'; end if;

  select xsi.* into v_item
  from public.xp_shop_items as xsi
  where xsi.id = v_redemption.item_id
  for update;
  if not found then raise exception 'Recompensa não encontrada'; end if;

  if v_redemption.status = p_status then
    return query select
      v_redemption.id,
      v_redemption.status,
      v_redemption.delivered_at,
      v_redemption.member_id,
      v_redemption.item_id,
      v_redemption.item_name,
      v_redemption.cost,
      v_account.total_earned,
      v_account.total_spent,
      v_account.balance;
    return;
  end if;

  if v_redemption.status = 'cancelado' then
    raise exception 'Resgate cancelado não pode ser reaberto';
  end if;

  if v_redemption.status = 'entregue' and p_status <> 'entregue' then
    raise exception 'Resgate já entregue não pode ser alterado';
  end if;

  if p_status = 'cancelado' then
    update public.xp_accounts as xa
    set total_spent = greatest(xa.total_spent - v_redemption.cost, 0),
        balance = xa.balance + v_redemption.cost,
        updated_at = now()
    where xa.member_id = v_redemption.member_id
    returning xa.* into v_account;

    if v_item.stock is not null then
      update public.xp_shop_items as xsi
      set stock = xsi.stock + 1,
          updated_at = now()
      where xsi.id = v_item.id;
    end if;

    insert into public.xp_transactions(
      member_id, type, amount, activity, content_id, variant, receipt_id, description
    ) values (
      v_redemption.member_id,
      'adjustment',
      v_redemption.cost,
      'shop_refund',
      v_redemption.item_id,
      'cancelled_redemption',
      'shop_refund:' || v_redemption.id::text,
      'Estorno: ' || v_redemption.item_name
    ) on conflict on constraint xp_transactions_member_id_receipt_id_key do nothing;

    update public.xp_redemptions as xr
    set status = 'cancelado',
        delivered_at = null
    where xr.id = v_redemption.id
    returning xr.* into v_redemption;
  elsif p_status = 'entregue' then
    update public.xp_redemptions as xr
    set status = 'entregue',
        delivered_at = now()
    where xr.id = v_redemption.id
    returning xr.* into v_redemption;
  else
    update public.xp_redemptions as xr
    set status = 'pendente',
        delivered_at = null
    where xr.id = v_redemption.id
    returning xr.* into v_redemption;
  end if;

  return query select
    v_redemption.id,
    v_redemption.status,
    v_redemption.delivered_at,
    v_redemption.member_id,
    v_redemption.item_id,
    v_redemption.item_name,
    v_redemption.cost,
    v_account.total_earned,
    v_account.total_spent,
    v_account.balance;
end;
$$;

revoke all on function public.xp_ensure_account(text, integer) from public, anon, authenticated;
revoke all on function public.xp_award(text, text, text, text, text, integer, text, integer) from public, anon, authenticated;
revoke all on function public.xp_redeem(text, text) from public, anon, authenticated;
revoke all on function public.xp_admin_update_redemption(uuid, text) from public, anon, authenticated;

grant execute on function public.xp_ensure_account(text, integer) to service_role;
grant execute on function public.xp_award(text, text, text, text, text, integer, text, integer) to service_role;
grant execute on function public.xp_redeem(text, text) to service_role;
grant execute on function public.xp_admin_update_redemption(uuid, text) to service_role;
