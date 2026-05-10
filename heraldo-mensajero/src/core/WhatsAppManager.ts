// src/core/WhatsAppManager.ts
import pkg from 'whatsapp-web.js';
const { Client, LocalAuth } = pkg;
type WAClient = InstanceType<typeof Client>;
import qrcode from 'qrcode-terminal';
import fs from 'fs';
import path from 'path';
import { logger } from './logger.js';

// Configuration
const SESSIONS_DIR = process.env.SESSIONS_DIR || path.join(process.cwd(), 'sessions');
const GESTOR_WEBHOOK_URL = process.env.GESTOR_WEBHOOK_URL || 'http://gestor:8080/webhook/mensajero';

export class WhatsAppManager {
    private client: WAClient | null = null;
    public sessionState: any = { status: 'OFFLINE' };

    private lastPingTime: number = 0;
    private readonly PING_COOLDOWN_MS = 30000; // 30-second rate limit

    constructor() {
        if (!fs.existsSync(SESSIONS_DIR)) fs.mkdirSync(SESSIONS_DIR, { recursive: true });
    }

    private pingGestor(force: boolean = false) {
        const now = Date.now();

        // If not forced, check if we are still in the cooldown period
        if (!force && (now - this.lastPingTime < this.PING_COOLDOWN_MS)) {
            return;
        }

        this.lastPingTime = now;
        const API_KEY = process.env.HERALDO_INTERNAL_TOKEN || '';

        fetch(GESTOR_WEBHOOK_URL, {
            method: 'POST',
            headers: { 'x-api-key': API_KEY }
        }).catch((err) => logger.warn(`📡 Failed to ping Gestor: ${err.message}`));
    }
    async initAccount() {
        // Clear deadlocks to prevent startup hangs in Docker containers
        const lockPath = path.join(SESSIONS_DIR, `session-primary`, 'SingletonLock');
        if (fs.existsSync(lockPath)) {
            logger.warn(`🧹 Clearing dead SingletonLock for primary session`);
            fs.unlinkSync(lockPath);
        }

        if (this.client) return;
        this.sessionState = { status: 'STARTING' };

        const client = new Client({
            authStrategy: new LocalAuth({ clientId: 'primary', dataPath: SESSIONS_DIR }),
            puppeteer: {
                executablePath: process.env.PUPPETEER_EXECUTABLE_PATH ||
                    (process.platform === 'linux' ? '/usr/bin/chromium' : undefined),
                headless: true,
                args: [
                    '--no-sandbox',
                    '--disable-setuid-sandbox',
                    '--disable-dev-shm-usage',
                    '--disable-accelerated-2d-canvas',
                    '--disable-gpu',
                    '--no-first-run',
                    '--no-zygote' // Extra stability flags for headless Chromium in Docker
                ]
            }
        });

        client.on('qr', (qr) => {
            this.sessionState = { status: 'AWAITING_QR', qrCode: qr };
            qrcode.generate(qr, { small: true });
            this.pingGestor(); // Standard ping (will be throttled to once per 30s)
        });

        client.on('ready', () => {
            this.sessionState = { status: 'CONNECTED' };
            this.client = client;
            logger.info(`✅ Primary Session Connected (The Flame is Kindled)`);
            this.pingGestor(true); // FORCE ping to immediately show "Connected" on dashboard
        });

        client.on('disconnected', (reason) => {
            logger.error(`❌ Disconnected. Reason: ${reason}`);
            this.sessionState = { status: 'OFFLINE', reason };

            // Clean up the dead client instance
            if (this.client) {
                this.client.destroy().catch((e: any) => logger.error(`Cleanup failed: ${e}`));
                this.client = null;
            }

            this.pingGestor(true); // FORCE ping to immediately show "Offline"

            // Auto-heal: Increased to 30 seconds to prevent rapid-fire crash loops
            setTimeout(() => {
                logger.info("🔄 Auto-healing: Attempting to reconnect WhatsApp...");
                this.initAccount().catch(err => logger.error(`🚨 Reconnect failed: ${err}`));
            }, 30000);
        });

        client.on('auth_failure', async (msg) => {
            logger.error(`⚠️ Auth Failure: ${msg}. The sigil was rejected.`);
            this.sessionState = { status: 'AUTH_FAILED' };

            // Delete corrupted session data so it doesn't get stuck in a boot loop
            await this.deleteAccount();
            this.pingGestor(true);
        });

        try {
            await client.initialize();
        } catch (err) {
            logger.error(`🚨 Failed to initialize Puppeteer/WhatsApp client: ${err}`);
            this.sessionState = { status: 'OFFLINE', reason: 'Initialization crash' };
        }
    }

    async sendMessage(to: string, text: string): Promise<boolean> {
        if (this.sessionState.status === 'CONNECTED' && this.client) {
            const cleanNumber = to.replace(/\D/g, '');
            if (!cleanNumber) {
                logger.error(`⚠️ Cannot send message: recipient number is empty or invalid.`);
                return false;
            }

            logger.info(`Attempting to send message to: ${cleanNumber}`);
            try {
                const numberId = await this.client.getNumberId(cleanNumber);
                if (!numberId) {
                    logger.error(`⚠️ Could not resolve WhatsApp ID for number ${cleanNumber}`);
                    return false;
                }

                await this.client.sendMessage(numberId._serialized, text);
                logger.info(`✅ Message successfully delivered to ${cleanNumber}`);
                return true;
            } catch (e: any) {
                logger.error(`⚠️ Send failed. Error: ${e?.message || e}`);
                return false;
            }
        }

        logger.warn(`⏳ Node is not ready. Rejecting message so Heraldo Gestor will retry later.`);
        return false;
    }

    async stop() {
        if (this.client) {
            logger.warn(`🛑 Stopping WhatsApp client gracefully...`);
            await this.client.destroy().catch((e: any) => logger.error(`Cleanup failed: ${e}`));
            this.client = null;
        }
        this.sessionState = { status: 'OFFLINE', reason: 'Graceful Shutdown' };
    }

    async deleteAccount() {
        if (this.client) {
            logger.warn(`💀 Killing primary session`);
            await this.client.destroy().catch((e: any) => logger.error(`Cleanup failed: ${e}`));
            this.client = null;
        }
        this.sessionState = { status: 'OFFLINE', reason: 'Session data purged' };

        const sessionFolder = path.join(SESSIONS_DIR, `session-primary`);
        if (fs.existsSync(sessionFolder)) {
            fs.rmSync(sessionFolder, { recursive: true, force: true });
            logger.info(`🗑️ Deleted auth data for primary session`);
        }
    }
}