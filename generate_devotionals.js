const fs = require('fs');
const path = require('path');

const verses = [
  { ref: "Romanos 8:28", text: "E sabemos que todas as coisas contribuem juntamente para o bem daqueles que amam a Deus..." },
  { ref: "Filipenses 4:13", text: "Posso todas as coisas em Cristo que me fortalece." },
  { ref: "Salmos 23:1", text: "O Senhor é o meu pastor, nada me faltará." },
  { ref: "Isaías 41:10", text: "Não temas, porque eu sou contigo; não te assombres, porque eu sou teu Deus..." },
  { ref: "Provérbios 3:5-6", text: "Confia no Senhor de todo o teu coração, e não te estribes no teu próprio entendimento..." },
  { ref: "Mateus 11:28", text: "Vinde a mim, todos os que estais cansados e oprimidos, e eu vos aliviarei." },
  { ref: "Josué 1:9", text: "Não to mandei eu? Esforça-te, e tem bom ânimo; não temas, nem te espantes..." },
  { ref: "Salmos 46:1", text: "Deus é o nosso refúgio e fortaleza, socorro bem presente na angústia." },
  { ref: "João 14:6", text: "Disse-lhe Jesus: Eu sou o caminho, e a verdade e a vida; ninguém vem ao Pai, senão por mim." },
  { ref: "Romanos 12:2", text: "E não sede conformados com este mundo, mas sede transformados pela renovação do vosso entendimento..." }
];

const themes = [
  "A Força da Fé", "Descansando em Deus", "Confiança Inabalável", "O Amor do Pai", "Superando Limites",
  "A Paz que Excede", "Renovação Diária", "Caminho de Luz", "Refúgio Seguro", "Identidade em Cristo"
];

const contents = [
  "Deus tem um propósito em todas as estações da nossa vida. Mesmo quando não entendemos, podemos confiar que Ele está trabalhando nos bastidores.",
  "O Senhor nunca nos abandona. Suas promessas são fiéis e verdadeiras. Hoje é um dia para lembrar do Seu cuidado constante.",
  "Muitas vezes as dificuldades parecem montanhas, mas com Deus, podemos mover qualquer obstáculo pela fé.",
  "A verdadeira paz não é a ausência de tempestades, mas a presença de Jesus no barco. Descanse Nele hoje.",
  "A graça de Deus é suficiente para te sustentar. Não confie na sua própria força, mas entregue seus caminhos ao Senhor.",
  "Você é amado com um amor eterno. Não deixe que as mentiras do mundo ofusquem a sua verdadeira identidade em Cristo.",
  "A Palavra de Deus é viva e eficaz. Ela é luz para o nosso caminho e lâmpada para os nossos pés em tempos de escuridão.",
  "Busque a Deus em primeiro lugar e todas as outras coisas serão acrescentadas. Priorize a presença do Pai.",
  "O perdão liberta quem perdoa. Que o amor de Cristo inunde o seu coração para curar feridas do passado.",
  "A alegria do Senhor é a nossa força. Celebre as pequenas vitórias e seja grato pelo fôlego de vida."
];

const startYear = 2026;
const devotionals = [];
let idCounter = 1;

for (let month = 0; month < 12; month++) {
  const daysInMonth = new Date(startYear, month + 1, 0).getDate();
  for (let day = 1; day <= daysInMonth; day++) {
    const dateStr = `${startYear}-${String(month + 1).padStart(2, '0')}-${String(day).padStart(2, '0')}`;
    
    // Pick deterministic data based on day of year
    const index = (month * 31 + day) % 10;
    
    const verseObj = verses[index];
    const theme = themes[index];
    const content = contents[index];
    
    devotionals.push({
      id: String(idCounter++),
      title: `${theme} - Dia ${String(day).padStart(2, '0')}/${String(month + 1).padStart(2, '0')}`,
      date: dateStr,
      verse: verseObj.text,
      verseReference: verseObj.ref,
      content: content + "\n\nOração: Senhor, guie os meus passos neste dia e me ajude a viver segundo a Sua Palavra. Amém.",
      timestamp: new Date(startYear, month, day).getTime()
    });
  }
}

const targetPath = path.join(__dirname, 'app', 'src', 'main', 'assets', 'devotionals.json');
fs.writeFileSync(targetPath, JSON.stringify(devotionals, null, 2));
console.log('Successfully generated 365 devotionals.');
