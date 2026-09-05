create table if not exists public.xp_media_progress (
  id uuid primary key default gen_random_uuid(),
  member_id text not null references public.xp_accounts(member_id) on delete cascade,
  media_type text not null check (media_type in ('book', 'audio', 'video')),
  content_id text not null,
  content_hash text generated always as (md5(content_id)) stored,
  active_ms bigint not null default 0 check (active_ms >= 0),
  consumed_ms bigint not null default 0 check (consumed_ms >= 0),
  last_position_ms bigint not null default 0 check (last_position_ms >= 0),
  duration_ms bigint not null default 0 check (duration_ms >= 0),
  max_fraction numeric(6,5) not null default 0 check (max_fraction >= 0 and max_fraction <= 1),
  ten_awarded boolean not null default false,
  complete_awarded boolean not null default false,
  last_seen_at timestamptz not null default now(),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  unique(member_id, media_type, content_hash)
);

create index if not exists xp_media_progress_member_updated_idx
  on public.xp_media_progress(member_id, updated_at desc);

alter table public.xp_media_progress enable row level security;
revoke all on public.xp_media_progress from anon, authenticated;
grant select, insert, update on public.xp_media_progress to service_role;

-- Mídia nunca pode ser premiada pelo RPC genérico sem a marca interna do
-- rastreador verificado. xp-engine usa variant vazio, então tentativas diretas
-- passam a falhar mesmo que um cliente conheça um ID/URL válido.
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
returns table(
  granted integer,
  duplicate boolean,
  cap_reached boolean,
  total_earned integer,
  total_spent integer,
  balance integer
)
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

  if p_activity in ('book_10', 'book_complete', 'audio_10min', 'audio_90', 'video_10min', 'video_90')
     and coalesce(p_variant, '') <> 'verified_media' then
    raise exception 'Progresso de mídia não verificado';
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

revoke all on function public.xp_award(text, text, text, text, text, integer, text, integer) from public, anon, authenticated;
grant execute on function public.xp_award(text, text, text, text, text, integer, text, integer) to service_role;

create or replace function public.xp_record_media_progress(
  p_member_id text,
  p_media_type text,
  p_content_id text,
  p_position_ms bigint,
  p_duration_ms bigint,
  p_fraction numeric,
  p_is_active boolean default true
)
returns table(
  ten_granted integer,
  complete_granted integer,
  total_earned integer,
  total_spent integer,
  balance integer,
  active_ms bigint,
  consumed_ms bigint,
  max_fraction numeric
)
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_now timestamptz := clock_timestamp();
  v_row public.xp_media_progress%rowtype;
  v_account public.xp_accounts%rowtype;
  v_elapsed_ms bigint := 0;
  v_position bigint := greatest(coalesce(p_position_ms, 0), 0);
  v_duration bigint := greatest(coalesce(p_duration_ms, 0), 0);
  v_fraction numeric := least(greatest(coalesce(p_fraction, 0), 0), 1);
  v_position_delta bigint := 0;
  v_max_natural_delta bigint := 0;
  v_ten_activity text;
  v_complete_activity text;
  v_ten_amount integer;
  v_complete_amount integer;
  v_ten_description text;
  v_complete_description text;
  v_ten_result record;
  v_complete_result record;
  v_ten_granted integer := 0;
  v_complete_granted integer := 0;
  v_should_ten boolean := false;
  v_should_complete boolean := false;
