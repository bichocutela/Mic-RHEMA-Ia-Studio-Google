alter table public.custom_profile_badges
  add column if not exists challenge_metric text null,
  add column if not exists challenge_target integer null;

create table if not exists public.custom_badge_unlocks (
  member_id text not null,
  badge_id text not null references public.custom_profile_badges(id) on delete cascade,
  source text not null default 'challenge',
  unlocked_at timestamptz not null default now(),
  primary key (member_id, badge_id)
);

create index if not exists custom_badge_unlocks_member_idx
  on public.custom_badge_unlocks(member_id, unlocked_at desc);

alter table public.custom_badge_unlocks enable row level security;
revoke all on public.custom_badge_unlocks from anon, authenticated;
grant select, insert, update, delete on public.custom_badge_unlocks to service_role;

create or replace function public.xp_custom_badge_metric_count(
  p_member_id text,
  p_metric text,
  p_since timestamptz
)
returns integer
language plpgsql
stable
security definer
set search_path = ''
as $function$
declare
  v integer := 0;
begin
  if p_metric = 'bible_chapters' then
    select count(distinct content_id)::int into v from public.xp_transactions
      where member_id=p_member_id and type='earn' and activity='bible_chapter' and created_at>=p_since;
  elsif p_metric = 'devotionals' then
    select count(distinct content_id)::int into v from public.xp_transactions
      where member_id=p_member_id and type='earn' and activity='devotional' and created_at>=p_since;
  elsif p_metric = 'plan_themes' then
    select count(distinct content_id)::int into v from public.xp_transactions
      where member_id=p_member_id and type='earn' and activity='plan_theme' and created_at>=p_since;
  elsif p_metric = 'plans' then
    select count(distinct content_id)::int into v from public.xp_transactions
      where member_id=p_member_id and type='earn' and activity='plan_complete' and created_at>=p_since;
  elsif p_metric = 'books' then
    select count(distinct content_id)::int into v from public.xp_transactions
      where member_id=p_member_id and type='earn' and activity in ('book_10','book_complete') and created_at>=p_since;
  elsif p_metric = 'videos' then
    select count(distinct content_id)::int into v from public.xp_transactions
      where member_id=p_member_id and type='earn' and activity in ('video_10min','video_90') and created_at>=p_since;
  elsif p_metric = 'audios' then
    select count(distinct content_id)::int into v from public.xp_transactions
      where member_id=p_member_id and type='earn' and activity in ('audio_10min','audio_90') and created_at>=p_since;
  elsif p_metric = 'bible_news' then
    select count(distinct content_id)::int into v from public.xp_transactions
      where member_id=p_member_id and type='earn' and activity='news_read' and created_at>=p_since;
  elsif p_metric = 'active_minutes' then
    select (count(*)*5)::int into v from public.xp_transactions
      where member_id=p_member_id and type='earn' and activity='active_5min' and created_at>=p_since;
  elsif p_metric = 'quiz_correct' then
    select count(*)::int into v from public.xp_quiz_attempts
      where member_id=p_member_id and correct=true and created_at>=p_since;
  elsif p_metric = 'quiz_correct_no_easy_hint' then
    select count(*)::int into v from public.xp_quiz_attempts
      where member_id=p_member_id and correct=true and coalesce(variant,'')<>'easy_hint' and created_at>=p_since;
  elsif p_metric = 'quiz_correct_no_hint' then
    select count(*)::int into v from public.xp_quiz_attempts
      where member_id=p_member_id and correct=true and coalesce(variant,'')='' and created_at>=p_since;
  elsif p_metric = 'quiz_hard_correct' then
    select count(distinct content_id)::int into v from public.xp_transactions
      where member_id=p_member_id and type='earn' and activity='quiz_hard' and created_at>=p_since;
  elsif p_metric = 'xp_total' then
    select coalesce(sum(amount),0)::int into v from public.xp_transactions
      where member_id=p_member_id and type='earn' and created_at>=p_since;
  else
    v := 0;
  end if;
  return greatest(coalesce(v,0),0);
end;
$function$;

create or replace function public.xp_reconcile_custom_badges(p_member_id text)
returns table (
  badge_id text,
  current_value integer,
  target_value integer,
  unlocked boolean,
  newly_unlocked boolean
)
language plpgsql
security definer
set search_path = ''
as $function$
declare
  b record;
  v_current integer;
  v_exists boolean;
begin
  for b in
    select id, challenge_metric, challenge_target, created_at
    from public.custom_profile_badges
    where active=true and challenge_metric is not null and challenge_target is not null and challenge_target > 0
    order by special asc, sequence_no asc nulls last, created_at asc
  loop
    v_current := public.xp_custom_badge_metric_count(p_member_id, b.challenge_metric, b.created_at);
    select exists(
      select 1 from public.custom_badge_unlocks u where u.member_id=p_member_id and u.badge_id=b.id
    ) into v_exists;

    if not v_exists and v_current >= b.challenge_target then
      insert into public.custom_badge_unlocks(member_id,badge_id,source)
      values (p_member_id,b.id,'challenge')
      on conflict (member_id,badge_id) do nothing;
      badge_id := b.id;
      current_value := v_current;
      target_value := b.challenge_target;
      unlocked := true;
      newly_unlocked := true;
      return next;
    else
      badge_id := b.id;
      current_value := v_current;
      target_value := b.challenge_target;
      unlocked := v_exists;
      newly_unlocked := false;
      return next;
    end if;
  end loop;
end;
$function$;

revoke execute on function public.xp_custom_badge_metric_count(text,text,timestamptz) from public, anon, authenticated;
revoke execute on function public.xp_reconcile_custom_badges(text) from public, anon, authenticated;
grant execute on function public.xp_custom_badge_metric_count(text,text,timestamptz) to service_role;
grant execute on function public.xp_reconcile_custom_badges(text) to service_role;

comment on column public.custom_profile_badges.challenge_metric is
  'Métrica estruturada detectada no texto do desafio para desbloqueio automático no backend.';
comment on column public.custom_profile_badges.challenge_target is
  'Meta numérica do desafio; o backend acompanha apenas eventos ocorridos após a criação do emblema.';