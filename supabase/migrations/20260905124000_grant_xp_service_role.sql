grant select, insert, update, delete on public.xp_accounts to service_role;
grant select, insert, update, delete on public.xp_transactions to service_role;
grant execute on function public.xp_ensure_account(text, integer) to service_role;
grant execute on function public.xp_award(text, text, text, text, text, integer, text, integer) to service_role;
