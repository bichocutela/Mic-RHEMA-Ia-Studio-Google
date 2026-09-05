create table if not exists public.xp_quiz_hints (
  member_id text not null references public.xp_accounts(member_id) on delete cascade,
  question_id text not null,
  max_hint_rank smallint not null default 0 check (max_hint_rank between 0 and 2),
  updated_at timestamptz not null default now(),
  primary key (member_id, question_id)
);

alter table public.xp_quiz_hints enable row level security;
revoke all on public.xp_quiz_hints from anon, authenticated;
grant select, insert, update on public.xp_quiz_hints to service_role;

create or replace function public.xp_record_quiz_hint(
  p_member_id text,
  p_question_id text,
  p_hint_rank integer
)
returns smallint
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_rank smallint;
begin
  if p_question_id is null or btrim(p_question_id) = '' then
    raise exception 'Pergunta do quiz inválida';
  end if;
  if p_hint_rank not between 1 and 2 then
    raise exception 'Nível de dica inválido';
  end if;
  perform 1 from public.xp_accounts as xa where xa.member_id = p_member_id;
  if not found then raise exception 'Conta XP não encontrada'; end if;

  insert into public.xp_quiz_hints(member_id, question_id, max_hint_rank, updated_at)
  values (p_member_id, p_question_id, p_hint_rank::smallint, now())
  on conflict (member_id, question_id) do update
    set max_hint_rank = greatest(public.xp_quiz_hints.max_hint_rank, excluded.max_hint_rank),
        updated_at = now()
  returning max_hint_rank into v_rank;
  return v_rank;
end;
$$;

revoke all on function public.xp_record_quiz_hint(text, text, integer) from public, anon, authenticated;
grant execute on function public.xp_record_quiz_hint(text, text, integer) to service_role;

create or replace function public.xp_journey_metric_counts(p_member_id text)
returns jsonb
language sql
security definer
set search_path = ''
stable
as $$
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
$$;

revoke all on function public.xp_journey_metric_counts(text) from public, anon, authenticated;
grant execute on function public.xp_journey_metric_counts(text) to service_role;

create or replace function public.xp_validate_journey_mission_insert()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
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
$$;

revoke all on function public.xp_validate_journey_mission_insert() from public, anon, authenticated;
drop trigger if exists xp_validate_journey_mission_before_insert on public.xp_transactions;
create trigger xp_validate_journey_mission_before_insert
before insert on public.xp_transactions
for each row execute function public.xp_validate_journey_mission_insert();

drop function if exists public.xp_submit_quiz(text, text, integer, text, text, boolean, integer, text);
create function public.xp_submit_quiz(
  p_member_id text,
  p_question_id text,
  p_selected_option integer,
  p_activity text,
  p_variant text,
  p_correct boolean,
  p_amount integer,
  p_description text
)
returns table(
  granted integer,
  duplicate boolean,
  correct boolean,
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
  v_attempt_id uuid;
  v_award record;
  v_requested_rank integer;
  v_stored_rank integer := 0;
  v_rank integer;
  v_variant text;
  v_base integer;
  v_amount integer;
  v_existing_correct boolean;
begin
  if p_question_id is null or btrim(p_question_id) = '' then raise exception 'Pergunta do quiz inválida'; end if;
  if p_selected_option not between 0 and 3 then raise exception 'Alternativa do quiz inválida'; end if;
  if p_activity not in ('quiz_easy','quiz_medium','quiz_hard') then raise exception 'Dificuldade do quiz inválida'; end if;
  if coalesce(p_variant,'') not in ('','subtle_hint','easy_hint') then raise exception 'Dica do quiz inválida'; end if;

  v_requested_rank := case coalesce(p_variant,'') when 'easy_hint' then 2 when 'subtle_hint' then 1 else 0 end;
  select qh.max_hint_rank into v_stored_rank from public.xp_quiz_hints qh
  where qh.member_id = p_member_id and qh.question_id = p_question_id;
  v_stored_rank := coalesce(v_stored_rank,0);
  v_rank := greatest(v_requested_rank, v_stored_rank);
  v_variant := case v_rank when 2 then 'easy_hint' when 1 then 'subtle_hint' else '' end;
  v_base := case p_activity when 'quiz_easy' then 10 when 'quiz_medium' then 20 when 'quiz_hard' then 30 end;
  v_amount := case v_rank when 2 then round(v_base * 0.70)::int when 1 then round(v_base * 0.90)::int else v_base end;

  select xa.* into v_account from public.xp_accounts xa where xa.member_id = p_member_id for update;
  if not found then raise exception 'Conta XP não encontrada'; end if;

  insert into public.xp_quiz_attempts(member_id, question_id, selected_option, variant, correct)
  values (p_member_id, p_question_id, p_selected_option, v_variant, coalesce(p_correct,false))
  on conflict (member_id, question_id) do nothing
  returning id into v_attempt_id;

  if v_attempt_id is null then
    select qa.correct into v_existing_correct from public.xp_quiz_attempts qa
    where qa.member_id = p_member_id and qa.question_id = p_question_id;
    return query select 0,true,coalesce(v_existing_correct,false),v_account.total_earned,v_account.total_spent,v_account.balance;
    return;
  end if;
  if not coalesce(p_correct,false) then
    return query select 0,false,false,v_account.total_earned,v_account.total_spent,v_account.balance;
    return;
  end if;

  select * into v_award from public.xp_award(
    p_member_id,p_activity,p_question_id,v_variant,'quiz:' || p_question_id,v_amount,p_description,0
  );
  return query select
    coalesce(v_award.granted,0)::integer,
    coalesce(v_award.duplicate,false)::boolean,
    true,
    coalesce(v_award.total_earned,v_account.total_earned)::integer,
    coalesce(v_award.total_spent,v_account.total_spent)::integer,
    coalesce(v_award.balance,v_account.balance)::integer;
end;
$$;

revoke all on function public.xp_submit_quiz(text, text, integer, text, text, boolean, integer, text) from public, anon, authenticated;
grant execute on function public.xp_submit_quiz(text, text, integer, text, text, boolean, integer, text) to service_role;
