const functions = require("firebase-functions");
const admin = require("firebase-admin");
admin.initializeApp();

exports.dailyDevotionalReminder = functions.pubsub.schedule('0 8 * * *')
  .timeZone('America/Sao_Paulo')
  .onRun(async (context) => {
    const db = admin.firestore();
    
    // Get latest devotional
    const snapshot = await db.collection("devocionais")
      .orderBy("date", "desc")
      .limit(1)
      .get();
      
    if (snapshot.empty) {
      console.log("No devotionals found.");
      return null;
    }
    
    const doc = snapshot.docs[0];
    const dev = doc.data();
    const title = dev.title || "Nova Palavra";
    const body = dev.verse || "Tempo para o seu devocional de hoje! Venha se fortalecer com a Palavra.";
    
    const message = {
      notification: {
        title: "Devocional Diário: " + title,
        body: body,
      },
      topic: "devocionais"
    };
    
    try {
      const response = await admin.messaging().send(message);
      console.log("Successfully sent daily devotional reminder:", response);
    } catch (error) {
      console.error("Error sending daily reminder:", error);
    }
    
    return null;
  });
