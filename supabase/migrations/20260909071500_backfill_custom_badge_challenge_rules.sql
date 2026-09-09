do $block$
declare
  r record;
begin
  for r in
    select id
    from public.custom_profile_badges
    where active = true and (challenge_metric is null or challenge_target is null)
    order by created_at asc
  loop
    begin
      -- Reassigning challenge invokes the structured detector added in the prior migration.
      update public.custom_profile_badges
      set challenge = challenge,
          updated_at = updated_at
      where id = r.id;
    exception when others then
      -- Preserve old/ambiguous badges instead of blocking deployment. They remain
      -- available for manual ADM unlock until the challenge is edited and validated.
      raise notice 'Custom badge % kept manual-only during challenge-rule backfill: %', r.id, sqlerrm;
    end;
  end loop;
end
$block$;
