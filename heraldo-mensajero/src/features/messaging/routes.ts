import { Router } from 'express';
import { wa } from '../../core/whatsapp.js';
import { logger } from '../../core/logger.js';

export const messagingRouter = Router();
const API_KEY = process.env.HERALDO_INTERNAL_TOKEN;

// ==========================================
// SECURITY MIDDLEWARE
// ==========================================
messagingRouter.use((req, res, next) => {
    // Protect POST and DELETE requests
    if (req.method === 'POST' || req.method === 'DELETE') {
        const clientKey = req.headers['x-api-key'];
        if (!clientKey || clientKey !== API_KEY) {
            logger.warn(`🛑 Blocked unauthorized ${req.method} request to ${req.path} from ${req.ip}`);
            return res.status(401).json({ error: 'Unauthorized: Invalid API Key' });
        }
    }
    next();
});

// ==========================================
// GESTOR ROUTES (Mounted at root in index.ts)
// ==========================================
messagingRouter.get('/health', (_req, res) => {
    res.json({ ...wa.sessionState, service: 'UP' });
});

messagingRouter.post('/send', async (req, res) => {
    // Heraldo Gestor sends { phone, message }
    const success = await wa.sendMessage(req.body.phone, req.body.message);
    res.status(success ? 200 : 500).json({ success });
});

// ==========================================
// DASHBOARD API ROUTES (Mounted at /api in index.ts)
// ==========================================
messagingRouter.get('/status', (_req, res) => {
    res.json({ session: wa.sessionState });
});

messagingRouter.post('/session/start', async (_req, res) => {
    await wa.initAccount();
    res.sendStatus(202);
});

messagingRouter.delete('/session', async (_req, res) => {
    await wa.deleteAccount();
    res.sendStatus(200);
});

messagingRouter.post('/sendText', async (req, res) => {
    const success = await wa.sendMessage(req.body.chatId, req.body.text);
    res.status(success ? 200 : 500).json({ success });
});