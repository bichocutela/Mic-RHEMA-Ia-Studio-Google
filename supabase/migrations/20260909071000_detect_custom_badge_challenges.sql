create or replace function public.detect_custom_badge_challenge_rule()
returns trigger
language plpgsql
security definer
set search_path = ''
as $function$
declare
  v text;
  v_target integer;
begin
  v := lower(coalesce(new.challenge,''));
  v_target := nullif((regexp_match(v, '([0-9]+)'))[1], '')::integer;
  if v_target is null or v_target <= 0 then v_target := 1; end if;

  if v ~ 'xp' then
    new.challenge_metric := 'xp_total';
  elsif v ~ 'tema' and v ~ 'plano' then
    new.challenge_metric := 'plan_themes';
  elsif v ~ 'cap[ií]tulo' or v ~ 'capitulo' then
    new.challenge_metric := 'bible_chapters';
  elsif v ~ 'devocional' then
    new.challenge_metric := 'devotionals';
  elsif v ~ 'livro' then
    new.challenge_metric := 'books';
  elsif v ~ 'v[ií]deo' or v ~ 'video' then
    new.challenge_metric := 'videos';
  elsif v ~ '[áa]udio' or v ~ 'audio' then
    new.challenge_metric := 'audios';
  elsif v ~ 'not[ií]cia' or v ~ 'noticia' then
    new.challenge_metric := 'bible_news';
  elsif v ~ 'plano' then
    new.challenge_metric := 'plans';
  elsif v ~ 'minuto' then
    new.challenge_metric := 'active_minutes';
  elsif (v ~ 'sem' and v ~ 'dica' and v ~ 'f[aá]cil') then
    new.challenge_metric := 'quiz_correct_no_easy_hint';
  elsif (v ~ 'sem' and v ~ 'dica') then
    new.challenge_metric := 'quiz_correct_no_hint';
  elsif v ~ 'dif[ií]cil' then
    new.challenge_metric := 'quiz_hard_correct';
  elsif v ~ 'acerto' or v ~ 'resposta' or v ~ 'pergunta' or v ~ 'quiz' then
    new.challenge_metric := 'quiz_correct';
  else
    raise exception 'Desafio não reconhecido automaticamente. Use uma meta com número e uma atividade reconhecida: capítulos, devocionais, temas de plano, planos, livros, vídeos, áudios, notícias, minutos, respostas/acertos, perguntas difíceis ou XP.';
  end if;

  new.challenge_target := v_target;
  return new;
end;
$function$;

drop trigger if exists detect_custom_badge_challenge_rule_before_write on public.custom_profile_badges;
create trigger detect_custom_badge_challenge_rule_before_write
before insert or update of challenge on public.custom_profile_badges
for each row execute function public.detect_custom_badge_challenge_rule();

comment on function public.detect_custom_badge_challenge_rule() is
  'Transforma o texto do desafio em uma regra estruturada; rejeita desafios ambíguos para impedir emblemas impossíveis de detectar.';
