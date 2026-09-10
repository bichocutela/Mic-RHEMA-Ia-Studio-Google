create or replace function public.sync_distinctive_shop_item() returns trigger
language plpgsql security invoker set search_path = public as $$
begin
  if new.purchasable then
    if new.free_for_all then
      raise exception 'Escolha apenas uma forma de liberação.';
    end if;
    if new.xp_cost <= 0 then
      raise exception 'Informe um valor em XP maior que zero.';
    end if;

    insert into public.xp_shop_items(
      id,name,description,cost,category,kind,image_url,stock,limit_per_member,active
    )
    values (
      'cosmetic:' || new.id,
      new.name,
      new.description,
      new.xp_cost,
      case when new.kind = 'moldura' then 'Molduras' else 'Distintivos' end,
      'profile',
      new.image_ref,
      null,
      1,
      new.active
    )
    on conflict(id) do update set
      name = excluded.name,
      description = excluded.description,
      cost = excluded.cost,
      category = excluded.category,
      kind = excluded.kind,
      image_url = excluded.image_url,
      active = excluded.active,
      updated_at = now();
  else
    update public.xp_shop_items
      set active = false, updated_at = now()
      where id = 'cosmetic:' || new.id;
  end if;
  return new;
end $$;

revoke execute on function public.sync_distinctive_shop_item() from public,anon,authenticated;
