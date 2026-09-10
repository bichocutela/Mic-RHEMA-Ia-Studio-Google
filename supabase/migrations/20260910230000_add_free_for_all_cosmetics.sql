alter table public.profile_cosmetics
  add column if not exists free_for_all boolean not null default false;

alter table public.profile_light_effects
  add column if not exists free_for_all boolean not null default false;

do $$
begin
  if not exists (
    select 1 from pg_constraint where conname = 'profile_cosmetics_release_mode_check'
  ) then
    alter table public.profile_cosmetics
      add constraint profile_cosmetics_release_mode_check
      check (not (purchasable and free_for_all));
  end if;

  if not exists (
    select 1 from pg_constraint where conname = 'profile_light_effects_release_mode_check'
  ) then
    alter table public.profile_light_effects
      add constraint profile_light_effects_release_mode_check
      check (not (purchasable and free_for_all));
  end if;
end $$;

comment on column public.profile_cosmetics.free_for_all is
  'Quando true, o cosmético fica disponível para todos sem compra, XP ou missão.';
comment on column public.profile_light_effects.free_for_all is
  'Quando true, o efeito fica disponível para todos sem compra, XP ou missão.';
