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
    <script src="https://cdnjs.cloudflare.com/ajax/libs/qrcodejs/1.0.0/qrcode.min.js"></script>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Cinzel:wght@500;700&family=Cormorant+Garamond:ital,wght@0,400;0,600;0,700;1,400&display=swap" rel="stylesheet">
    <style>
        *, *::before, *::after { box-sizing: border-box; margin: 0; padding: 0; }

        :root {
            /* Parchment & Ink Theme */
            --bg:       #e8dfca; /* Darker parchment backdrop */
            --surface:  #f4ecd8; /* Lighter vellum for cards */
            --primary:  #8b2635; /* Wax seal red */
            --primary-hover: #6e1c28;
            --text:     #2c241b; /* Iron gall ink */
            --muted:    #5c5346; /* Faded ink */
            --success:  #3a5a40; /* Muted forest green */
            --warning:  #967230; /* Antiqued gold/brass */
            --danger:   #8b2635; 
            --border:   #c4b59d; /* Faint ink line */
            --gold:     #7a5c24; /* Muted matte gold */
        }

        body {
            font-family: 'Cormorant Garamond', serif;
            background-color: var(--bg);
            color: var(--text);
            min-height: 100dvh;
            display: flex;
            flex-direction: column;
            align-items: center;
            padding: 2rem 1rem 3rem;
            /* Subtle paper texture illusion using radial gradient */
            background-image: radial-gradient(circle at center, rgba(255,255,255,0.2) 0%, rgba(0,0,0,0.05) 100%);
        }

        /* ── Header ── */
        .page-header {
            width: 100%;
            max-width: 480px;
            margin-bottom: 1.75rem;
            padding-bottom: 1.5rem;
            border-bottom: 1px solid var(--border);
            text-align: center;
        }
        .page-header h1 {
            font-family: 'Cinzel', serif;
            font-size: clamp(1.6rem, 5vw, 2.2rem);
            color: var(--text);
            font-weight: 700;
            margin-bottom: 0.3rem;
            letter-spacing: 0.02em;
        }
        .page-header p {
            color: var(--muted);
            font-size: 1.2rem;
            font-style: italic;
        }

        /* ── Engine status pill ── */
        .engine-status {
            display: inline-flex;
            align-items: center;
            gap: 0.6rem;
            font-family: 'Cinzel', serif;
            font-size: 0.85rem;
            font-weight: 600;
            color: var(--text);
            letter-spacing: 0.04em;
            text-transform: uppercase;
            margin-bottom: 1.25rem;
            padding: 6px 16px;
            border: 1px solid var(--border);
            background: rgba(255, 255, 255, 0.3);
        }
        .engine-dot {
            width: 8px; height: 8px;
            border-radius: 50%;
            background: var(--muted);
            flex-shrink: 0;
        }
        .dot-online   { background: var(--success); }
        .dot-offline  { background: var(--danger); }
        .dot-starting { background: var(--warning); animation: blink 1.5s infinite; }
        .dot-qr       { background: var(--warning); animation: blink 1.5s infinite; }

        /* ── Main card ── */
        .card {
            width: 100%;
            max-width: 480px;
            background: var(--surface);
            border: 1px solid var(--border);
            padding: 2.5rem 2rem;
            box-shadow: 0 4px 15px rgba(0,0,0,0.06), 0 1px 3px rgba(0,0,0,0.03);
            position: relative;
        }
        /* Corner flourishes using pseudo-elements */
        .card::before, .card::after {
            content: '';
            position: absolute;
            width: 15px; height: 15px;
            border: 1px solid var(--border);
        }
        .card::before { top: 6px; left: 6px; border-right: none; border-bottom: none; }
        .card::after  { bottom: 6px; right: 6px; border-left: none; border-top: none; }

        /* ── State views ── */
        .state-view {
            display: flex;
            flex-direction: column;
            align-items: center;
            text-align: center;
        }
        .state-icon   { font-size: 2.5rem; margin-bottom: 1rem; line-height: 1; filter: grayscale(20%) opacity(0.9); }
        .state-title  { font-family: 'Cinzel', serif; font-size: 1.3rem; font-weight: 700; margin-bottom: 0.5rem; color: var(--text); }
        .state-sub    { color: var(--muted); font-size: 1.15rem; line-height: 1.6; max-width: 300px; margin-bottom: 1.75rem; }

        /* ── Status badge ── */
        .status-badge {
            display: inline-flex;
            align-items: center;
            gap: 0.4rem;
            padding: 0.3rem 0.85rem;
            font-family: 'Cinzel', serif;
            font-size: 0.78rem;
            font-weight: 700;
            letter-spacing: 0.06em;
            text-transform: uppercase;
            margin-bottom: 1.5rem;
            border-bottom: 2px solid currentColor;
        }
        .badge-online   { color: var(--success); }
        .badge-offline  { color: var(--danger); }
        .badge-starting { color: var(--warning); animation: blink 1.5s infinite; }
        .badge-qr       { color: var(--warning); animation: blink 1.5s infinite; }

        /* ── Connected box ── */
        .connected-box {
            width: 100%;
            background: rgba(0,0,0,0.02);
            border: 1px solid var(--border);
            border-left: 3px solid var(--success);
            padding: 1.5rem;
            margin-bottom: 1.5rem;
        }
        .connected-box .icon { font-size: 1.8rem; margin-bottom: 0.5rem; opacity: 0.8; }
        .connected-box h3 { font-family: 'Cinzel', serif; color: var(--text); font-size: 1.1rem; margin-bottom: 0.3rem; }
        .connected-box p  { color: var(--muted); font-size: 1.05rem; }

        /* ── QR wrapper ── */
        .qr-wrapper {
            background: #ffffff;
            border: 1px solid var(--border);
            padding: 14px;
            margin-bottom: 1rem;
        }
        .qr-hint {
            font-family: 'Cinzel', serif;
            color: var(--muted);
            font-size: 0.85rem;
            letter-spacing: 0.05em;
            text-transform: uppercase;
            font-weight: 600;
            margin-bottom: 1.5rem;
        }

        /* ── Spinner (Simple Astrolabe Ring) ── */
        .spinner {
            width: 48px; height: 48px;
            border: 2px solid transparent;
            border-top: 2px solid var(--text);
            border-right: 2px solid var(--text);
            border-radius: 50%;
            animation: spin 1.5s linear infinite;
            margin-bottom: 1.25rem;
        }

        /* ── Buttons ── */
        .btn {
            width: 100%;
            padding: 0.85rem 1.25rem;
            font-family: 'Cinzel', serif;
            font-size: 0.95rem;
            font-weight: 600;
            text-transform: uppercase;
            letter-spacing: 1px;
            cursor: pointer;
            transition: all 0.2s ease;
            background: transparent;
        }
        .btn + .btn { margin-top: 0.75rem; }
        .btn-primary {
            background: var(--primary);
            color: #f4ecd8;
            border: 1px solid var(--primary);
        }
        .btn-primary:hover { 
            background: var(--primary-hover);
            border-color: var(--primary-hover);
        }
        .btn-danger {
            color: var(--danger);
            border: 1px solid var(--border);
        }
        .btn-danger:hover { 
            background: rgba(139, 38, 53, 0.05);
            border-color: var(--danger);
        }

        /* ── Test message row ── */
        .send-row {
            display: flex;
            gap: 0.5rem;
            width: 100%;
            margin-bottom: 1rem;
        }
        .send-row input {
            flex: 1;
            background: transparent;
            border: 1px solid var(--border);
            border-bottom: 2px solid var(--border);
            padding: 0.7rem 0.9rem;
            color: var(--text);
            font-family: 'Cormorant Garamond', serif;
            font-size: 1.15rem;
            outline: none;
            transition: border-color 0.2s;
            min-width: 0;
        }
        .send-row input:focus { border-color: var(--text); }
        .send-row input::placeholder { color: var(--muted); font-style: italic; }
        .send-row button {
            background: var(--text);
            color: var(--surface);
            border: 1px solid var(--text);
            padding: 0.7rem 1.1rem;
            font-family: 'Cinzel', serif;
            font-weight: 600;
            font-size: 0.85rem;
            cursor: pointer;
            white-space: nowrap;
            transition: background 0.2s;
            flex-shrink: 0;
            text-transform: uppercase;
        }
        .send-row button:hover { background: #1a1510; }
        .send-row button:disabled { opacity: 0.5; cursor: default; }

        /* ── Footer ── */
        .page-footer {
            margin-top: 2rem;
            color: var(--muted);
            font-family: 'Cinzel', serif;
            font-size: 0.75rem;
            letter-spacing: 0.05em;
            text-align: center;
        }

        @keyframes spin  { to { transform: rotate(360deg); } }
        @keyframes blink { 0%,100% { opacity:1; } 50% { opacity:0.5; } }
    </style>
</head>
<body>

    <div class="page-header">
        <h1>📜 Heraldo Mensajero</h1>
        <p>WhatsApp Engine</p>
    </div>

    <div class="engine-status">
        <div class="engine-dot" id="engine-dot"></div>
        <span id="engine-label">Checking status...</span>
    </div>

    <div class="card" id="main-card">
        <div class="state-view">
            <div class="spinner"></div>
            <div class="state-sub">Connecting to WhatsApp Engine...</div>
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
                CONNECTED:   ['dot-online',   'WhatsApp Engine: Connected'],
                OFFLINE:     ['dot-offline',  'WhatsApp Engine: Offline'],
                AUTH_FAILED: ['dot-offline',  'Auth Failed'],
                STARTING:    ['dot-starting', 'Starting Engine...'],
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
                    <div class="state-icon">🛡️</div>
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
                    <div class="state-sub">This takes a few moments. The QR cipher will appear shortly.</div>
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
                    <button class="btn btn-danger" onclick="stopEngine()">Cancel & Go Offline</button>
                </div>
            \`;
            setTimeout(() => {
                const el = document.getElementById('qr-code');
                if (el && qrCode) {
                    new QRCode(el, {
                        text: qrCode, width: 220, height: 220,
                        colorDark: '#2c241b', /* Matching the Iron gall ink text color */
                        colorLight: '#ffffff',
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
                        <div class="icon">📜</div>
                        <h3>Signal Established</h3>
                        <p>Ready to receive and deliver tasks from Heraldo Gestor.</p>
                    </div>
                    <div class="send-row">
                        <input id="test-msg" type="text" placeholder="Draft a missive..." />
                        <button id="send-btn" onclick="sendTest()">Send</button>
                    </div>
                    <button class="btn btn-danger" onclick="stopEngine()">Sever Connection</button>
                </div>
            \`;
        }

        function renderAuthFailed() {
            setEngineBar('AUTH_FAILED');
            document.getElementById('main-card').innerHTML = \`
                <div class="state-view">
                    <div class="status-badge badge-offline">&bull; Auth Failed</div>
                    <div class="state-icon">🚩</div>
                    <div class="state-title">Authentication Failed</div>
                    <div class="state-sub">WhatsApp rejected the session. Restart the engine and scan the QR code anew.</div>
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
            if (!confirm('Disconnect WhatsApp?\\nYou will need to scan the QR code again to reconnect.')) return;
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
    </script>
</body>
</html>
`;