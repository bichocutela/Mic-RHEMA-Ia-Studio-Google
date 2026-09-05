drop function if exists public.xp_submit_quiz(text,text,integer,text,text,boolean,integer,text);

create function public.xp_submit_quiz(
  p_member_id text,
  p_question_id text,
  p_selected_option integer,
  p_activity text,
  p_variant text,
  p_correct boolean,
  p_amount integer,
  p_description text,
  p_auth_context text
)
returns table(granted integer, duplicate boolean, correct boolean, total_earned integer, total_spent integer, balance integer)
language plpgsql
security definer
set search_path = ''
as $function$
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
  if p_auth_context <> 'firebase_member_v1' then raise exception 'Quiz requer sessão Firebase autenticada'; end if;
  if p_question_id is null or btrim(p_question_id) = '' then raise exception 'Pergunta do quiz inválida'; end if;
  if p_selected_option not between 0 and 3 then raise exception 'Alternativa do quiz inválida'; end if;
  if p_activity not in ('quiz_easy','quiz_medium','quiz_hard') then raise exception 'Dificuldade do quiz inválida'; end if;
  if coalesce(p_variant,'') not in ('','subtle_hint','easy_hint') then raise exception 'Dica do quiz inválida'; end if;

  perform pg_catalog.pg_advisory_xact_lock(pg_catalog.hashtextextended(p_member_id || ':' || p_question_id, 0));

  v_requested_rank := case coalesce(p_variant,'') when 'easy_hint' then 2 when 'subtle_hint' then 1 else 0 end;
  select qh.max_hint_rank into v_stored_rank from public.xp_quiz_hints qh where qh.member_id=p_member_id and qh.question_id=p_question_id;
  v_stored_rank := coalesce(v_stored_rank,0);
  v_rank := greatest(v_requested_rank,v_stored_rank);
  v_variant := case v_rank when 2 then 'easy_hint' when 1 then 'subtle_hint' else '' end;
  v_base := case p_activity when 'quiz_easy' then 10 when 'quiz_medium' then 20 when 'quiz_hard' then 30 end;
  v_amount := case v_rank when 2 then round(v_base*.70)::int when 1 then round(v_base*.90)::int else v_base end;

  select xa.* into v_account from public.xp_accounts xa where xa.member_id=p_member_id for update;
  if not found then raise exception 'Conta XP não encontrada'; end if;

  insert into public.xp_quiz_attempts(member_id,question_id,selected_option,variant,correct)
  values(p_member_id,p_question_id,p_selected_option,v_variant,coalesce(p_correct,false))
  on conflict(member_id,question_id) do nothing returning id into v_attempt_id;

  if v_attempt_id is null then
    select qa.correct into v_existing_correct from public.xp_quiz_attempts qa where qa.member_id=p_member_id and qa.question_id=p_question_id;
    return query select 0,true,coalesce(v_existing_correct,false),v_account.total_earned,v_account.total_spent,v_account.balance;
    return;
  end if;
  if not coalesce(p_correct,false) then
    return query select 0,false,false,v_account.total_earned,v_account.total_spent,v_account.balance;
    return;
  end if;

  select * into v_award from public.xp_award(p_member_id,p_activity,p_question_id,v_variant,'quiz:'||p_question_id,v_amount,p_description,0);
  return query select coalesce(v_award.granted,0)::int,coalesce(v_award.duplicate,false)::boolean,true,
    coalesce(v_award.total_earned,v_account.total_earned)::int,
    coalesce(v_award.total_spent,v_account.total_spent)::int,
    coalesce(v_award.balance,v_account.balance)::int;
end;
$function$;

revoke all on function public.xp_submit_quiz(text,text,integer,text,text,boolean,integer,text,text) from public,anon,authenticated;
grant execute on function public.xp_submit_quiz(text,text,integer,text,text,boolean,integer,text,text) to service_role;