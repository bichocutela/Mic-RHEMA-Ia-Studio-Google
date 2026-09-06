alter table public.xp_redemptions
  add column if not exists stock_consumed boolean not null default false;

update public.xp_shop_items
set limit_per_member = 1,
    updated_at = now()
where kind in ('digital','profile')
  and limit_per_member <> 1;

do $$
begin
  if not exists (
    select 1 from pg_constraint
    where conname = 'xp_shop_items_nonphysical_single_limit_chk'
      and conrelid = 'public.xp_shop_items'::regclass
  ) then
    alter table public.xp_shop_items
      add constraint xp_shop_items_nonphysical_single_limit_chk
      check (kind = 'physical' or limit_per_member = 1);
  end if;
end $$;

create or replace function public.xp_mark_redemption_stock_consumed()
returns trigger
language plpgsql
security definer
set search_path = ''
as $function$
begin
  select (xsi.stock is not null)
    into new.stock_consumed
  from public.xp_shop_items as xsi
  where xsi.id = new.item_id;
  new.stock_consumed := coalesce(new.stock_consumed, false);
  return new;
end;
$function$;

drop trigger if exists xp_mark_redemption_stock_consumed_before_insert on public.xp_redemptions;
create trigger xp_mark_redemption_stock_consumed_before_insert
before insert on public.xp_redemptions
for each row execute function public.xp_mark_redemption_stock_consumed();

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
as $function$
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
    delete from public.xp_entitlements as xe
    where xe.redemption_id = v_redemption.id;

    update public.xp_accounts as xa
    set total_spent = greatest(xa.total_spent - v_redemption.cost, 0),
        balance = xa.balance + v_redemption.cost,
        updated_at = now()
    where xa.member_id = v_redemption.member_id
    returning xa.* into v_account;

    if v_redemption.stock_consumed then
      update public.xp_shop_items as xsi
      set stock = coalesce(xsi.stock, 0) + 1,
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
$function$;

create or replace function public.xp_daily_mission_state(p_member_id text)
returns jsonb
language plpgsql
stable
security definer
set search_path = ''
as $function$
declare
  v_today date := (timezone('America/Recife', now()))::date;
  v_chapter integer := 0;
  v_growth integer := 0;
  v_quiz integer := 0;
  v_active integer := 0;
  v_bonus boolean := false;
  v_complete boolean := false;
begin
  select
    count(*) filter (where xt.activity = 'bible_chapter')::integer,
    count(*) filter (where xt.activity in ('devotional','plan_theme','book_10','book_complete','ibr_lesson'))::integer,
    count(*) filter (where xt.activity in ('quiz_easy','quiz_medium','quiz_hard'))::integer,
    count(*) filter (where xt.activity = 'active_5min')::integer,
    exists(
      select 1 from public.xp_transactions dm
      where dm.member_id = p_member_id
        and dm.type = 'earn'
        and dm.activity = 'daily_mission'
        and dm.date_key = v_today
    )
  into v_chapter, v_growth, v_quiz, v_active, v_bonus
  from public.xp_transactions xt
  where xt.member_id = p_member_id
    and xt.type = 'earn'
    and xt.date_key = v_today;

  v_chapter := coalesce(v_chapter, 0);
  v_growth := coalesce(v_growth, 0);
  v_quiz := coalesce(v_quiz, 0);
  v_active := coalesce(v_active, 0);
  v_complete := v_chapter >= 1 and v_growth >= 1 and v_quiz >= 3 and v_active >= 2;

  return jsonb_build_object(
    'date', v_today::text,
    'chapter', least(v_chapter, 1),
    'chapterTarget', 1,
    'growth', least(v_growth, 1),
    'growthTarget', 1,
    'quiz', least(v_quiz, 3),
    'quizTarget', 3,
    'activeBlocks', least(v_active, 2),
    'activeBlocksTarget', 2,
    'complete', v_complete,
    'bonusGranted', coalesce(v_bonus, false)
  );
end;
$function$;

revoke execute on function public.xp_daily_mission_state(text) from public, anon, authenticated;
grant execute on function public.xp_daily_mission_state(text) to service_role;
