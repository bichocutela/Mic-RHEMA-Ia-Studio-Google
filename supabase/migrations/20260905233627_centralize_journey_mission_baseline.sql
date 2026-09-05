create table if not exists public.xp_journey_baselines (
  member_id text primary key references public.xp_accounts(member_id) on delete cascade,
  metrics jsonb not null default '{}'::jsonb,
  started_at timestamptz not null default now()
);

alter table public.xp_journey_baselines enable row level security;
revoke all on public.xp_journey_baselines from anon, authenticated;
grant select, insert on public.xp_journey_baselines to service_role;

create or replace function public.xp_journey_metric_counts_raw(p_member_id text)
returns jsonb
language sql
stable
security definer
set search_path = ''
as $function$
with tx as (
  select activity, content_id
  from public.xp_transactions
  where member_id = p_member_id and type = 'earn'
), core as (
  select
    count(distinct content_id) filter (where activity = 'bible_chapter')::int as bible_chapters,
    count(distinct content_id) filter (where activity = 'devotional')::int as devotionals,
    count(distinct content_id) filter (where activity = 'plan_theme')::int as plan_themes,
    count(distinct content_id) filter (where activity = 'plan_complete')::int as plans,
    count(distinct content_id) filter (where activity in ('book_10','book_complete'))::int as books,
    count(distinct content_id) filter (where activity in ('video_10min','video_90'))::int as videos,
    count(distinct content_id) filter (where activity in ('audio_10min','audio_90'))::int as audios,
    count(distinct content_id) filter (where activity = 'news_read')::int as bible_news,
    (count(*) filter (where activity = 'active_5min') * 5)::int as active_minutes
  from tx
), quiz as (
  select
    count(*) filter (where correct)::int as quiz_correct,
    count(*) filter (where correct and variant <> 'easy_hint')::int as quiz_correct_no_easy_hint,
    count(*) filter (where correct and variant = '')::int as quiz_correct_no_hint
  from public.xp_quiz_attempts
  where member_id = p_member_id
), hardq as (
  select count(distinct content_id)::int as quiz_hard_correct
  from tx where activity = 'quiz_hard'
)
select jsonb_build_object(
  'bible_chapters', core.bible_chapters,
  'devotionals', core.devotionals,
  'plan_themes', core.plan_themes,
  'plans', core.plans,
  'books', core.books,
  'videos', core.videos,
  'audios', core.audios,
  'bible_news', core.bible_news,
  'active_minutes', core.active_minutes,
  'total_activities', core.bible_chapters + core.devotionals + core.plan_themes + core.plans + core.books + core.videos + core.audios + core.bible_news,
  'quiz_correct', quiz.quiz_correct,
  'quiz_correct_no_easy_hint', quiz.quiz_correct_no_easy_hint,
  'quiz_correct_no_hint', quiz.quiz_correct_no_hint,
  'quiz_hard_correct', hardq.quiz_hard_correct
)
from core cross join quiz cross join hardq;
$function$;

create or replace function public.xp_capture_journey_baseline(p_member_id text)
returns jsonb
language plpgsql
security definer
set search_path = ''
as $function$
declare
  v_metrics jsonb;
begin
  select metrics into v_metrics from public.xp_journey_baselines where member_id = p_member_id;
  if found then return v_metrics; end if;
  v_metrics := public.xp_journey_metric_counts_raw(p_member_id);
  insert into public.xp_journey_baselines(member_id, metrics)
  values (p_member_id, coalesce(v_metrics, '{}'::jsonb))
  on conflict (member_id) do nothing;
  select metrics into v_metrics from public.xp_journey_baselines where member_id = p_member_id;
  return coalesce(v_metrics, '{}'::jsonb);
end;
$function$;

create or replace function public.xp_journey_metric_counts(p_member_id text)
returns jsonb
language plpgsql
stable
security definer
set search_path = ''
as $function$
declare
  r jsonb := public.xp_journey_metric_counts_raw(p_member_id);
  b jsonb := coalesce((select metrics from public.xp_journey_baselines where member_id = p_member_id), '{}'::jsonb);
  k text;
  result jsonb := '{}'::jsonb;
  rv int;
  bv int;
begin
  foreach k in array array['bible_chapters','devotionals','plan_themes','plans','books','videos','audios','bible_news','active_minutes','total_activities','quiz_correct','quiz_correct_no_easy_hint','quiz_correct_no_hint','quiz_hard_correct'] loop
    rv := coalesce((r->>k)::int,0);
    bv := coalesce((b->>k)::int,0);
    result := result || jsonb_build_object(k, greatest(rv - bv, 0));
  end loop;
  return result;
end;
$function$;

create or replace function public.xp_capture_journey_baseline_before_insert()
returns trigger
language plpgsql
security definer
set search_path = ''
as $function$
begin
  if new.type = 'earn'
     and new.activity not like 'quiz_%'
     and new.activity not like 'journey_mission_%'
     and new.activity not in ('legacy_sync','daily_mission','streak_7','streak_30') then
    perform public.xp_capture_journey_baseline(new.member_id);
  end if;
  return new;
end;
$function$;

drop trigger if exists xp_capture_journey_baseline_before_insert on public.xp_transactions;
create trigger xp_capture_journey_baseline_before_insert
before insert on public.xp_transactions
for each row execute function public.xp_capture_journey_baseline_before_insert();

