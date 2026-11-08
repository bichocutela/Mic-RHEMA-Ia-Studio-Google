-- No credential values belong here. Import values directly between trusted servers.
begin;
create or replace function public.micrhema_runtime_configuration()
returns jsonb language sql stable security invoker set search_path='' as $$
  select coalesce(jsonb_object_agg(substring(name from 18),decrypted_secret),'{}'::jsonb)
  from vault.decrypted_secrets
  where name in (
    'micrhema_runtime_FIREBASE_SERVICE_ACCOUNT_JSON','micrhema_runtime_FIREBASE_PROJECT_ID',
    'micrhema_runtime_RHEMA_ADMIN_PASSWORD','micrhema_runtime_RHEMA_ADMIN_PASSWORD_SHA256',
    'micrhema_runtime_FIREBASE_WEB_API_KEY','micrhema_runtime_YOUTUBE_API_KEY'
  );
$$;
revoke all on function public.micrhema_runtime_configuration() from public,anon,authenticated;
grant execute on function public.micrhema_runtime_configuration() to service_role;

create or replace function public.micrhema_import_runtime_configuration(config jsonb)
returns integer language plpgsql security invoker set search_path='' as $$
declare k text; v text; sid uuid; imported integer:=0;
begin
  for k,v in select key,value from jsonb_each_text(config) loop
    if k not in ('FIREBASE_SERVICE_ACCOUNT_JSON','FIREBASE_PROJECT_ID','RHEMA_ADMIN_PASSWORD',
      'RHEMA_ADMIN_PASSWORD_SHA256','FIREBASE_WEB_API_KEY','YOUTUBE_API_KEY') or length(v)=0 then continue; end if;
    select id into sid from vault.secrets where name='micrhema_runtime_'||k;
    if sid is null then
      perform vault.create_secret(v,'micrhema_runtime_'||k,'MICRHEMA server configuration migrated from original project');
    else
      perform vault.update_secret(sid,v);
    end if;
    imported:=imported+1;
  end loop;
  return imported;
end;
$$;
revoke all on function public.micrhema_import_runtime_configuration(jsonb) from public,anon,authenticated;
grant execute on function public.micrhema_import_runtime_configuration(jsonb) to service_role;
commit;
