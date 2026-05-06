// dashboard.ts

// Type declarations for QRCode library loaded via CDN
interface QRCodeOptions {
    text: string;
    width?: number;
    height?: number;
    colorDark?: string;
    colorLight?: string;
    correctLevel?: number;
}
declare class QRCode {
    static CorrectLevel: { L: number; M: number; Q: number; H: number };
    constructor(container: HTMLElement | null, options: string | QRCodeOptions);
}
declare const process: { env: { API_KEY?: string } };

export const getDashboardHtml = () => `
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Heraldo Mensajero</title>
    <script src="https://cdnjs.cloudflare.com/ajax/libs/qrcodejs/1.0.0/qrcode.min.js"><\/script>
    <style>
        *, *::before, *::after { box-sizing: border-box; margin: 0; padding: 0; }

        :root {
            --bg:       #0f172a;
            --surface:  #1e293b;
            --primary:  #8b5cf6;
            --text:     #f8fafc;
            --muted:    #94a3b8;
            --success:  #10b981;
            --warning:  #f59e0b;
            --danger:   #ef4444;
            --border:   #334155;
            --secondary:#475569;
            --cyan:     #06b6d4;
        }

        body {
            font-family: 'Segoe UI', system-ui, sans-serif;
            background-color: var(--bg);
            color: var(--text);
            min-height: 100dvh;
            display: flex;
            flex-direction: column;
            align-items: center;
            padding: 2rem 1rem 3rem;
        }

        /* ── Header ── */
        .page-header {
            width: 100%;
            max-width: 480px;
            margin-bottom: 1.75rem;
            padding-bottom: 1.5rem;
            border-bottom: 1px solid var(--border);
        }
        .page-header h1 {
            font-size: clamp(1.6rem, 5vw, 2.2rem);
            background: linear-gradient(to right, var(--primary), var(--cyan));
            -webkit-background-clip: text;
            -webkit-text-fill-color: transparent;
            background-clip: text;
        }
        .page-header p {
            color: var(--muted);
            font-size: 0.9rem;
            margin-top: 0.3rem;
        }

        /* ── Engine status pill ── */
        .engine-status {
            display: inline-flex;
            align-items: center;
            gap: 0.5rem;
            font-size: 0.8rem;
            font-weight: 600;
            color: var(--muted);
            letter-spacing: 0.04em;
            text-transform: uppercase;
            margin-bottom: 1.25rem;
        }
        .engine-dot {
            width: 8px; height: 8px;
            border-radius: 50%;
            background: var(--secondary);
            flex-shrink: 0;
        }
        .dot-online   { background: var(--success); box-shadow: 0 0 6px var(--success); }
        .dot-offline  { background: var(--danger);  box-shadow: 0 0 6px var(--danger); }
        .dot-starting { background: var(--primary); box-shadow: 0 0 6px var(--primary); animation: blink 1s infinite; }
        .dot-qr       { background: var(--warning); box-shadow: 0 0 6px var(--warning); animation: blink 1s infinite; }

        /* ── Main card ── */
        .card {
            width: 100%;
            max-width: 480px;
            background: var(--surface);
            border: 1px solid var(--border);
            border-radius: 16px;
            padding: 2rem 1.75rem;
            box-shadow: 0 20px 40px rgba(0,0,0,0.35);
        }

        /* ── State views ── */
        .state-view {
            display: flex;
            flex-direction: column;
            align-items: center;
            text-align: center;
        }
        .state-icon   { font-size: 3rem; margin-bottom: 1rem; line-height: 1; }
        .state-title  { font-size: 1.3rem; font-weight: 700; margin-bottom: 0.5rem; }
        .state-sub    { color: var(--muted); font-size: 0.9rem; line-height: 1.6; max-width: 300px; margin-bottom: 1.75rem; }

        /* ── Status badge ── */
        .status-badge {
            display: inline-flex;
            align-items: center;
            gap: 0.4rem;
            padding: 0.3rem 0.85rem;
            border-radius: 9999px;
            font-size: 0.78rem;
            font-weight: 700;
            letter-spacing: 0.06em;
            text-transform: uppercase;
            border: 1px solid currentColor;
            margin-bottom: 1.5rem;
        }
        .badge-online   { color: var(--success); background: rgba(16,185,129,0.1); }
        .badge-offline  { color: var(--danger);  background: rgba(239,68,68,0.1); }
        .badge-starting { color: var(--primary); background: rgba(139,92,246,0.1); animation: blink 1s infinite; }
        .badge-qr       { color: var(--warning); background: rgba(245,158,11,0.1); animation: blink 1s infinite; }

        /* ── Connected box ── */
        .connected-box {
            width: 100%;
            background: rgba(16,185,129,0.07);
            border: 1px solid rgba(16,185,129,0.2);
            border-radius: 12px;
            padding: 1.5rem;
            margin-bottom: 1.25rem;
        }
        .connected-box .icon { font-size: 2rem; margin-bottom: 0.75rem; }
        .connected-box h3 { color: var(--success); font-size: 1.05rem; margin-bottom: 0.3rem; }
        .connected-box p  { color: var(--muted); font-size: 0.875rem; }

        /* ── QR wrapper ── */
        .qr-wrapper {
            background: #ffffff;
            border-radius: 12px;
            padding: 14px;
            margin-bottom: 1rem;
            box-shadow: 0 0 30px rgba(139,92,246,0.2);
        }
        .qr-hint {
            color: var(--muted);
            font-size: 0.8rem;
            letter-spacing: 0.05em;
            text-transform: uppercase;
            font-weight: 600;
            margin-bottom: 1.5rem;
        }

        /* ── Spinner ── */
        .spinner {
            width: 52px; height: 52px;
            border: 3px solid rgba(139,92,246,0.15);
            border-top-color: var(--primary);
            border-radius: 50%;
            animation: spin 0.75s linear infinite;
            margin-bottom: 1.25rem;
        }

        /* ── Buttons ── */
        .btn {
            width: 100%;
            padding: 0.85rem 1.25rem;
            border-radius: 10px;
            font-family: inherit;
            font-size: 0.95rem;
            font-weight: 700;
            cursor: pointer;
            border: none;
            transition: all 0.2s;
        }
        .btn + .btn { margin-top: 0.75rem; }
        .btn-primary {
            background: var(--primary);
            color: #fff;
            box-shadow: 0 4px 14px rgba(139,92,246,0.35);
        }
        .btn-primary:hover { filter: brightness(1.1); transform: translateY(-1px); }
        .btn-danger {
            background: rgba(239,68,68,0.1);
            color: var(--danger);
            border: 1px solid rgba(239,68,68,0.3);
        }
        .btn-danger:hover { background: rgba(239,68,68,0.18); }

        /* ── Test message row ── */
        .send-row {
            display: flex;
            gap: 0.6rem;
            width: 100%;
            margin-bottom: 1rem;
        }
        .send-row input {
            flex: 1;
            background: rgba(255,255,255,0.04);
            border: 1px solid var(--border);
            border-radius: 8px;
            padding: 0.7rem 0.9rem;
            color: var(--text);
            font-family: inherit;
            font-size: 0.875rem;
            outline: none;
            transition: border-color 0.2s;
            min-width: 0;
        }
        .send-row input:focus { border-color: var(--primary); }
        .send-row input::placeholder { color: var(--secondary); }
        .send-row button {
            background: var(--primary);
            color: #fff;
            border: none;
            border-radius: 8px;
            padding: 0.7rem 1.1rem;
            font-family: inherit;
            font-weight: 700;
            font-size: 0.875rem;
            cursor: pointer;
            white-space: nowrap;
            transition: filter 0.2s;
            flex-shrink: 0;
        }
        .send-row button:hover { filter: brightness(1.1); }
        .send-row button:disabled { opacity: 0.5; cursor: default; }

        /* ── Footer ── */
        .page-footer {
            margin-top: 2rem;
            color: rgba(148,163,184,0.4);
            font-size: 0.72rem;
            letter-spacing: 0.05em;
            text-align: center;
        }

        @keyframes spin  { to { transform: rotate(360deg); } }
        @keyframes blink { 0%,100% { opacity:1; } 50% { opacity:0.45; } }
    </style>
</head>
<body>

    <div class="page-header">
        <h1>📡 Heraldo Mensajero</h1>
        <p>WhatsApp Delivery Engine</p>
    </div>

    <div class="engine-status">
        <div class="engine-dot" id="engine-dot"></div>
        <span id="engine-label">Checking status...</span>
    </div>

    <div class="card" id="main-card">
        <div class="state-view">
            <div class="spinner"></div>
            <div class="state-sub">Connecting to engine...</div>
        </div>
    </div>

    <div class="page-footer">Heraldo Mensajero &middot; Single-Session Engine</div>

    <script>
        const API_HEADERS = {
            'Content-Type': 'application/json',
            'x-api-key': '${process.env.API_KEY}'
        };

        // ── Engine status bar ──────────────────────────────────────
        function setEngineBar(status) {
            const dot = document.getElementById('engine-dot');
            const label = document.getElementById('engine-label');
            dot.className = 'engine-dot';
            const map = {
                CONNECTED:   ['dot-online',   'Engine Active \u00b7 Connected'],
                OFFLINE:     ['dot-offline',  'Engine Offline'],
                AUTH_FAILED: ['dot-offline',  'Auth Failed'],
                STARTING:    ['dot-starting', 'Engine Starting\u2026'],
                AWAITING_QR: ['dot-qr',       'Awaiting QR Scan'],
            };
            const [cls, text] = map[status] || ['', status];
            if (cls) dot.classList.add(cls);
            label.textContent = text;
        }

        // ── Renderers ──────────────────────────────────────────────
        function renderOffline(reason) {
            setEngineBar('OFFLINE');
            const card = document.getElementById('main-card');
            card.innerHTML = \`
                <div class="state-view">
                    <div class="status-badge badge-offline">&bull; Offline</div>
                    <div class="state-icon">🛑</div>
                    <div class="state-title">Engine is Offline</div>
                    <div class="state-sub">\${reason ? 'Reason: ' + reason + '.' : 'Start the engine to connect WhatsApp.'}</div>
                    <button class="btn btn-primary" onclick="startEngine()">Start WhatsApp Engine</button>
                </div>
            \`;
        }

        function renderStarting() {
            setEngineBar('STARTING');
            document.getElementById('main-card').innerHTML = \`
                <div class="state-view">
                    <div class="status-badge badge-starting">&bull; Starting</div>
                    <div class="spinner"></div>
                    <div class="state-title">Booting Chromium...</div>
                    <div class="state-sub">This takes a few seconds. The QR code will appear shortly.</div>
                </div>
            \`;
        }

        function renderQR(qrCode) {
            setEngineBar('AWAITING_QR');
            document.getElementById('main-card').innerHTML = \`
                <div class="state-view">
                    <div class="status-badge badge-qr">&bull; Awaiting QR Scan</div>
                    <div class="qr-wrapper"><div id="qr-code"></div></div>
                    <div class="qr-hint">WhatsApp &rarr; Linked Devices &rarr; Scan QR</div>
                    <button class="btn btn-danger" onclick="stopEngine()">Cancel &amp; Go Offline</button>
                </div>
            \`;
            setTimeout(() => {
                const el = document.getElementById('qr-code');
                if (el && qrCode) {
                    new QRCode(el, {
                        text: qrCode, width: 220, height: 220,
                        colorDark: '#0f172a', colorLight: '#ffffff',
                        correctLevel: QRCode.CorrectLevel.L
                    });
                }
            }, 50);
        }

        function renderConnected() {
            setEngineBar('CONNECTED');
            document.getElementById('main-card').innerHTML = \`
                <div class="state-view">
                    <div class="status-badge badge-online">&bull; Connected</div>
                    <div class="connected-box">
                        <div class="icon">📱</div>
                        <h3>Signal Established</h3>
                        <p>Ready to receive &amp; deliver tasks from Heraldo Gestor.</p>
                    </div>
                    <div class="send-row">
                        <input id="test-msg" type="text" placeholder="Test message text..." />
                        <button id="send-btn" onclick="sendTest()">Send</button>
                    </div>
                    <button class="btn btn-danger" onclick="stopEngine()">Disconnect Session</button>
                </div>
            \`;
        }

        function renderAuthFailed() {
            setEngineBar('AUTH_FAILED');
            document.getElementById('main-card').innerHTML = \`
                <div class="state-view">
                    <div class="status-badge badge-offline">&bull; Auth Failed</div>
                    <div class="state-icon">⚠️</div>
                    <div class="state-title">Authentication Failed</div>
                    <div class="state-sub">WhatsApp rejected the session. Start a new session and scan the QR code again.</div>
                    <button class="btn btn-primary" onclick="startEngine()">Restart Engine</button>
                </div>
            \`;
        }

        // ── Actions ────────────────────────────────────────────────
        async function startEngine() {
            renderStarting();
            await fetch('/api/session/start', { method: 'POST', headers: API_HEADERS });
        }

        async function stopEngine() {
            if (!confirm('Disconnect WhatsApp?\\nYou will need to scan the QR code again.')) return;
            await fetch('/api/session', { method: 'DELETE', headers: API_HEADERS });
            await update();
        }

        async function sendTest() {
            const input = document.getElementById('test-msg');
            const btn   = document.getElementById('send-btn');
            const msg   = input?.value.trim();
            if (!msg) return;
            btn.textContent = 'Sending\u2026';
            btn.disabled = true;
            try {
                await fetch('/api/sendText', {
                    method: 'POST',
                    headers: API_HEADERS,
                    body: JSON.stringify({ chatId: '', text: msg })
                });
            } finally {
                if (input) input.value = '';
                if (btn) { btn.textContent = 'Send'; btn.disabled = false; }
            }
        }

        // ── Polling loop ───────────────────────────────────────────
        async function update() {
            try {
                const res    = await fetch('/api/status');
                const data   = await res.json();
                const status = data.session?.status;

                if      (status === 'CONNECTED')   renderConnected();
                else if (status === 'AWAITING_QR') renderQR(data.session?.qrCode);
                else if (status === 'STARTING')    renderStarting();
                else if (status === 'AUTH_FAILED') renderAuthFailed();
                else                               renderOffline(data.session?.reason);
            } catch (e) {
                console.error('Dashboard poll failed', e);
            }
        }

        setInterval(update, 2500);
        update();
    <\/script>
</body>
</html>
`;