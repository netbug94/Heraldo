// src/index.ts
import express from 'express';
import dotenv from 'dotenv';
import cookieParser from 'cookie-parser';

import { logger } from './core/logger.js';
import { wa } from './core/whatsapp.js'; // Imports the initialized instance

// Feature Routes
import { authRouter } from './features/auth/routes.js';
import { dashboardRouter } from './features/dashboard/routes.js';
import { messagingRouter } from './features/messaging/routes.js';

dotenv.config();

const PORT = process.env.PORT || 3000;
const API_KEY = process.env.HERALDO_INTERNAL_TOKEN;

if (!API_KEY) {
    logger.error("❌ FATAL: HERALDO_INTERNAL_TOKEN is not defined!");
    process.exit(1);
}

const app = express();
app.use(express.json());
app.use(express.urlencoded({ extended: true }));
app.use(cookieParser());

// Mount Vertical Slices
app.use('/', authRouter);            // Public: /login, /logout
app.use('/', dashboardRouter);       // Protected: /
app.use('/api', messagingRouter);    // Dashboard API: /api/status, /api/sendText, etc.
app.use('/', messagingRouter);       // Gestor API: /send, /health

const server = app.listen(PORT, () => logger.info(`🚀 Heraldo Mensajero running on port ${PORT}`));

// Graceful Shutdown
const shutdown = async (signal: string) => {
    logger.info(`\n🛑 Received ${signal}. Shutting down Mensajero gracefully...`);
    server.close(() => logger.info('✅ HTTP server closed.'));
    await wa.stop();
    logger.info('✅ Client stopped. Exiting.');
    process.exit(0);
};

process.on('SIGTERM', () => shutdown('SIGTERM'));
process.on('SIGINT', () => shutdown('SIGINT'));