create or replace function public.xp_validate_journey_mission_insert()
returns trigger
language plpgsql
security definer
set search_path = ''
as $function$
declare
  v_expected_activity text;
  v_expected_amount integer;
  v_m jsonb;
  v_ok boolean := false;
  q int;
begin
  if new.type <> 'earn' or new.activity not like 'journey_mission_%' then return new; end if;
  v_expected_activity := case
    when new.content_id like 'easy_%' then 'journey_mission_easy'
    when new.content_id like 'medium_%' then 'journey_mission_medium'
    when new.content_id like 'hard_%' then 'journey_mission_hard'
    else null end;
  v_expected_amount := case v_expected_activity
    when 'journey_mission_easy' then 15
    when 'journey_mission_medium' then 35
    when 'journey_mission_hard' then 70
    else null end;
  if v_expected_activity is null or new.activity <> v_expected_activity or new.amount <> v_expected_amount then
    raise exception 'Missão da Jornada inválida';
  end if;
  perform public.xp_capture_journey_baseline(new.member_id);
  v_m := public.xp_journey_metric_counts(new.member_id);
  q := coalesce((v_m->>'quiz_correct')::int,0);
  v_ok := case new.content_id
    when 'easy_first_chapter' then coalesce((v_m->>'bible_chapters')::int,0) >= 1
    when 'easy_devotional' then coalesce((v_m->>'devotionals')::int,0) >= 1
    when 'easy_plan_theme' then coalesce((v_m->>'plan_themes')::int,0) >= 1
    when 'easy_five_minutes' then coalesce((v_m->>'active_minutes')::int,0) >= 5
    when 'easy_three_activities' then coalesce((v_m->>'total_activities')::int,0) >= 3
    when 'easy_first_quiz_correct' then q >= 1
    when 'easy_three_quiz_correct' then q >= 3
    when 'medium_three_chapters' then coalesce((v_m->>'bible_chapters')::int,0) >= 3
    when 'medium_devotional_and_plan' then coalesce((v_m->>'devotionals')::int,0) >= 2 and coalesce((v_m->>'plan_themes')::int,0) >= 2
    when 'medium_active_student' then coalesce((v_m->>'active_minutes')::int,0) >= 15 and coalesce((v_m->>'total_activities')::int,0) >= 5
    when 'medium_media_study' then coalesce((v_m->>'books')::int,0) >= 1 and coalesce((v_m->>'videos')::int,0) >= 1
    when 'medium_bible_and_news' then coalesce((v_m->>'bible_chapters')::int,0) >= 3 and coalesce((v_m->>'bible_news')::int,0) >= 2
    when 'medium_five_quiz_correct' then q >= 5
    when 'medium_three_without_easy_hint' then coalesce((v_m->>'quiz_correct_no_easy_hint')::int,0) >= 3
    when 'hard_ten_chapters' then coalesce((v_m->>'bible_chapters')::int,0) >= 10
    when 'hard_hour_of_study' then coalesce((v_m->>'active_minutes')::int,0) >= 60 and coalesce((v_m->>'total_activities')::int,0) >= 15
    when 'hard_multimedia' then coalesce((v_m->>'books')::int,0) >= 3 and coalesce((v_m->>'videos')::int,0) >= 3 and coalesce((v_m->>'audios')::int,0) >= 2
    when 'hard_word_and_context' then coalesce((v_m->>'bible_chapters')::int,0) >= 10 and coalesce((v_m->>'bible_news')::int,0) >= 3 and coalesce((v_m->>'plan_themes')::int,0) >= 3
    when 'hard_full_journey' then coalesce((v_m->>'bible_chapters')::int,0) >= 10 and coalesce((v_m->>'devotionals')::int,0) >= 3 and coalesce((v_m->>'plan_themes')::int,0) >= 3 and coalesce((v_m->>'books')::int,0) >= 1 and coalesce((v_m->>'videos')::int,0) >= 1 and coalesce((v_m->>'audios')::int,0) >= 1 and coalesce((v_m->>'bible_news')::int,0) >= 1 and coalesce((v_m->>'active_minutes')::int,0) >= 90
    when 'hard_ten_without_easy_hint' then coalesce((v_m->>'quiz_correct_no_easy_hint')::int,0) >= 10
    when 'hard_five_hard_questions' then coalesce((v_m->>'quiz_hard_correct')::int,0) >= 5
    when 'hard_five_without_hint' then coalesce((v_m->>'quiz_correct_no_hint')::int,0) >= 5
    else false end;
  if not v_ok then raise exception 'Missão da Jornada ainda não foi concluída no ledger central'; end if;
  return new;
end;
$function$;

revoke execute on function public.xp_capture_journey_baseline(text) from public, anon, authenticated;
revoke execute on function public.xp_journey_metric_counts_raw(text) from public, anon, authenticated;
revoke execute on function public.xp_journey_metric_counts(text) from public, anon, authenticated;
grant execute on function public.xp_capture_journey_baseline(text) to service_role;
grant execute on function public.xp_journey_metric_counts_raw(text) to service_role;
grant execute on function public.xp_journey_metric_counts(text) to service_role;
