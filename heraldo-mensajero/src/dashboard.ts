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
    <script src="https://cdnjs.cloudflare.com/ajax/libs/qrcodejs/1.0.0/qrcode.min.js"></script>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Cinzel:wght@600;700&family=Cormorant+Garamond:wght@400;600;700&display=swap" rel="stylesheet">
    <style>
        *, *::before, *::after { box-sizing: border-box; margin: 0; padding: 0; }

        :root {
            --bg:       #1a1614;
            --surface:  #1e1917;
            --primary:  #8c1c1c;
            --primary-hover: #b71c1c;
            --text:     #d8cbb5;
            --muted:    #a89f91;
            --success:  #388e3c;
            --warning:  #d4af37;
            --danger:   #b71c1c;
            --border:   #8b7355;
            --secondary:#3a322b;
            --gold:     #d4af37;
        }

        body {
            font-family: 'Cormorant Garamond', serif;
            background-color: var(--bg);
            background-image: radial-gradient(circle at center, #2a2420 0%, #0d0b0a 100%);
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
            border-bottom: 2px dashed var(--border);
            text-align: center;
        }
        .page-header h1 {
            font-family: 'Cinzel', serif;
            font-size: clamp(1.6rem, 5vw, 2.2rem);
            color: var(--gold);
            text-shadow: 2px 2px 4px rgba(0,0,0,0.8);
            margin-bottom: 0.3rem;
        }
        .page-header p {
            color: var(--muted);
            font-size: 1.1rem;
            font-style: italic;
        }

        /* ── Engine status pill ── */
        .engine-status {
            display: inline-flex;
            align-items: center;
            gap: 0.5rem;
            font-family: 'Cinzel', serif;
            font-size: 0.85rem;
            font-weight: 600;
            color: var(--text);
            letter-spacing: 0.04em;
            text-transform: uppercase;
            margin-bottom: 1.25rem;
            background: rgba(0,0,0,0.4);
            padding: 6px 16px;
            border: 1px solid var(--border);
            border-radius: 4px;
            box-shadow: inset 0 0 10px rgba(0,0,0,0.5);
        }
        .engine-dot {
            width: 10px; height: 10px;
            border-radius: 50%;
            background: var(--secondary);
            flex-shrink: 0;
            border: 1px solid #000;
        }
        .dot-online   { background: var(--success); box-shadow: 0 0 8px var(--success); }
        .dot-offline  { background: var(--danger);  box-shadow: 0 0 8px var(--danger); }
        .dot-starting { background: var(--gold);    box-shadow: 0 0 8px var(--gold); animation: blink 1s infinite; }
        .dot-qr       { background: var(--gold);    box-shadow: 0 0 8px var(--gold); animation: blink 1s infinite; }

        /* ── Main card ── */
        .card {
            width: 100%;
            max-width: 480px;
            background: var(--surface);
            border: 3px double var(--border);
            border-radius: 4px;
            padding: 2rem 1.75rem;
            box-shadow: 0 15px 40px rgba(0,0,0,0.9), inset 0 0 20px rgba(0,0,0,0.5);
        }

        /* ── State views ── */
        .state-view {
            display: flex;
            flex-direction: column;
            align-items: center;
            text-align: center;
        }
        .state-icon   { font-size: 3rem; margin-bottom: 1rem; line-height: 1; text-shadow: 2px 2px 5px #000; }
        .state-title  { font-family: 'Cinzel', serif; font-size: 1.3rem; font-weight: 700; margin-bottom: 0.5rem; color: var(--gold); }
        .state-sub    { color: var(--muted); font-size: 1.1rem; line-height: 1.6; max-width: 300px; margin-bottom: 1.75rem; }

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
            border: 1px solid currentColor;
            margin-bottom: 1.5rem;
            box-shadow: inset 0 0 5px rgba(0,0,0,0.6);
        }
        .badge-online   { color: #a5d6a7; background: #1a2e1e; border-color: #388e3c; }
        .badge-offline  { color: #ff8a80; background: #3e1515; border-color: #b71c1c; }
        .badge-starting { color: #ffe082; background: #3e2713; border-color: #f57f17; animation: blink 1s infinite; }
        .badge-qr       { color: #ffe082; background: #3e2713; border-color: #f57f17; animation: blink 1s infinite; }

        /* ── Connected box ── */
        .connected-box {
            width: 100%;
            background: #25201c;
            border: 1px solid var(--border);
            border-left: 4px solid var(--success);
            padding: 1.5rem;
            margin-bottom: 1.25rem;
            box-shadow: inset 0 0 10px rgba(0,0,0,0.5);
        }
        .connected-box .icon { font-size: 2rem; margin-bottom: 0.75rem; }
        .connected-box h3 { font-family: 'Cinzel', serif; color: var(--gold); font-size: 1.1rem; margin-bottom: 0.3rem; }
        .connected-box p  { color: var(--muted); font-size: 1rem; }

        /* ── QR wrapper ── */
        .qr-wrapper {
            background: #ffffff;
            border: 4px solid var(--border);
            padding: 14px;
            margin-bottom: 1rem;
            box-shadow: 0 0 20px rgba(212, 175, 55, 0.2);
        }
        .qr-hint {
            font-family: 'Cinzel', serif;
            color: var(--gold);
            font-size: 0.85rem;
            letter-spacing: 0.05em;
            text-transform: uppercase;
            font-weight: 600;
            margin-bottom: 1.5rem;
        }

        /* ── Spinner (Medieval Sun/Astrolabe vibe) ── */
        .spinner {
            width: 52px; height: 52px;
            border: 3px dashed var(--gold);
            border-radius: 50%;
            animation: spin 3s linear infinite;
            margin-bottom: 1.25rem;
            box-shadow: 0 0 15px rgba(212, 175, 55, 0.2);
        }

        /* ── Buttons ── */
        .btn {
            width: 100%;
            padding: 0.85rem 1.25rem;
            font-family: 'Cinzel', serif;
            font-size: 0.95rem;
            font-weight: 700;
            text-transform: uppercase;
            letter-spacing: 1px;
            cursor: pointer;
            border: 1px solid var(--gold);
            transition: all 0.2s ease;
            box-shadow: 2px 2px 6px rgba(0,0,0,0.7);
        }
        .btn + .btn { margin-top: 0.75rem; }
        .btn-primary {
            background: linear-gradient(to bottom, #4a1c1c, #2a0b0b);
            color: #e6dfd1;
        }
        .btn-primary:hover { 
            background: linear-gradient(to bottom, #5c2323, #3a0f0f);
            color: #fff;
            transform: translateY(-1px); 
        }
        .btn-danger {
            background: linear-gradient(to bottom, #3a322b, #1f1b17);
            color: #c29b62;
            border-color: var(--border);
        }
        .btn-danger:hover { 
            background: linear-gradient(to bottom, #4a4037, #2a2520);
            color: var(--gold); 
        }

        /* ── Test message row ── */
        .send-row {
            display: flex;
            gap: 0.6rem;
            width: 100%;
            margin-bottom: 1rem;
        }
        .send-row input {
            flex: 1;
            background: #2a2420;
            border: 1px solid var(--border);
            padding: 0.7rem 0.9rem;
            color: var(--text);
            font-family: 'Cormorant Garamond', serif;
            font-size: 1.1rem;
            outline: none;
            transition: border-color 0.2s;
            min-width: 0;
        }
        .send-row input:focus { border-color: var(--gold); }
        .send-row input::placeholder { color: var(--secondary); font-style: italic; }
        .send-row button {
            background: linear-gradient(to bottom, #4a1c1c, #2a0b0b);
            color: #e6dfd1;
            border: 1px solid var(--gold);
            padding: 0.7rem 1.1rem;
            font-family: 'Cinzel', serif;
            font-weight: 700;
            font-size: 0.85rem;
            cursor: pointer;
            white-space: nowrap;
            transition: filter 0.2s;
            flex-shrink: 0;
            text-transform: uppercase;
        }
        .send-row button:hover { filter: brightness(1.2); }
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
        @keyframes blink { 0%,100% { opacity:1; } 50% { opacity:0.45; } }
    </style>
</head>
<body>

    <div class="page-header">
        <h1>📜 Heraldo Mensajero</h1>
        <p>WhatsApp Courier Engine</p>
    </div>

    <div class="engine-status">
        <div class="engine-dot" id="engine-dot"></div>
        <span id="engine-label">Consulting the Oracle...</span>
    </div>

    <div class="card" id="main-card">
        <div class="state-view">
            <div class="spinner"></div>
            <div class="state-sub">Connecting to the Courier Engine...</div>
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
                CONNECTED:   ['dot-online',   'Couriers Active \u00b7 Connected'],
                OFFLINE:     ['dot-offline',  'Couriers Asleep'],
                AUTH_FAILED: ['dot-offline',  'Seal Rejected'],
                STARTING:    ['dot-starting', 'Summoning Magic\u2026'],
                AWAITING_QR: ['dot-qr',       'Awaiting Royal Seal'],
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
                    <div class="state-title">Couriers are Asleep</div>
                    <div class="state-sub">\${reason ? 'Reason: ' + reason + '.' : 'Summon the engine to connect thy WhatsApp.'}</div>
                    <button class="btn btn-primary" onclick="startEngine()">Summon Couriers</button>
                </div>
            \`;
        }

        function renderStarting() {
            setEngineBar('STARTING');
            document.getElementById('main-card').innerHTML = \`
                <div class="state-view">
                    <div class="status-badge badge-starting">&bull; Awakening</div>
                    <div class="spinner"></div>
                    <div class="state-title">Awakening the mystical browser...</div>
                    <div class="state-sub">Patience, my lord. The magical seal (QR) shall appear shortly.</div>
                </div>
            \`;
        }

        function renderQR(qrCode) {
            setEngineBar('AWAITING_QR');
            document.getElementById('main-card').innerHTML = \`
                <div class="state-view">
                    <div class="status-badge badge-qr">&bull; Awaiting Royal Seal</div>
                    <div class="qr-wrapper"><div id="qr-code"></div></div>
                    <div class="qr-hint">WhatsApp &rarr; Linked Devices &rarr; Scan Seal</div>
                    <button class="btn btn-danger" onclick="stopEngine()">Abandon Summoning</button>
                </div>
            \`;
            setTimeout(() => {
                const el = document.getElementById('qr-code');
                if (el && qrCode) {
                    new QRCode(el, {
                        text: qrCode, width: 220, height: 220,
                        colorDark: '#1a1614', /* Dark brown instead of black for theme matching */
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
                        <p>The couriers stand ready to receive and deliver decrees from Heraldo Gestor.</p>
                    </div>
                    <div class="send-row">
                        <input id="test-msg" type="text" placeholder="Draft a test dispatch..." />
                        <button id="send-btn" onclick="sendTest()">Send</button>
                    </div>
                    <button class="btn btn-danger" onclick="stopEngine()">Dismiss Couriers (Disconnect)</button>
                </div>
            \`;
        }

        function renderAuthFailed() {
            setEngineBar('AUTH_FAILED');
            document.getElementById('main-card').innerHTML = \`
                <div class="state-view">
                    <div class="status-badge badge-offline">&bull; Seal Rejected</div>
                    <div class="state-icon">🚩</div>
                    <div class="state-title">Authentication Failed</div>
                    <div class="state-sub">The realm of WhatsApp rejected thy session. Summon the couriers anew and rescan the seal.</div>
                    <button class="btn btn-primary" onclick="startEngine()">Resummon Couriers</button>
                </div>
            \`;
        }

        // ── Actions ────────────────────────────────────────────────
        async function startEngine() {
            renderStarting();
            await fetch('/api/session/start', { method: 'POST', headers: API_HEADERS });
        }

        async function stopEngine() {
            if (!confirm('Dismiss the couriers?\\nThou wilt need to present the Royal Seal (QR) again to reconnect.')) return;
            await fetch('/api/session', { method: 'DELETE', headers: API_HEADERS });
            await update();
        }

        async function sendTest() {
            const input = document.getElementById('test-msg');
            const btn   = document.getElementById('send-btn');
            const msg   = input?.value.trim();
            if (!msg) return;
            btn.textContent = 'Flying\u2026';
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