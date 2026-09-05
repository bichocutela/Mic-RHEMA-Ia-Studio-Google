-- Recompensa digital só pode ser vendida quando existe um conteúdo configurado.
-- O ADM pode reativar normalmente depois de preencher image_url/arquivo de entrega.
update public.xp_shop_items
set active = false,
    updated_at = now()
where kind = 'digital'
  and coalesce(nullif(btrim(image_url), ''), '') = '';
