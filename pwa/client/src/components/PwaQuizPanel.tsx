import { useEffect, useState } from "react";
import { BookOpenCheck, Lightbulb, RefreshCcw } from "lucide-react";
import { toast } from "sonner";
import {
  loadPwaQuiz,
  requestPwaQuizHint,
  submitPwaQuizAnswer,
  syncPwaQuizProfile,
  type PwaQuizAnswer,
  type PwaQuizDifficulty,
  type PwaQuizHint,
  type PwaQuizStatus,
} from "@/lib/xp";

const labels: Record<PwaQuizDifficulty, string> = { easy: "Fácil", medium: "Médio", hard: "Difícil" };
const rewards: Record<PwaQuizDifficulty, number> = { easy: 10, medium: 20, hard: 30 };

export function PwaQuizPanel({ onXpChange }: { onXpChange?: () => void | Promise<void> }) {
  const [difficulty, setDifficulty] = useState<PwaQuizDifficulty>("easy");
  const [status, setStatus] = useState<PwaQuizStatus | null>(null);
  const [hint, setHint] = useState<PwaQuizHint | null>(null);
  const [answer, setAnswer] = useState<PwaQuizAnswer | null>(null);
  const [selected, setSelected] = useState(-1);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");

  const load = async (target = difficulty) => {
    setBusy(true);
    setError("");
    try {
      const value = await loadPwaQuiz(target);
      setStatus(value);
      setHint(null);
      setAnswer(null);
      setSelected(-1);
    } catch (failure) {
      setError(failure instanceof Error ? failure.message : "Não foi possível carregar o Quiz.");
    } finally {
      setBusy(false);
    }
  };

  useEffect(() => { void load(difficulty); }, [difficulty]);

  const askHint = async (kind: "subtle" | "easy") => {
    const question = status?.question;
    if (!question || busy || answer) return;
    setBusy(true);
    setError("");
    try {
      const value = await requestPwaQuizHint(question.id, kind);
      setHint(value);
    } catch (failure) {
      setError(failure instanceof Error ? failure.message : "Não foi possível registrar a dica.");
    } finally {
      setBusy(false);
    }
  };

  const submit = async (index: number) => {
    const question = status?.question;
    if (!question || busy || answer) return;
    setSelected(index);
    setBusy(true);
    setError("");
    try {
      const result = await submitPwaQuizAnswer(question.id, index);
      setAnswer(result);
      setSelected(result.selectedOptionIndex);
      await syncPwaQuizProfile().catch(() => undefined);
      window.dispatchEvent(new Event("micrhema:pwa:quiz-synced"));
      await onXpChange?.();
      if (result.correct && result.granted > 0) toast.success(`Resposta correta · +${result.granted} XP`);
      else if (result.correct) toast.success("Resposta correta · tentativa já registrada");
      else toast.info("Resposta registrada · 0 XP");
    } catch (failure) {
      setSelected(-1);
      setError(failure instanceof Error ? failure.message : "Não foi possível validar a resposta.");
    } finally {
      setBusy(false);
    }
  };

  const question = status?.question;
  const effectiveHintLabel = hint?.variant === "easy_hint" ? "Dica direta · 70% do XP" : hint ? "Dica sutil · 90% do XP" : "";

  return <div style={{marginTop:20}}>
    <div className="parity-title" style={{marginBottom:10}}>
      <div><p>CONHECIMENTO BÍBLICO</p><h3>Quiz Bíblico</h3><span>Primeira tentativa validada no mesmo servidor do Android.</span></div>
      <BookOpenCheck size={24}/>
    </div>

    <div className="filter-pills" style={{marginBottom:10}}>
      {(["easy","medium","hard"] as PwaQuizDifficulty[]).map(item =>
        <button key={item} className={difficulty === item ? "active" : ""} disabled={busy} onClick={() => setDifficulty(item)}>
          {labels[item]} · {rewards[item]} XP
        </button>
      )}
    </div>

    {busy && !status && <p className="parity-status">Carregando Quiz…</p>}
    {status && <p className="parity-status">{status.answered}/{status.total} perguntas respondidas em {labels[difficulty].toLowerCase()}.</p>}

    {status && !question && <article style={{padding:14,border:"1px solid rgba(127,127,127,.25)",borderRadius:16}}>
      <strong>Todas as perguntas desta dificuldade foram concluídas.</strong>
      <small style={{display:"block",opacity:.75,marginTop:4}}>A primeira tentativa fica registrada no ledger central e não gera XP novamente.</small>
    </article>}

    {question && <article style={{padding:14,border:"1px solid rgba(127,127,127,.25)",borderRadius:16}}>
      <strong>{labels[question.difficulty]} · até {question.baseXp} XP</strong>
      <p style={{margin:"10px 0",fontWeight:600}}>{question.prompt}</p>

      {!answer && <div style={{display:"grid",gridTemplateColumns:"1fr 1fr",gap:8,marginBottom:10}}>
        <button className="back-link" disabled={busy} onClick={() => void askHint("subtle")}><Lightbulb size={16}/> Dica sutil</button>
        <button className="back-link" disabled={busy} onClick={() => void askHint("easy")}><Lightbulb size={16}/> Dica direta</button>
      </div>}

      {hint && !answer && <div className="parity-warning" style={{marginBottom:10}}>
        <strong>{effectiveHintLabel}</strong><div style={{marginTop:4}}>{hint.hint}</div>
      </div>}

      <div className="android-list-cards">
        {question.options.map((option,index) => {
          const isCorrect = answer && index === answer.correctOptionIndex;
          const isWrongSelected = answer && index === answer.selectedOptionIndex && !answer.correct;
          const background = isCorrect ? "rgba(46,160,67,.15)" : isWrongSelected ? "rgba(220,38,38,.12)" : selected === index ? "rgba(127,127,127,.13)" : undefined;
          return <button key={`${question.id}:${index}`} className="back-link" style={{width:"100%",justifyContent:"flex-start",textAlign:"left",background}} disabled={busy || !!answer} onClick={() => void submit(index)}>
            <strong>{String.fromCharCode(65 + index)}.</strong> {option}
          </button>;
        })}
      </div>

      {busy && <p className="parity-status">{hint ? "Validando no servidor…" : "Sincronizando…"}</p>}
      {answer && <div className={answer.correct ? "parity-status" : "parity-warning"} style={{marginTop:10}}>
        <strong>{answer.correct ? "Resposta correta" : "Resposta incorreta"}</strong>
        <div>{answer.explanation}</div>
        <small>Referência: {answer.reference}</small>
        <div style={{marginTop:6,fontWeight:700}}>{answer.granted > 0 ? `+${answer.granted} XP` : "0 XP"}</div>
      </div>}
      {error && <p className="parity-warning">{error}</p>}
      {answer && <button className="parity-primary" style={{width:"100%",marginTop:10}} onClick={() => void load()} disabled={busy}><RefreshCcw size={16}/> Próxima pergunta</button>}
    </article>}

    {error && !question && <p className="parity-warning">{error}</p>}
  </div>;
}
