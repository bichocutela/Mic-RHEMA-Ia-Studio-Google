const functions = require("firebase-functions");
const admin = require("firebase-admin");

admin.initializeApp();

async function sendTopicNotification(topic, title, body, data = {}) {
  // All publication triggers use the same durable queue as Android and PWA.
  // FIREBASE_SERVICE_ACCOUNT_JSON already identifies this project on Supabase.
  const response = await fetch("https://cwphbkdtorfpgmnlafqb.supabase.co/functions/v1/notify-fcm", {
    method: "POST", headers: { "content-type": "application/json" },
    body: JSON.stringify({ topic, title, body, data }),
  });
  if (!response.ok) throw new Error(`Notification queue rejected request: ${response.status}`);
  return response.json();
}

function collectionUpdate(collection, topic, title, body) {
  return functions.firestore
    .document(`${collection}/{documentId}`)
    .onCreate(async (snapshot) => {
      const data = snapshot.data() || {};
      if (data.isApproved === false || data.isPublished === false || Number(data.scheduledPublishAt || 0) > Date.now()) return null;
      const category = collection === "ibr_courses" ? "courses" : collection.startsWith("conteudos_") ? (collection === "conteudos_videos" ? "sermons" : "media") : "content_updates";
      const itemTitle = data.title || data.name || title;
      const resolvedTitle = title.replace("{title}", itemTitle);
      const itemBody = body.replace("{title}", itemTitle);
      await sendTopicNotification(topic, resolvedTitle, itemBody, {
        documentId: snapshot.id,
        collection, category,
      });
      return null;
    });
}

// Kept as no-op exports for a safe deployment over legacy functions.
// The Android fallback owns its daily 08h reminder; the Supabase cron owns PWA.
exports.dailyDevotionalReminder = functions.pubsub.schedule("0 8 * * *")
  .timeZone("America/Fortaleza").onRun(async () => null);
exports.notifyNewDevotional = functions.firestore.document("devocionais/{documentId}").onCreate(async () => null);

exports.notifyNewBook = collectionUpdate(
  "conteudos_books",
  "all_users",
  "Foi adicionado o livro",
  "{title}"
);

exports.notifyNewVideo = collectionUpdate(
  "conteudos_videos",
  "all_users",
  "Foi adicionado o vídeo",
  "{title}"
);

exports.notifyNewAudio = collectionUpdate(
  "conteudos_audios",
  "all_users",
  "Foi adicionado um áudio em mídia",
  "{title}"
);

exports.notifyNewDiscipulado = collectionUpdate(
  "discipulado_pdfs",
  "all_users",
  "Novo estudo de Discipulado",
  "Confira o novo PDF: {title}"
);

exports.notifyNewIbrContent = collectionUpdate(
  "ibr_courses",
  "ibr_users",
  "Novo conteúdo IBR disponível",
  "Confira o novo conteúdo: {title}"
);

exports.notifyNewEvent = functions.firestore.document("cultos_agenda/{documentId}").onCreate(async () => null);

exports.notifyNewMemberContent = collectionUpdate(
  "membros_conteudos",
  "members_users",
  "Novo conteúdo para membros",
  "Confira o novo conteúdo: {title}"
);
