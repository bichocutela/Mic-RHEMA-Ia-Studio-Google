import { useCallback, useEffect, useMemo, useState, type CSSProperties } from "react";
import { Check, RefreshCcw, Save, Sparkles, Star } from "lucide-react";
import { toast } from "sonner";
import { loadPwaXpDashboard } from "@/lib/xp";
import {
  availablePwaCosmetics,
  availablePwaEffects,
  emptyPwaProfileSelections,
  loadPwaCustomizationCatalog,
  loadPwaProfileSelections,
  normalizePwaSelections,
  resolvePwaXpAssetUrl,
  savePwaProfileSelections,
  type PwaCustomizationCatalog,
  type PwaLightEffect,
  type PwaProfileCosmetic,
  type PwaProfileSelections,
} from "@/lib/profile-customization";
import { BiblicalBadgeAvatar } from "./BiblicalBadgeAvatar";
import "./PwaProfileCustomization.css";

type LoadedCustomization = {
  catalog: PwaCustomizationCatalog;
  distinctives: PwaProfileCosmetic[];
  frames: PwaProfileCosmetic[];
  effects: PwaLightEffect[];
  selections: PwaProfileSelections;
};

function usePwaCustomization(memberId: string, badgeId: string) {
  const [data, setData] = useState<LoadedCustomization | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const reload = useCallback(async (force = false) => {
    if (!memberId) return;
    setLoading(true);
    setError("");
    try {
      const [catalog, dashboard, selections] = await Promise.all([
        loadPwaCustomizationCatalog(force),
        loadPwaXpDashboard(),
        loadPwaProfileSelections(memberId),
      ]);
      const ownedIds = new Set(dashboard.entitlements.map((item) => item.item_id));
      const distinctives = availablePwaCosmetics(catalog.distinctives, "distintivo", badgeId, ownedIds);
      const frames = availablePwaCosmetics(catalog.frames, "moldura", badgeId, ownedIds);
      const effects = availablePwaEffects(catalog.effects, badgeId, ownedIds);
      setData({ catalog, distinctives, frames, effects, selections: normalizePwaSelections(selections, distinctives, frames, effects) });
    } catch (failure) {
      setError(failure instanceof Error ? failure.message : "Não foi possível carregar as personalizações.");
    } finally {
      setLoading(false);
    }
  }, [memberId, badgeId]);

  useEffect(() => { void reload(); }, [reload]);
  return { data, setData, loading, error, reload };
}

function CosmeticImage({ item, className = "" }: { item: PwaProfileCosmetic; className?: string }) {
  const url = resolvePwaXpAssetUrl(item.image_ref);
  if (url) return <img className={className} src={url} alt={item.name} loading="lazy"/>;
  if (item.id === "moldura_luz_promessa") return <span className={`${className} pwa-builtin-promise-frame`} aria-label={item.name}/>;
  return <span className={`${className} pwa-builtin-reader-badge`} aria-label={item.name}><Star fill="currentColor"/></span>;
}

function effectStyle(effect: PwaLightEffect): CSSProperties {
  const strength = effect.tone === "forte" ? 1 : effect.tone === "suave" ? 0.62 : 0.82;
  return { "--profile-effect-color": effect.color_hex, "--profile-effect-strength": strength } as CSSProperties;
}

export function PwaProfileShowcase({
  avatarId,
  badgeId,
  size = 180,
  distinctives = [],
  frame = null,
  effect = null,
  selections = emptyPwaProfileSelections(),
}: {
  avatarId: string;
  badgeId: string;
  size?: number;
  distinctives?: PwaProfileCosmetic[];
  frame?: PwaProfileCosmetic | null;
  effect?: PwaLightEffect | null;
  selections?: PwaProfileSelections;
}) {
  const distinctiveById = useMemo(() => new Map(distinctives.map((item) => [item.id, item])), [distinctives]);
  const primary = distinctiveById.get(selections.primaryDistinctiveId) || null;
  const featured = selections.featuredDistinctiveIds.map((id) => distinctiveById.get(id)).filter((item): item is PwaProfileCosmetic => Boolean(item)).slice(0, 4);
  const effectType = String(effect?.effect_type || "orbit").replace(/[^a-z0-9_-]/gi, "-").toLowerCase();

  return <div className="pwa-profile-showcase" style={{ width: size, height: size }}>
    {effect && <span className={`pwa-profile-light-effect effect-${effectType}`} style={effectStyle(effect)} aria-label={effect.name}/>} 
    <span className="pwa-profile-showcase-avatar"><BiblicalBadgeAvatar avatarId={avatarId} badgeId={badgeId} size={size} title="Prévia do perfil"/></span>
    {frame && <CosmeticImage item={frame} className="pwa-profile-showcase-frame"/>}
    {primary && <span className="pwa-profile-primary-distinctive"><CosmeticImage item={primary}/></span>}
    {featured.length > 0 && <span className="pwa-profile-featured-distinctives">{featured.map((item) => <CosmeticImage key={item.id} item={item}/>)}</span>}
  </div>;
}

