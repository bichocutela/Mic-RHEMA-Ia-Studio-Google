create table if not exists public.xp_shop_items (
  id text primary key,
  name text not null,
  description text not null default '',
  cost integer not null check (cost > 0),
  category text not null default 'Personalização',
  kind text not null default 'digital' check (kind in ('digital','profile','physical')),
  image_url text not null default '',
  stock integer null check (stock is null or stock >= 0),
  limit_per_member integer not null default 1 check (limit_per_member > 0),
  active boolean not null default true,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table if not exists public.xp_redemptions (
  id uuid primary key default gen_random_uuid(),
  member_id text not null references public.xp_accounts(member_id) on delete cascade,
  item_id text not null references public.xp_shop_items(id),
  item_name text not null,
  cost integer not null check (cost > 0),
  status text not null check (status in ('liberado','pendente','entregue','cancelado')),
  redemption_code text not null unique,
  created_at timestamptz not null default now(),
  delivered_at timestamptz null
);

create index if not exists xp_redemptions_member_created_idx on public.xp_redemptions(member_id, created_at desc);
create index if not exists xp_redemptions_status_created_idx on public.xp_redemptions(status, created_at desc);

alter table public.xp_shop_items enable row level security;
alter table public.xp_redemptions enable row level security;

insert into public.xp_shop_items(id, name, description, cost, category, kind, stock, limit_per_member, active)
values
  ('wallpaper_salmo_23', 'Wallpaper Salmo 23', 'Arte digital exclusiva para levar uma mensagem de fé à tela do seu celular.', 100, 'Arte e fé', 'digital', null, 1, true),
  ('pack_wallpapers_rhema', 'Coleção de Wallpapers Rhema', 'Coleção digital de artes cristãs exclusivas da Jornada Rhema.', 300, 'Arte e fé', 'digital', null, 1, true),
  ('moldura_luz_promessa', 'Moldura Luz da Promessa', 'Recompensa cosmética especial para sua coleção de perfil.', 300, 'Perfil', 'profile', null, 1, true),
  ('tema_dourado_rhema', 'Tema Dourado Rhema', 'Recompensa visual premium para personalização da experiência MIC Rhema.', 500, 'Personalização', 'profile', null, 1, true),
  ('badge_leitor_palavra', 'Distintivo Leitor da Palavra', 'Distintivo cosmético de coleção para quem valoriza o estudo das Escrituras.', 750, 'Perfil', 'profile', null, 1, true)
on conflict (id) do nothing;

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
set search_path = public
as $$
declare
  v_account public.xp_accounts%rowtype;
  v_item public.xp_shop_items%rowtype;
  v_redemption public.xp_redemptions%rowtype;
  v_count integer := 0;
begin
  select * into v_account from public.xp_accounts where member_id = p_member_id for update;
  if not found then raise exception 'Conta XP não encontrada'; end if;

  select * into v_item from public.xp_shop_items where id = p_item_id for update;
  if not found or not v_item.active then raise exception 'Recompensa indisponível'; end if;

  if v_account.balance < v_item.cost then raise exception 'Saldo XP insuficiente'; end if;
  if v_item.stock is not null and v_item.stock <= 0 then raise exception 'Recompensa esgotada'; end if;

  select count(*)::integer into v_count
  from public.xp_redemptions
  where member_id = p_member_id and item_id = p_item_id and status <> 'cancelado';
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
    update public.xp_shop_items set stock = stock - 1, updated_at = now() where id = v_item.id;
  end if;

  update public.xp_accounts
  set total_spent = total_spent + v_item.cost,
      balance = balance - v_item.cost,
      updated_at = now()
  where member_id = p_member_id
  returning * into v_account;

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

revoke all on public.xp_shop_items from anon, authenticated;
revoke all on public.xp_redemptions from anon, authenticated;
revoke all on function public.xp_redeem(text, text) from public, anon, authenticated;
grant select, insert, update on public.xp_shop_items to service_role;
grant select, insert, update on public.xp_redemptions to service_role;
grant execute on function public.xp_redeem(text, text) to service_role;
