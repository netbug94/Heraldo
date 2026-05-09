// src/core/whatsapp.ts
import { WhatsAppManager } from './WhatsAppManager.js';
import { logger } from './logger.js';
export const wa = new WhatsAppManager();

// Auto-initialize on startup if possible
wa.initAccount().catch(err => logger.error(`🚨 Failed to auto-init WhatsApp: ${err}`));