create or replace function public.xp_submit_quiz(
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
begin
  if p_question_id is null or btrim(p_question_id) = '' then
    raise exception 'Pergunta do quiz inválida';
  end if;
  if p_selected_option not between 0 and 3 then
    raise exception 'Alternativa do quiz inválida';
  end if;
  if p_activity not in ('quiz_easy', 'quiz_medium', 'quiz_hard') then
    raise exception 'Dificuldade do quiz inválida';
  end if;
  if coalesce(p_amount, 0) < 0 then
    raise exception 'Valor de XP inválido';
  end if;

  select xa.* into v_account
  from public.xp_accounts as xa
  where xa.member_id = p_member_id
  for update;
  if not found then raise exception 'Conta XP não encontrada'; end if;

  insert into public.xp_quiz_attempts(member_id, question_id, selected_option, variant, correct)
  values (p_member_id, p_question_id, p_selected_option, coalesce(p_variant, ''), coalesce(p_correct, false))
  on conflict (member_id, question_id) do nothing
  returning id into v_attempt_id;

  if v_attempt_id is null then
    return query select 0, true, false, v_account.total_earned, v_account.total_spent, v_account.balance;
    return;
  end if;

  if not coalesce(p_correct, false) then
    return query select 0, false, false, v_account.total_earned, v_account.total_spent, v_account.balance;
    return;
  end if;

  select * into v_award
  from public.xp_award(
    p_member_id,
    p_activity,
    p_question_id,
    coalesce(p_variant, ''),
    'quiz:' || p_question_id,
    p_amount,
    p_description,
    0
  );

  return query select
    coalesce(v_award.granted, 0)::integer,
    coalesce(v_award.duplicate, false)::boolean,
    true,
    coalesce(v_award.total_earned, v_account.total_earned)::integer,
    coalesce(v_award.total_spent, v_account.total_spent)::integer,
    coalesce(v_award.balance, v_account.balance)::integer;
end;
$$;

revoke all on function public.xp_submit_quiz(text, text, integer, text, text, boolean, integer, text) from public, anon, authenticated;
grant execute on function public.xp_submit_quiz(text, text, integer, text, text, boolean, integer, text) to service_role;

drop function if exists public.xp_submit_quiz(text, text, integer, text, boolean, integer, text);