export function PwaProfileLiveShowcase({ memberId, avatarId, badgeId, size = 96 }: { memberId: string; avatarId: string; badgeId: string; size?: number }) {
  const { data, reload } = usePwaCustomization(memberId, badgeId);
  useEffect(() => {
    const refresh = () => void reload(true);
    window.addEventListener("micrhema:pwa-customization-updated", refresh);
    return () => window.removeEventListener("micrhema:pwa-customization-updated", refresh);
  }, [reload]);
  if (!data) return <BiblicalBadgeAvatar avatarId={avatarId} badgeId={badgeId} size={size} title="Avatar do perfil"/>;
  const frame = data.frames.find((item) => item.id === data.selections.selectedProfileFrameId) || null;
  const effect = data.effects.find((item) => item.id === data.selections.selectedProfileEffectId) || null;
  return <PwaProfileShowcase avatarId={avatarId} badgeId={badgeId} size={size} distinctives={data.distinctives} frame={frame} effect={effect} selections={data.selections}/>;
}

function ChoiceCard({ selected, title, subtitle, image, onClick }: { selected: boolean; title: string; subtitle?: string; image?: React.ReactNode; onClick: () => void }) {
  return <button className={`pwa-custom-choice ${selected ? "selected" : ""}`} onClick={onClick} type="button">
    <span className="pwa-custom-choice-image">{image || <span className="pwa-custom-none">—</span>}</span>
    <span><strong>{title}</strong>{subtitle && <small>{subtitle}</small>}</span>
    {selected && <Check size={18}/>} 
  </button>;
}

