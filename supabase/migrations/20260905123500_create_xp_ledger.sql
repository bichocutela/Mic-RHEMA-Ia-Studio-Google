create table if not exists public.xp_accounts (
  member_id text primary key,
  total_earned integer not null default 0 check (total_earned >= 0),
  total_spent integer not null default 0 check (total_spent >= 0),
  balance integer not null default 0 check (balance >= 0),
  migrated_legacy_xp integer not null default 0 check (migrated_legacy_xp >= 0),
  updated_at timestamptz not null default now()
);

create table if not exists public.xp_transactions (
  id uuid primary key default gen_random_uuid(),
  member_id text not null references public.xp_accounts(member_id) on delete cascade,
  type text not null check (type in ('earn','spend','adjustment')),
  amount integer not null check (amount > 0),
  activity text not null,
  content_id text not null default '',
  variant text not null default '',
  receipt_id text not null,
  description text not null default '',
  date_key date not null default ((timezone('America/Recife', now()))::date),
  created_at timestamptz not null default now(),
  unique(member_id, receipt_id)
);

create index if not exists xp_transactions_member_created_idx on public.xp_transactions(member_id, created_at desc);
create index if not exists xp_transactions_member_activity_date_idx on public.xp_transactions(member_id, activity, date_key);

alter table public.xp_accounts enable row level security;
alter table public.xp_transactions enable row level security;

create or replace function public.xp_ensure_account(p_member_id text, p_legacy_xp integer default 0)
returns table(member_id text, total_earned integer, total_spent integer, balance integer, migrated_legacy_xp integer, updated_at timestamptz)
language plpgsql
security definer
set search_path = public
as $$
begin
  insert into public.xp_accounts(member_id, total_earned, total_spent, balance, migrated_legacy_xp)
  values (p_member_id, greatest(coalesce(p_legacy_xp, 0), 0), 0, greatest(coalesce(p_legacy_xp, 0), 0), greatest(coalesce(p_legacy_xp, 0), 0))
  on conflict (member_id) do nothing;

  return query
  select a.member_id, a.total_earned, a.total_spent, a.balance, a.migrated_legacy_xp, a.updated_at
  from public.xp_accounts a
  where a.member_id = p_member_id;
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
set search_path = public
as $$
declare
  v_today date := (timezone('America/Recife', now()))::date;
  v_today_xp integer := 0;
  v_account public.xp_accounts%rowtype;
begin
  if coalesce(p_amount, 0) <= 0 then
    raise exception 'XP inválido';
  end if;

  perform 1 from public.xp_accounts where member_id = p_member_id for update;
  if not found then
    raise exception 'Conta XP não inicializada';
  end if;

  if exists(select 1 from public.xp_transactions where member_id = p_member_id and receipt_id = p_receipt_id) then
    select * into v_account from public.xp_accounts where member_id = p_member_id;
    return query select 0, true, false, v_account.total_earned, v_account.total_spent, v_account.balance;
    return;
  end if;

  if coalesce(p_daily_cap, 0) > 0 then
    select coalesce(sum(t.amount),0)::integer into v_today_xp
    from public.xp_transactions t
    where t.member_id = p_member_id and t.type = 'earn' and t.activity = p_activity and t.date_key = v_today;
    if v_today_xp + p_amount > p_daily_cap then
      select * into v_account from public.xp_accounts where member_id = p_member_id;
      return query select 0, false, true, v_account.total_earned, v_account.total_spent, v_account.balance;
      return;
    end if;
  end if;

  begin
    insert into public.xp_transactions(member_id, type, amount, activity, content_id, variant, receipt_id, description, date_key)
    values (p_member_id, 'earn', p_amount, p_activity, coalesce(p_content_id,''), coalesce(p_variant,''), p_receipt_id, coalesce(p_description,''), v_today);
  exception when unique_violation then
    select * into v_account from public.xp_accounts where member_id = p_member_id;
    return query select 0, true, false, v_account.total_earned, v_account.total_spent, v_account.balance;
    return;
  end;

  update public.xp_accounts
  set total_earned = total_earned + p_amount,
      balance = balance + p_amount,
      updated_at = now()
  where member_id = p_member_id
  returning * into v_account;

  return query select p_amount, false, false, v_account.total_earned, v_account.total_spent, v_account.balance;
end;
$$;

revoke all on public.xp_accounts from anon, authenticated;
revoke all on public.xp_transactions from anon, authenticated;
revoke all on function public.xp_ensure_account(text, integer) from public, anon, authenticated;
revoke all on function public.xp_award(text, text, text, text, text, integer, text, integer) from public, anon, authenticated;
