const functions = require("firebase-functions/v1");
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

const DEVOTIONAL_DELIVERY_COLLECTION = "_notification_deliveries";
const DEVOTIONAL_DELIVERY_LEASE_MS = 2 * 60 * 1000;
const DEVOTIONAL_MAX_EVENT_AGE_MS = 12 * 60 * 60 * 1000;

async function claimDevotionalDelivery(eventId, documentId) {
  const deliveryRef = admin.firestore()
    .collection(DEVOTIONAL_DELIVERY_COLLECTION)
    .doc(eventId);
  const now = Date.now();

  return admin.firestore().runTransaction(async (transaction) => {
    const current = await transaction.get(deliveryRef);
    if (current.exists) {
      const state = current.get("state");
      const leaseUntil = Number(current.get("leaseUntil") || 0);
      if (state === "sent") return "sent";
      if (state === "sending" && leaseUntil > now) return "busy";
    }

    transaction.set(deliveryRef, {
      kind: "devotional",
      documentId,
      state: "sending",
      leaseUntil: now + DEVOTIONAL_DELIVERY_LEASE_MS,
      attempts: admin.firestore.FieldValue.increment(1),
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    }, { merge: true });
    return "claimed";
  });
}

async function markDevotionalDelivery(eventId, patch) {
  await admin.firestore()
    .collection(DEVOTIONAL_DELIVERY_COLLECTION)
    .doc(eventId)
    .set({
      ...patch,
      leaseUntil: 0,
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    }, { merge: true });
}

exports.notifyNewDevotional = functions
  .runWith({ failurePolicy: true, timeoutSeconds: 60, memory: "256MB" })
  .firestore.document("devocionais/{documentId}")
  .onCreate(async (snapshot, context) => {
    const data = snapshot.data() || {};
    if (data.isApproved === false || data.approved === false) {
      console.log("Devocional ainda não aprovado; notificação ignorada.", snapshot.id);
      return null;
    }

    const eventAge = Date.now() - Date.parse(context.timestamp);
    if (Number.isFinite(eventAge) && eventAge > DEVOTIONAL_MAX_EVENT_AGE_MS) {
      console.warn("Evento de devocional expirado; evitando aviso atrasado.", snapshot.id);
      return null;
    }

    const deliveryState = await claimDevotionalDelivery(context.eventId, snapshot.id);
    if (deliveryState === "sent") return null;
    if (deliveryState === "busy") {
      throw new Error("Envio do devocional já está em andamento; tentar novamente.");
    }

    const itemTitle = String(data.title || "").trim().slice(0, 120);
    const title = "Novo devocional disponível";
    const body = itemTitle ? `Confira agora: ${itemTitle}` : "Uma nova palavra está disponível no MIC Rhema.";

    try {
      await admin.firestore().collection("settings").doc("sync_trigger").set({
        timestamp: Date.now(),
        source: "devotional_backend",
        devotionalId: snapshot.id,
      }, { merge: true });

      const messageId = await admin.messaging().send({
        topic: "all_users",
        notification: { title, body },
        data: {
          title,
          body,
          documentId: snapshot.id,
          collection: "devocionais",
          category: "daily_devotional",
          destination: "devocionais",
        },
        android: {
          priority: "high",
          notification: {
            tag: `devotional-${snapshot.id}`,
          },
        },
      });

      await Promise.all([
        markDevotionalDelivery(context.eventId, {
          state: "sent",
          sentAt: admin.firestore.FieldValue.serverTimestamp(),
          messageId,
        }),
        snapshot.ref.set({
          notificationSentAt: admin.firestore.FieldValue.serverTimestamp(),
          notificationSource: "firebase_function",
        }, { merge: true }),
      ]);

      console.log("Devocional distribuído pelo servidor.", snapshot.id, messageId);
      return null;
    } catch (error) {
      await markDevotionalDelivery(context.eventId, {
        state: "failed",
        lastError: String(error?.message || error).slice(0, 500),
      }).catch(() => undefined);
      throw error;
    }
  });

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