export function PwaProfileCustomizationPanel({ memberId, avatarId, badgeId }: { memberId: string; avatarId: string; badgeId: string }) {
  const { data, setData, loading, error, reload } = usePwaCustomization(memberId, badgeId);
  const [draft, setDraft] = useState<PwaProfileSelections>(emptyPwaProfileSelections());
  const [saving, setSaving] = useState(false);

  useEffect(() => { if (data) setDraft(data.selections); }, [data]);

  const selectedFrame = data?.frames.find((item) => item.id === draft.selectedProfileFrameId) || null;
  const selectedEffect = data?.effects.find((item) => item.id === draft.selectedProfileEffectId) || null;
  const dirty = Boolean(data) && JSON.stringify(draft) !== JSON.stringify(data.selections);

  const save = async () => {
    if (!data || saving) return;
    const normalized = normalizePwaSelections(draft, data.distinctives, data.frames, data.effects);
    setSaving(true);
    try {
      await savePwaProfileSelections(memberId, normalized);
      setDraft(normalized);
      setData({ ...data, selections: normalized });
      window.dispatchEvent(new Event("micrhema:pwa-customization-updated"));
      toast.success("Personalização sincronizada com o Android.");
    } catch (failure) {
      toast.error(failure instanceof Error ? failure.message : "Não foi possível salvar a personalização.");
    } finally {
      setSaving(false);
    }
  };

  const toggleFeatured = (id: string) => {
    setDraft((current) => {
      if (current.featuredDistinctiveIds.includes(id)) return { ...current, featuredDistinctiveIds: current.featuredDistinctiveIds.filter((item) => item !== id) };
      if (current.featuredDistinctiveIds.length >= 4) { toast.info("Você pode destacar até 4 distintivos."); return current; }
      return { ...current, featuredDistinctiveIds: [...current.featuredDistinctiveIds, id] };
    });
  };

  if (loading && !data) return <div className="pwa-custom-loading"><span className="pwa-route-spinner"/><p>Sincronizando personalizações…</p></div>;
  if (!data) return <div className="pwa-custom-error"><p>{error || "Não foi possível carregar as personalizações."}</p><button className="profile-v2-refresh" onClick={() => void reload(true)}><RefreshCcw size={17}/> Tentar novamente</button></div>;

  return <div className="pwa-profile-customization">
    <div className="profile-v2-section-heading"><div><strong>Personalização do perfil</strong><small>Mesmos itens e mesmas regras de liberação do Android.</small></div><Sparkles size={22}/></div>

    <div className="pwa-custom-preview-card">
      <PwaProfileShowcase avatarId={avatarId} badgeId={badgeId} size={180} distinctives={data.distinctives} frame={selectedFrame} effect={selectedEffect} selections={draft}/>
      <div><strong>Prévia em tempo real</strong><small>Somente itens que sua conta realmente possui ou que foram liberados para este emblema aparecem abaixo.</small></div>
    </div>

    <section className="pwa-custom-group">
      <header><strong>Distintivo principal</strong><small>Itens da Loja XP aparecem aqui somente depois do resgate.</small></header>
      <div className="pwa-custom-grid">
        <ChoiceCard selected={!draft.primaryDistinctiveId} title="Sem distintivo" onClick={() => setDraft((current) => ({ ...current, primaryDistinctiveId: "", featuredDistinctiveIds: [] }))}/>
        {data.distinctives.map((item) => <ChoiceCard key={item.id} selected={draft.primaryDistinctiveId === item.id} title={item.name} subtitle={item.free_for_all ? "Gratuito p/ Todos" : item.purchasable ? "Adquirido na Loja XP" : "Liberado por emblema/ADM"} image={<CosmeticImage item={item}/>} onClick={() => setDraft((current) => ({ ...current, primaryDistinctiveId: item.id }))}/>) }
      </div>
    </section>

    {data.distinctives.length > 0 && <section className="pwa-custom-group">
      <header><strong>Escolher destaques</strong><small>Selecione até 4 distintivos para aparecerem junto ao perfil.</small></header>
      <div className="pwa-featured-list">{data.distinctives.map((item) => { const checked = draft.featuredDistinctiveIds.includes(item.id); return <button type="button" key={item.id} className={checked ? "selected" : ""} onClick={() => toggleFeatured(item.id)}><CosmeticImage item={item}/><span>{item.name}</span><span className="pwa-featured-check">{checked ? <Check size={16}/> : "+"}</span></button>; })}</div>
    </section>}

    <section className="pwa-custom-group">
      <header><strong>Moldura</strong><small>A moldura acompanha o avatar quando estiver disponível para sua conta.</small></header>
      <div className="pwa-custom-grid">
        <ChoiceCard selected={!draft.selectedProfileFrameId} title="Sem moldura" onClick={() => setDraft((current) => ({ ...current, selectedProfileFrameId: "" }))}/>
        {data.frames.map((item) => <ChoiceCard key={item.id} selected={draft.selectedProfileFrameId === item.id} title={item.name} subtitle={item.free_for_all ? "Gratuito p/ Todos" : item.purchasable ? "Adquirida na Loja XP" : "Liberada por emblema/ADM"} image={<CosmeticImage item={item}/>} onClick={() => setDraft((current) => ({ ...current, selectedProfileFrameId: item.id }))}/>) }
      </div>
    </section>

    <section className="pwa-custom-group">
      <header><strong>Efeitos de luz</strong><small>O efeito fica ao redor do emblema sem cobrir o avatar.</small></header>
      <div className="pwa-custom-grid">
        <ChoiceCard selected={!draft.selectedProfileEffectId} title="Sem efeito" onClick={() => setDraft((current) => ({ ...current, selectedProfileEffectId: "" }))}/>
        {data.effects.map((item) => <ChoiceCard key={item.id} selected={draft.selectedProfileEffectId === item.id} title={item.name} subtitle={`${item.tone === "forte" ? "Luz forte" : item.tone === "suave" ? "Suave" : "Médio"} · ${item.free_for_all ? "Gratuito p/ Todos" : item.purchasable ? "Adquirido na Loja XP" : "Emblema/ADM"}`} image={<span className="pwa-effect-choice" style={effectStyle(item)}/>} onClick={() => setDraft((current) => ({ ...current, selectedProfileEffectId: item.id }))}/>) }
      </div>
    </section>

    {error && <p className="parity-warning">{error}</p>}
    <div className="pwa-custom-actions"><button className="profile-v2-refresh" disabled={loading || saving} onClick={() => void reload(true)}><RefreshCcw size={17}/> Atualizar catálogo</button><button className="parity-primary" disabled={!dirty || saving} onClick={() => void save()}><Save size={18}/>{saving ? "Salvando…" : "Salvar personalização"}</button></div>
  </div>;
}
