create or replace function public.xp_admin_update_redemption(p_redemption_id uuid, p_status text)
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
      v_redemption.id, v_redemption.status, v_redemption.delivered_at,
      v_redemption.member_id, v_redemption.item_id, v_redemption.item_name,
      v_redemption.cost, v_account.total_earned, v_account.total_spent, v_account.balance;
    return;
  end if;

  if v_redemption.status = 'cancelado' then
    raise exception 'Resgate cancelado não pode ser reaberto';
  end if;
  if v_redemption.status = 'entregue' and p_status <> 'entregue' then
    raise exception 'Resgate já entregue não pode ser alterado';
  end if;

  if p_status = 'cancelado' then
    -- A propriedade digital/perfil faz parte do mesmo ciclo de vida do resgate.
    -- Revoga primeiro; qualquer falha posterior desfaz tudo pela transação do Postgres.
    delete from public.xp_entitlements as xe
    where xe.redemption_id = v_redemption.id;

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
      v_redemption.member_id, 'adjustment', v_redemption.cost,
      'shop_refund', v_redemption.item_id, 'cancelled_redemption',
      'shop_refund:' || v_redemption.id::text,
      'Estorno: ' || v_redemption.item_name
    ) on conflict on constraint xp_transactions_member_id_receipt_id_key do nothing;

    update public.xp_redemptions as xr
    set status = 'cancelado', delivered_at = null
    where xr.id = v_redemption.id
    returning xr.* into v_redemption;
  elsif p_status = 'entregue' then
    update public.xp_redemptions as xr
    set status = 'entregue', delivered_at = now()
    where xr.id = v_redemption.id
    returning xr.* into v_redemption;
  else
    update public.xp_redemptions as xr
    set status = 'pendente', delivered_at = null
    where xr.id = v_redemption.id
    returning xr.* into v_redemption;
  end if;

  return query select
    v_redemption.id, v_redemption.status, v_redemption.delivered_at,
    v_redemption.member_id, v_redemption.item_id, v_redemption.item_name,
    v_redemption.cost, v_account.total_earned, v_account.total_spent, v_account.balance;
end;
$$;

revoke all on function public.xp_admin_update_redemption(uuid, text) from public, anon, authenticated;
grant execute on function public.xp_admin_update_redemption(uuid, text) to service_role;

create index if not exists xp_entitlements_item_idx on public.xp_entitlements(item_id);
create index if not exists xp_redemptions_item_idx on public.xp_redemptions(item_id);
