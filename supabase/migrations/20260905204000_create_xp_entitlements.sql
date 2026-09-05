create table if not exists public.xp_entitlements (
  id uuid primary key default gen_random_uuid(),
  member_id text not null references public.xp_accounts(member_id) on delete cascade,
  item_id text not null references public.xp_shop_items(id),
  redemption_id uuid not null references public.xp_redemptions(id),
  item_name text not null,
  kind text not null check (kind in ('digital', 'profile')),
  unlocked_at timestamptz not null default now(),
  unique(member_id, item_id),
  unique(redemption_id)
);

create index if not exists xp_entitlements_member_unlocked_idx
  on public.xp_entitlements(member_id, unlocked_at desc);

alter table public.xp_entitlements enable row level security;
revoke all on public.xp_entitlements from anon, authenticated;
grant select, insert on public.xp_entitlements to service_role;

insert into public.xp_entitlements(member_id, item_id, redemption_id, item_name, kind, unlocked_at)
select
  xr.member_id,
  xr.item_id,
  xr.id,
  xr.item_name,
  xsi.kind,
  xr.created_at
from public.xp_redemptions as xr
join public.xp_shop_items as xsi on xsi.id = xr.item_id
where xr.status <> 'cancelado'
  and xsi.kind in ('digital', 'profile')
on conflict (member_id, item_id) do nothing;

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

  if v_item.kind in ('digital', 'profile') then
    insert into public.xp_entitlements(member_id, item_id, redemption_id, item_name, kind)
    values (p_member_id, v_item.id, v_redemption.id, v_item.name, v_item.kind)
    on conflict (member_id, item_id) do nothing;
  end if;

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

revoke all on function public.xp_redeem(text, text) from public, anon, authenticated;
grant execute on function public.xp_redeem(text, text) to service_role;
