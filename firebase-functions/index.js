const functions = require("firebase-functions");
const admin = require("firebase-admin");

admin.initializeApp();

async function sendTopicNotification(topic, title, body, data = {}) {
  const message = {
    topic,
      notification: { title, body },
    data: Object.fromEntries(
      Object.entries({ ...data, category: data.category || "content_updates" })
        .map(([key, value]) => [key, String(value)])
    ),
  };

  return admin.messaging().send(message);
}

function collectionUpdate(collection, topic, title, body) {
  return functions.firestore
    .document(`${collection}/{documentId}`)
    .onCreate(async (snapshot) => {
      const data = snapshot.data() || {};
      const itemTitle = data.title || data.name || title;
      const resolvedTitle = title.replace("{title}", itemTitle);
      const itemBody = body.replace("{title}", itemTitle);
      await sendTopicNotification(topic, resolvedTitle, itemBody, {
        documentId: snapshot.id,
        collection,
      });
      return null;
    });
}

exports.dailyDevotionalReminder = functions.pubsub.schedule("0 8 * * *")
  .timeZone("America/Sao_Paulo")
  .onRun(async () => {
    const snapshot = await admin.firestore()
      .collection("devocionais")
      .orderBy("date", "desc")
      .limit(1)
      .get();

    if (snapshot.empty) return null;

    const doc = snapshot.docs[0];
    const dev = doc.data();
    await sendTopicNotification(
      "all_users",
      `Devocional Diário: ${dev.title || "Nova Palavra"}`,
      dev.verse || "Tempo para se fortalecer com a Palavra.",
      { collection: "devocionais", documentId: doc.id, category: "daily_devotional" }
    );
    return null;
  });

exports.notifyNewDevotional = collectionUpdate(
  "devocionais",
  "all_users",
  "Novo devocional disponível",
  "Confira agora: {title}"
);

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

exports.notifyNewEvent = collectionUpdate(
  "cultos_agenda",
  "all_users",
  "Hoje tem {title}",
  "Confira a programação e os horários."
);

exports.notifyNewMemberContent = collectionUpdate(
  "membros_conteudos",
  "members_users",
  "Novo conteúdo para membros",
  "Confira o novo conteúdo: {title}"
);
