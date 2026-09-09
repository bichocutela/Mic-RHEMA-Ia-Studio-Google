create table if not exists public.custom_profile_badges (
  id text primary key,
  sequence_no integer null unique,
  name text not null,
  description text not null default '',
  challenge text not null default '',
  image_ref text not null,
  special boolean not null default false,
  active boolean not null default true,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create index if not exists custom_profile_badges_active_order_idx
  on public.custom_profile_badges(active desc, sequence_no asc nulls last, created_at asc);

alter table public.custom_profile_badges enable row level security;

comment on table public.custom_profile_badges is
  'Catálogo ilimitado de emblemas de perfil criados pelo administrador. sequence_no continua 23, 24, 25...; especiais podem não ter número.';