begin
  if p_media_type not in ('book', 'audio', 'video') then
    raise exception 'Tipo de mídia inválido';
  end if;
  if p_content_id is null or btrim(p_content_id) = '' or length(p_content_id) > 1000 then
    raise exception 'Conteúdo de mídia inválido';
  end if;

  perform 1 from public.xp_accounts as xa where xa.member_id = p_member_id for update;
  if not found then raise exception 'Conta XP não inicializada'; end if;

  insert into public.xp_media_progress(member_id, media_type, content_id, last_position_ms, duration_ms, max_fraction, last_seen_at)
  values (p_member_id, p_media_type, p_content_id, v_position, v_duration, v_fraction, v_now)
  on conflict (member_id, media_type, content_hash) do nothing;

  select xmp.* into v_row
  from public.xp_media_progress as xmp
  where xmp.member_id = p_member_id
    and xmp.media_type = p_media_type
    and xmp.content_hash = md5(p_content_id)
  for update;

  v_elapsed_ms := greatest(
    least((extract(epoch from (v_now - v_row.last_seen_at)) * 1000)::bigint, 5000),
    0
  );

  -- Só o tempo transcorrido no relógio do servidor pode avançar atividade.
  if coalesce(p_is_active, false) and v_elapsed_ms > 0 then
    v_row.active_ms := v_row.active_ms + v_elapsed_ms;
  end if;

  -- Áudio/vídeo acumulam apenas avanço natural da posição. Saltos grandes de
  -- seek não contam como conteúdo consumido. A margem suporta reprodução até 2x.
  if p_media_type in ('audio', 'video') and coalesce(p_is_active, false) and v_elapsed_ms > 0 then
    v_position_delta := v_position - v_row.last_position_ms;
    v_max_natural_delta := greatest(2500, (v_elapsed_ms * 5 / 2) + 1500);
    if v_position_delta >= 0 and v_position_delta <= v_max_natural_delta then
      v_row.consumed_ms := v_row.consumed_ms + v_position_delta;
    end if;
  end if;

  v_row.last_position_ms := v_position;
  if v_duration > v_row.duration_ms then v_row.duration_ms := v_duration; end if;
  if v_fraction > v_row.max_fraction then v_row.max_fraction := v_fraction; end if;
  v_row.last_seen_at := v_now;
  v_row.updated_at := v_now;

  if p_media_type = 'book' then
    v_ten_activity := 'book_10';
    v_complete_activity := 'book_complete';
    v_ten_amount := 3;
    v_complete_amount := 25;
    v_ten_description := 'Progresso de leitura no livro';
    v_complete_description := 'Livro concluído';
    v_should_ten := not v_row.ten_awarded and v_row.max_fraction >= 0.10 and v_row.active_ms >= 30000;
    v_should_complete := not v_row.complete_awarded and v_row.max_fraction >= 0.95 and v_row.active_ms >= 120000;
  elsif p_media_type = 'audio' then
    v_ten_activity := 'audio_10min';
    v_complete_activity := 'audio_90';
    v_ten_amount := 3;
    v_complete_amount := 8;
    v_ten_description := '10 minutos de áudio';
    v_complete_description := 'Áudio concluído';
    v_should_ten := not v_row.ten_awarded and v_row.consumed_ms >= 600000;
    v_should_complete := not v_row.complete_awarded
      and v_row.duration_ms >= 60000
      and v_row.max_fraction >= 0.90
      and v_row.active_ms >= 120000
      and v_row.consumed_ms >= (v_row.duration_ms * 9 / 10);
  else
    v_ten_activity := 'video_10min';
    v_complete_activity := 'video_90';
    v_ten_amount := 3;
    v_complete_amount := 8;
    v_ten_description := '10 minutos de vídeo';
    v_complete_description := 'Vídeo concluído';
    v_should_ten := not v_row.ten_awarded and v_row.consumed_ms >= 600000;
    v_should_complete := not v_row.complete_awarded
      and v_row.duration_ms >= 60000
      and v_row.max_fraction >= 0.90
      and v_row.active_ms >= 120000
      and v_row.consumed_ms >= (v_row.duration_ms * 9 / 10);
  end if;

  if v_should_ten then
    select * into v_ten_result
    from public.xp_award(
      p_member_id, v_ten_activity, p_content_id, 'verified_media',
      v_ten_activity || ':' || md5(p_content_id),
      v_ten_amount, v_ten_description, 0
    );
    v_ten_granted := coalesce(v_ten_result.granted, 0);
    v_row.ten_awarded := true;
  end if;

  if v_should_complete then
    select * into v_complete_result
    from public.xp_award(
      p_member_id, v_complete_activity, p_content_id, 'verified_media',
      v_complete_activity || ':' || md5(p_content_id),
      v_complete_amount, v_complete_description, 0
    );
    v_complete_granted := coalesce(v_complete_result.granted, 0);
    v_row.complete_awarded := true;
  end if;

  update public.xp_media_progress as xmp
  set active_ms = v_row.active_ms,
      consumed_ms = v_row.consumed_ms,
      last_position_ms = v_row.last_position_ms,
      duration_ms = v_row.duration_ms,
      max_fraction = v_row.max_fraction,
      ten_awarded = v_row.ten_awarded,
      complete_awarded = v_row.complete_awarded,
      last_seen_at = v_row.last_seen_at,
      updated_at = v_row.updated_at
  where xmp.id = v_row.id;

  select xa.* into v_account from public.xp_accounts as xa where xa.member_id = p_member_id;

  return query select
    v_ten_granted,
    v_complete_granted,
    v_account.total_earned,
    v_account.total_spent,
    v_account.balance,
    v_row.active_ms,
    v_row.consumed_ms,
    v_row.max_fraction;
end;
$$;

revoke all on function public.xp_record_media_progress(text, text, text, bigint, bigint, numeric, boolean) from public, anon, authenticated;
grant execute on function public.xp_record_media_progress(text, text, text, bigint, bigint, numeric, boolean) to service_role;
