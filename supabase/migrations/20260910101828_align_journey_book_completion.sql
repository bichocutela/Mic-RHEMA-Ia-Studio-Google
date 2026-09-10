create or replace function public.xp_journey_metric_counts_raw(p_member_id text)
returns jsonb
language sql
stable security definer
set search_path to ''
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
    count(distinct content_id) filter (where activity = 'book_complete')::int as books,
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

create or replace function public.xp_daily_mission_state(p_member_id text)
returns jsonb
language plpgsql
stable security definer
set search_path to ''
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
    count(*) filter (where xt.activity in ('devotional','plan_theme','book_complete','ibr_lesson'))::integer,
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