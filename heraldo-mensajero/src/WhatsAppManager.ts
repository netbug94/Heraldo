import pkg from 'whatsapp-web.js';
const { Client, LocalAuth } = pkg;
type WAClient = InstanceType<typeof Client>;
import qrcode from 'qrcode-terminal';
import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';
import { logger } from './logger.js';

// Workaround for __dirname in ES Modules (NodeNext)
const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const SESSIONS_DIR = path.join(__dirname, '../sessions');

export class WhatsAppManager {
    private client: WAClient | null = null;
    public sessionState: any = { status: 'OFFLINE' };

    constructor() {
        if (!fs.existsSync(SESSIONS_DIR)) fs.mkdirSync(SESSIONS_DIR, { recursive: true });
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
                // Priority: Docker Env > Standard Linux Path > Local OS Default
                executablePath: process.env.PUPPETEER_EXECUTABLE_PATH ||
                    (process.platform === 'linux' ? '/usr/bin/chromium' : undefined),
                headless: true,
                args: [
                    '--no-sandbox',
                    '--disable-setuid-sandbox',
                    '--disable-dev-shm-usage', // Recommended for Docker[cite: 3]
                    '--disable-extensions'
                ]
            }
        });

        client.on('qr', (qr) => {
            this.sessionState = { status: 'AWAITING_QR', qrCode: qr };
            qrcode.generate(qr, { small: true });
        });

        client.on('ready', () => {
            this.sessionState = { status: 'CONNECTED' };
            this.client = client;
            logger.info(`✅ Primary Session Connected`);
            fetch('http://gestor:8080/webhook/mensajero', { method: 'POST' }).catch(() => {});
        });

        client.on('disconnected', (reason) => {
            logger.error(`❌ Disconnected. Reason: ${reason}`);
            this.sessionState = { status: 'OFFLINE', reason };
            client.destroy().catch((e: any) => logger.error(`Cleanup failed: ${e}`));
            this.client = null;
            fetch('http://gestor:8080/webhook/mensajero', { method: 'POST' }).catch(() => {});
        });

        client.on('auth_failure', (msg) => {
            logger.error(`⚠️ Auth Failure: ${msg}`);
            this.sessionState = { status: 'AUTH_FAILED' };
        });

        try {
            await client.initialize();
        } catch (err) {
            logger.error(`🚨 Failed to initialize client: ${err}`);
            this.sessionState = { status: 'OFFLINE' };
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
        this.sessionState = { status: 'OFFLINE', reason: 'Killed via Dashboard' };
        const sessionFolder = path.join(SESSIONS_DIR, `session-primary`);
        if (fs.existsSync(sessionFolder)) {
            fs.rmSync(sessionFolder, { recursive: true, force: true });
            logger.info(`🗑️ Deleted auth data for primary session`);
        }
    }
}