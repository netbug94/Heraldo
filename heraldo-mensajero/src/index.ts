import express from 'express';
import dotenv from 'dotenv';
import { WhatsAppManager } from './WhatsAppManager.js';
import { getDashboardHtml } from './dashboard.js';
import { logger } from './logger.js';

// Load environment variables from system or .env file
dotenv.config();

const PORT = process.env.PORT || 3000;
const API_KEY = process.env.API_KEY;

// Fail fast if the system is misconfigured
if (!API_KEY) {
    logger.error("❌ FATAL: API_KEY is not defined in environment variables!");
    process.exit(1);
}

const app = express();
app.use(express.json());

const wa = new WhatsAppManager();

// ==========================================
// SECURITY MIDDLEWARE
// ==========================================
app.use('/api', (req, res, next) => {
    // Only secure mutations
    if (req.method === 'POST' || req.method === 'DELETE') {
        const clientKey = req.headers['x-api-key'];
        if (!clientKey || clientKey !== API_KEY) {
            logger.warn(`🛑 Blocked unauthorized ${req.method} request from ${req.ip}`);
            return res.status(401).json({ error: 'Unauthorized: Invalid API Key' });
        }
    }
    next();
});

// ==========================================
// ROUTES
// ==========================================

// UI Dashboard
app.get('/', (_req, res) => res.send(getDashboardHtml()));

// Internal Status API
app.get('/api/status', (_req, res) => {
    res.json({ session: wa.sessionState });
});

// Session Management
app.post('/api/session/start', async (_req, res) => {
    await wa.initAccount();
    res.sendStatus(202);
});

app.delete('/api/session', async (_req, res) => {
    await wa.deleteAccount();
    res.sendStatus(200);
});

// Manual Send it (From Dashboard)
app.post('/api/sendText', async (req, res) => {
    const success = await wa.sendMessage(req.body.chatId, req.body.text);
    res.status(success ? 200 : 500).json({ success });
});

// ==========================================
// HERALDO GESTOR COMPATIBILITY ROUTES
// ==========================================


// Health check (Tells Brain if we are ready)
app.get('/health', (_req, res) => {
    const statusMap = { ...wa.sessionState, status: 'UP' };
    res.json(statusMap);
});

// Main delivery endpoint
app.post('/send', async (req, res) => {
    const clientKey = req.headers['x-api-key'];
    if (!clientKey || clientKey !== API_KEY) {
        logger.warn(`🛑 Blocked unauthorized Brain /send request from ${req.ip}`);
        return res.status(401).json({ error: 'Unauthorized: Invalid API Key' });
    }

    // Heraldo Gestor sends { phone, message }
    const success = await wa.sendMessage(req.body.phone, req.body.message);
    res.status(success ? 200 : 500).json({ success });
});

app.listen(PORT, () => logger.info(`🚀 Heraldo Mensajero running on port ${PORT}`));