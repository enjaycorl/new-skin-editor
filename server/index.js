require('dotenv').config();

const express  = require('express');
const cors     = require('cors');
const helmet   = require('helmet');
const path     = require('path');
const fs       = require('fs');

const { connectDB }    = require('./config/database');
const requestLogger    = require('./middleware/requestLogger');
const errorHandler     = require('./middleware/errorHandler');
const skinsRouter      = require('./routes/skins');

// ── Ensure data directory exists (SQLite file lives here) ───────────────────
const dataDir = path.join(__dirname, '../data');
if (!fs.existsSync(dataDir)) fs.mkdirSync(dataDir, { recursive: true });

// ── App setup ───────────────────────────────────────────────────────────────
const app  = express();
const PORT = process.env.PORT || 5000;

// Security headers
app.use(helmet());

// CORS — allow all origins in dev; lock down in production via env
app.use(cors({
  origin: process.env.ALLOWED_ORIGINS
    ? process.env.ALLOWED_ORIGINS.split(',')
    : '*',
  methods: ['GET', 'POST', 'PUT', 'PATCH', 'DELETE', 'OPTIONS'],
  allowedHeaders: ['Content-Type', 'Authorization'],
}));

// Parse JSON bodies (textureData can be large — raise limit to 10 MB)
app.use(express.json({ limit: '10mb' }));
app.use(express.urlencoded({ extended: true, limit: '10mb' }));

// HTTP request logging
app.use(requestLogger);

// ── Routes ───────────────────────────────────────────────────────────────────
app.get('/', (req, res) => {
  res.json({
    name:    'SkinCraft Studio API',
    version: '1.0.0',
    status:  'running',
    docs:    'https://github.com/your-repo#api-docs',
    endpoints: {
      skins:   '/api/skins',
      folders: '/api/skins/folders',
      health:  '/health',
    },
  });
});

app.get('/health', async (req, res) => {
  try {
    const { sequelize } = require('./config/database');
    await sequelize.authenticate();
    res.json({ status: 'ok', db: 'connected', ts: Date.now() });
  } catch {
    res.status(503).json({ status: 'error', db: 'unreachable' });
  }
});

app.use('/api/skins', skinsRouter);

// 404 fallback
app.use((req, res) => {
  res.status(404).json({ error: `Route ${req.method} ${req.path} not found.` });
});

// Central error handler (must be last)
app.use(errorHandler);

// ── Start ────────────────────────────────────────────────────────────────────
(async () => {
  try {
    await connectDB();
    app.listen(PORT, '0.0.0.0', () => {
      console.log(`\n🎮  SkinCraft Studio API`);
      console.log(`    http://0.0.0.0:${PORT}`);
      console.log(`    Environment : ${process.env.NODE_ENV || 'development'}`);
      console.log(`    Database    : ${process.env.DB_PATH || 'data/skincraft.sqlite'}`);
      console.log(`\n  Routes:`);
      console.log(`    GET  /health`);
      console.log(`    GET  /api/skins`);
      console.log(`    GET  /api/skins/folders`);
      console.log(`    GET  /api/skins/:id`);
      console.log(`    POST /api/skins`);
      console.log(`    PUT  /api/skins/:id`);
      console.log(`    PATCH /api/skins/:id/favorite`);
      console.log(`    DELETE /api/skins/:id\n`);
    });
  } catch (err) {
    console.error('[FATAL] Could not start server:', err);
    process.exit(1);
  }
})();
