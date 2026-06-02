const { Client, LocalAuth } = require('whatsapp-web.js');
const express = require('express');
const qrcode = require('qrcode-terminal');
const QRCode = require('qrcode');

const app = express();
app.use(express.json());

const PORT = 3000;
let qrCodeData = null;
let isReady = false;

const client = new Client({
  authStrategy: new LocalAuth({ dataPath: '/sessions' }),
  puppeteer: {
    headless: true,
    executablePath: process.env.PUPPETEER_EXECUTABLE_PATH || '/usr/bin/chromium',
    args: [
      '--no-sandbox',
      '--disable-setuid-sandbox',
      '--disable-dev-shm-usage',
      '--disable-gpu',
      '--disable-software-rasterizer',
    ],
  },
});

client.on('qr', (qr) => {
  qrCodeData = qr;
  console.log('\n========== QR CODE WHATSAPP ==========');
  qrcode.generate(qr, { small: true });
  console.log('======================================');
  console.log('Ou ouvrez http://localhost:3000/qr dans votre navigateur\n');
});

client.on('ready', () => {
  isReady = true;
  qrCodeData = null;
  console.log('✅ WhatsApp connecté et prêt à envoyer des messages');
});

client.on('auth_failure', (msg) => {
  console.error('❌ Échec authentification WhatsApp:', msg);
});

client.on('disconnected', (reason) => {
  isReady = false;
  console.log('⚠️  WhatsApp déconnecté:', reason);
});

// ─── QR code page (navigateur) ────────────────────────────────────
app.get('/qr', async (req, res) => {
  if (isReady) {
    return res.send(`<!DOCTYPE html><html><body style="font-family:sans-serif;text-align:center;padding:60px">
      <h2>✅ WhatsApp Business connecté</h2><p>L'API est prête à envoyer des messages.</p>
    </body></html>`);
  }
  if (!qrCodeData) {
    return res.send(`<!DOCTYPE html><html>
      <head><meta http-equiv="refresh" content="3"></head>
      <body style="font-family:sans-serif;text-align:center;padding:60px">
        <h2>⏳ QR code en cours de génération…</h2>
        <p>La page se rafraîchit automatiquement.</p>
      </body></html>`);
  }
  const qrImage = await QRCode.toDataURL(qrCodeData);
  res.send(`<!DOCTYPE html>
<html>
<head>
  <title>Dinthialma – Connexion WhatsApp</title>
  <meta http-equiv="refresh" content="30">
  <style>
    body { font-family: sans-serif; display: flex; flex-direction: column;
           align-items: center; padding: 40px; background: #f5f5f5; }
    img  { width: 280px; height: 280px; border: 4px solid #25D366;
           border-radius: 12px; background: white; padding: 8px; }
    h2   { color: #128C7E; }
    p    { color: #666; }
  </style>
</head>
<body>
  <h2>📱 Scannez avec WhatsApp Business</h2>
  <img src="${qrImage}" alt="QR Code"/>
  <p>Ouvrez WhatsApp → Appareils liés → Lier un appareil</p>
  <p style="font-size:12px">La page se rafraîchit toutes les 30 secondes</p>
</body>
</html>`);
});

// ─── Health check ─────────────────────────────────────────────────
app.get('/health', (req, res) => {
  res.json({ status: isReady ? 'ready' : 'waiting_for_qr' });
});

// ─── Envoi de message texte ───────────────────────────────────────
app.post('/message/sendText/:chatId', async (req, res) => {
  if (!isReady) {
    return res.status(503).json({ error: 'WhatsApp non connecté — scannez le QR sur /qr' });
  }
  const { chatId } = req.params;
  const { text } = req.body;
  if (!text) {
    return res.status(400).json({ error: 'Champ "text" requis' });
  }
  try {
    await client.sendMessage(chatId, text);
    console.log(`Message envoyé à ${chatId}`);
    res.json({ success: true });
  } catch (err) {
    console.error('Erreur envoi message:', err.message);
    res.status(500).json({ error: err.message });
  }
});

app.listen(PORT, '0.0.0.0', () => {
  console.log(`🚀 API WhatsApp démarrée sur http://0.0.0.0:${PORT}`);
  console.log(`   QR Code : http://localhost:${PORT}/qr`);
  console.log(`   Santé   : http://localhost:${PORT}/health`);
});

client.initialize();
