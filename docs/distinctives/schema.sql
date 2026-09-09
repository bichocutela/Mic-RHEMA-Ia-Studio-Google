alter table public.profile_cosmetics
 add column if not exists purchasable boolean not null default false,
 add column if not exists xp_cost integer not null default 0 check (xp_cost >= 0),
 add column if not exists emblem_ids jsonb not null default '[]'::jsonb check (jsonb_typeof(emblem_ids) = 'array');

-- Cosmetics and their shop listing must commit together. Existing purchase ledger is reused.
create or replace function public.sync_distinctive_shop_item() returns trigger
language plpgsql security invoker set search_path = public as $$
begin
 if new.kind <> 'distintivo' then return new; end if;
 if new.purchasable then
   if new.xp_cost <= 0 then raise exception 'Informe um valor em XP maior que zero.'; end if;
   insert into public.xp_shop_items(id,name,description,cost,category,kind,image_url,stock,limit_per_member,active)
   values ('cosmetic:' || new.id,new.name,new.description,new.xp_cost,'Distintivos','profile',new.image_ref,null,1,new.active)
   on conflict(id) do update set name=excluded.name,description=excluded.description,cost=excluded.cost,
     image_url=excluded.image_url,active=excluded.active,updated_at=now();
 else
   update public.xp_shop_items set active=false,updated_at=now() where id='cosmetic:' || new.id;
 end if;
 return new;
end $$;
revoke execute on function public.sync_distinctive_shop_item() from public,anon,authenticated;
drop trigger if exists sync_distinctive_shop_item on public.profile_cosmetics;
create trigger sync_distinctive_shop_item after insert or update on public.profile_cosmetics
for each row execute function public.sync_distinctive_shop_item();
