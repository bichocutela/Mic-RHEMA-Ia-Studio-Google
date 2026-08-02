const admin = require("firebase-admin");
const fs = require("fs");

// INSTRUÇÕES DE USO:
// 1. Gere uma chave privada no Firebase Console:
//    Configurações do Projeto > Contas de Serviço > Gerar nova chave privada.
// 2. Salve o arquivo baixado neste diretório como "serviceAccountKey.json".
// 3. Instale o Firebase Admin SDK rodando: npm install firebase-admin
// 4. Execute este script: node firestore_upload_bibles.js

const serviceAccount = require("./serviceAccountKey.json");

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount)
});

const db = admin.firestore();

async function uploadVersion(version, filePath) {
    if (!fs.existsSync(filePath)) {
        console.log(`Arquivo não encontrado para a versão ${version}: ${filePath}`);
        return;
    }
    
    console.log(`Iniciando upload da versão: ${version}`);
    const rawData = fs.readFileSync(filePath, 'utf-8');
    
    // Remove o BOM (Byte Order Mark) se existir no início do arquivo
    let cleanData = rawData;
    if (cleanData.charCodeAt(0) === 0xFEFF) {
        cleanData = cleanData.slice(1);
    }
    
    const books = JSON.parse(cleanData);
    
    for (const book of books) {
        const bookName = book.name;
        // Estrutura da coleção: bibles/{version}/books/{bookName}
        const docRef = db.collection('bibles').doc(version).collection('books').doc(bookName);
        
        // O Firestore não suporta arrays aninhados (arrays dentro de arrays).
        // Por isso, convertemos o array de capítulos em um Mapa (Objeto), 
        // onde a chave é o número do capítulo ("1", "2", etc).
        const chaptersMap = {};
        book.chapters.forEach((versesArray, index) => {
            chaptersMap[(index + 1).toString()] = versesArray;
        });

        await docRef.set({
            name: book.name,
            abbrev: book.abbrev,
            chapters: chaptersMap
        });
        
        console.log(`  - Livro salvo: ${bookName} (${version})`);
    }
    
    console.log(`Upload concluído para a versão ${version}!\n`);
}

async function run() {
    try {
        // O pt_aa.json contém a versão que renomeamos para ARA no app
        await uploadVersion('ARA', './app/src/main/assets/bibles/pt_aa.json'); 
        await uploadVersion('NVI', './app/src/main/assets/bibles/pt_nvi.json');
        await uploadVersion('ACF', './app/src/main/assets/bibles/pt_acf.json');
        console.log("Todos os uploads foram finalizados com sucesso.");
    } catch (error) {
        console.error("Erro durante o upload:", error);
    }
}

run();
