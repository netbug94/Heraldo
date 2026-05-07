// Type declarations for QRCode library are not strictly needed here as they are inside the HTML string,
// but we'll keep the process.env access clean by letting Node.js types handle it.


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
    <link href="https://fonts.googleapis.com/css2?family=Cinzel:wght@400;600;700&family=Cormorant+Garamond:ital,wght@0,400;0,600;1,400&display=swap" rel="stylesheet">
    <link rel="icon" href="data:image/svg+xml,<svg xmlns=%22http://www.w3.org/2000/svg%22 viewBox=%220 0 100 100%22><text y=%22.9em%22 font-size=%2290%22>🪶</text></svg>">
    <style>
        *, *::before, *::after { box-sizing: border-box; margin: 0; padding: 0; }

        :root {
            /* The Abyss Theme */
            --bg:       #050505; /* The Abyss */
            --surface:  #0a0a0a; /* Cold dark stone */
            --primary:  #a83a22; /* Dying Ember */
            --primary-hover: #c8492c; /* Stoked Ember */
            --text:     #a39f98; /* Ash */
            --muted:    #54514d; /* Dark Ash */
            --success:  #c87a2a; /* Bonfire Lit */
            --warning:  #635c55; /* Cold Iron */
            --danger:   #7a1a1a; /* Blood */
            --border:   #242220; /* Tarnished Metal */
            --glow:     rgba(200, 122, 42, 0.15); /* Faint fire glow */
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
            background-image: radial-gradient(circle at center, #11100f 0%, #030303 100%);
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
            color: var(--success);
            font-weight: 400;
            margin-bottom: 0.3rem;
            letter-spacing: 4px;
            text-transform: uppercase;
            text-shadow: 0 0 20px var(--glow);
        }
        .page-header p {
            color: var(--muted);
            font-size: 1.2rem;
            font-style: italic;
            letter-spacing: 1px;
        }

        /* ── Engine status pill ── */
        .engine-status {
            display: inline-flex;
            align-items: center;
            gap: 0.8rem;
            font-family: 'Cinzel', serif;
            font-size: 0.85rem;
            font-weight: 600;
            color: var(--text);
            letter-spacing: 2px;
            text-transform: uppercase;
            margin-bottom: 1.25rem;
            padding: 8px 20px;
            border: 1px solid var(--border);
            background: rgba(10, 10, 10, 0.6);
            backdrop-filter: blur(4px);
        }
        .engine-dot {
            width: 8px; height: 8px;
            background: var(--muted);
            flex-shrink: 0;
            border-radius: 0; /* Square for rougher medieval feel */
            transform: rotate(45deg); /* Diamond shape */
            transition: all 0.5s ease;
        }
        .dot-online   { background: var(--success); box-shadow: 0 0 10px var(--success); }
        .dot-offline  { background: var(--danger); box-shadow: 0 0 10px var(--danger); }
        .dot-starting { background: var(--text); animation: ember-pulse 1.5s infinite; }
        .dot-qr       { background: var(--primary); animation: ember-pulse 1.5s infinite; }

        /* ── Main card ── */
        .card {
            width: 100%;
            max-width: 480px;
            background: rgba(10, 10, 10, 0.8);
            border: 1px solid var(--border);
            padding: 3rem 2rem;
            box-shadow: inset 0 0 50px rgba(0,0,0,0.9), 0 10px 30px rgba(0,0,0,0.8);
            position: relative;
            backdrop-filter: blur(5px);
        }
        /* Wrought Iron Corner Braces */
        .card::before, .card::after {
            content: '';
            position: absolute;
            width: 20px; height: 20px;
            border: 2px solid var(--muted);
            opacity: 0.5;
        }
        .card::before { top: -1px; left: -1px; border-right: none; border-bottom: none; }
        .card::after  { bottom: -1px; right: -1px; border-left: none; border-top: none; }

        /* ── State views ── */
        .state-view {
            display: flex;
            flex-direction: column;
            align-items: center;
            text-align: center;
        }
        .state-icon   { font-size: 2.5rem; margin-bottom: 1rem; line-height: 1; filter: grayscale(100%) brightness(80%); opacity: 0.7; }
        .state-title  { font-family: 'Cinzel', serif; font-size: 1.3rem; font-weight: 400; letter-spacing: 2px; margin-bottom: 0.5rem; color: var(--text); text-transform: uppercase; }
        .state-sub    { color: var(--muted); font-size: 1.15rem; font-style: italic; line-height: 1.6; max-width: 300px; margin-bottom: 2rem; }

        /* ── Status badge ── */
        .status-badge {
            display: inline-flex;
            align-items: center;
            gap: 0.6rem;
            padding: 0.3rem 0;
            font-family: 'Cinzel', serif;
            font-size: 0.85rem;
            font-weight: 600;
            letter-spacing: 2px;
            text-transform: uppercase;
            margin-bottom: 1.5rem;
            border-bottom: 1px solid currentColor;
        }
        .badge-online   { color: var(--success); text-shadow: 0 0 10px var(--glow); }
        .badge-offline  { color: var(--danger); text-shadow: 0 0 10px rgba(122,26,26,0.4); }
        .badge-starting { color: var(--text); animation: ember-pulse 2s infinite; }
        .badge-qr       { color: var(--primary); animation: ember-pulse 2s infinite; }

        /* ── Connected box ── */
        .connected-box {
            width: 100%;
            background: rgba(0,0,0,0.4);
            border: 1px solid var(--border);
            border-left: 2px solid var(--success);
            padding: 1.5rem;
            margin-bottom: 1.5rem;
            box-shadow: inset 0 0 20px rgba(200, 122, 42, 0.05);
        }
        .connected-box .icon { font-size: 1.8rem; margin-bottom: 0.5rem; opacity: 0.8; }
        .connected-box h3 { font-family: 'Cinzel', serif; color: var(--success); font-size: 1.1rem; margin-bottom: 0.3rem; letter-spacing: 1px; font-weight: 400; text-transform: uppercase; }
        .connected-box p  { color: var(--text); font-size: 1.05rem; font-style: italic; }

        /* ── QR wrapper (Summoning Sign) ── */
        .qr-wrapper {
            background: #d3ccc0; /* Old bone / ash color to make QR scannable */
            border: 3px solid var(--border);
            padding: 14px;
            margin-bottom: 1rem;
            box-shadow: 0 0 30px rgba(168, 58, 34, 0.2);
            position: relative;
        }
        .qr-wrapper::before {
            content: ''; position: absolute; inset: -6px; border: 1px solid var(--muted); opacity: 0.3;
        }
        .qr-hint {
            font-family: 'Cinzel', serif;
            color: var(--muted);
            font-size: 0.85rem;
            letter-spacing: 2px;
            text-transform: uppercase;
            font-weight: 600;
            margin-bottom: 2rem;
        }

        /* ── Spinner (The Dark Sign) ── */
        .spinner {
            width: 50px; height: 50px;
            border: 2px solid rgba(200, 122, 42, 0.1);
            border-top: 2px solid var(--success);
            border-radius: 50%;
            animation: spin 2s cubic-bezier(0.68, -0.55, 0.265, 1.55) infinite;
            margin-bottom: 1.5rem;
            box-shadow: 0 0 15px var(--glow);
        }

        /* ── Buttons ── */
        .btn {
            width: 100%;
            padding: 1rem 1.25rem;
            font-family: 'Cinzel', serif;
            font-size: 0.9rem;
            font-weight: 600;
            text-transform: uppercase;
            letter-spacing: 2px;
            cursor: pointer;
            transition: all 0.3s ease;
            background: transparent;
            position: relative;
            overflow: hidden;
            color: var(--text);
            border: 1px solid var(--border);
        }
        .btn + .btn { margin-top: 0.75rem; }
        
        .btn-primary:hover { 
            color: var(--success);
            border-color: var(--success);
            box-shadow: 0 0 15px var(--glow), inset 0 0 10px var(--glow);
            text-shadow: 0 0 8px var(--success);
        }
        
        .btn-danger {
            color: var(--muted);
        }
        .btn-danger:hover { 
            color: var(--danger);
            border-color: var(--danger);
            box-shadow: 0 0 15px rgba(122,26,26,0.3), inset 0 0 10px rgba(122,26,26,0.2);
            text-shadow: 0 0 8px var(--danger);
        }

        /* ── Test message row ── */
        .send-row {
            display: flex;
            gap: 0.5rem;
            width: 100%;
            margin-bottom: 1.5rem;
        }
        .send-row input {
            flex: 1;
            background: rgba(0,0,0,0.5);
            border: none;
            border-bottom: 1px solid var(--muted);
            padding: 0.8rem 1rem;
            color: var(--text);
            font-family: 'Cormorant Garamond', serif;
            font-size: 1.15rem;
            outline: none;
            transition: border-color 0.3s;
            min-width: 0;
            text-align: center;
        }
        .send-row input:focus { border-bottom-color: var(--success); color: var(--success); }
        .send-row input::placeholder { color: var(--muted); font-style: italic; }
        .send-row button {
            background: transparent;
            color: var(--text);
            border: 1px solid var(--border);
            padding: 0.8rem 1.2rem;
            font-family: 'Cinzel', serif;
            font-weight: 600;
            font-size: 0.85rem;
            cursor: pointer;
            white-space: nowrap;
            transition: all 0.3s;
            flex-shrink: 0;
            text-transform: uppercase;
            letter-spacing: 1px;
        }
        .send-row button:hover { 
            color: var(--success);
            border-color: var(--success);
            box-shadow: 0 0 10px var(--glow); 
        }
        .send-row button:disabled { opacity: 0.3; cursor: default; }

        /* ── Footer ── */
        .page-footer {
            margin-top: 3rem;
            color: var(--muted);
            font-family: 'Cinzel', serif;
            font-size: 0.75rem;
            letter-spacing: 3px;
            text-align: center;
            text-transform: uppercase;
            opacity: 0.6;
        }

        @keyframes spin  { 0% { transform: rotate(0deg); } 100% { transform: rotate(360deg); } }
        @keyframes ember-pulse { 0%,100% { opacity:1; filter: brightness(1); } 50% { opacity:0.6; filter: brightness(0.5); } }
    </style>
</head>
<body>

    <div class="page-header">
        <h1>Heraldo Mensajero</h1>
        <p>The Abyssal Engine</p>
    </div>

    <div class="engine-status">
        <div class="engine-dot" id="engine-dot"></div>
        <span id="engine-label">Gazing into the abyss...</span>
    </div>

    <div class="card" id="main-card">
        <div class="state-view">
            <div class="spinner"></div>
            <div class="state-sub">Communing with the ancient signals...</div>
        </div>
    </div>

    <div class="page-footer">Heraldo Mensajero &middot; Solitary Summoning</div>

    <script>
        const API_HEADERS = {
            'Content-Type': 'application/json',
            'x-api-key': '${process.env.API_KEY}'
        };
        const TEST_PHONE = '${process.env.MENSAJERO_PHONE || ''}';

        // ── Engine status bar ──────────────────────────────────────
        function setEngineBar(status) {
            const dot = document.getElementById('engine-dot');
            const label = document.getElementById('engine-label');
            dot.className = 'engine-dot';
            const map = {
                CONNECTED:   ['dot-online',   'The Flame is Kindled'],
                OFFLINE:     ['dot-offline',  'The Fire Fades...'],
                AUTH_FAILED: ['dot-offline',  'Curse of the Undead'],
                STARTING:    ['dot-starting', 'Kindling the Engine...'],
                AWAITING_QR: ['dot-qr',       'Awaiting the Sigil'],
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
                    <div class="status-badge badge-offline">🌑 Dormant</div>
                    <div class="state-icon">⚔️</div>
                    <div class="state-title">The Engine Slumbers</div>
                    <div class="state-sub">\${reason ? 'The curse reads: ' + reason + '.' : 'Stoke the bonfire to awaken the engine.'}</div>
                    <button class="btn btn-primary" onclick="startEngine()">Kindle the Flame</button>
                </div>
            \`;
        }

        function renderStarting() {
            setEngineBar('STARTING');
            document.getElementById('main-card').innerHTML = \`
                <div class="state-view">
                    <div class="status-badge badge-starting">🌫️ Traversing the Fog</div>
                    <div class="spinner"></div>
                    <div class="state-title">Summoning Phantoms...</div>
                    <div class="state-sub">The abyss takes time to answer. The summoning sigil will appear shortly.</div>
                </div>
            \`;
        }

        function renderQR(qrCode) {
            setEngineBar('AWAITING_QR');
            document.getElementById('main-card').innerHTML = \`
                <div class="state-view">
                    <div class="status-badge badge-qr">👁️ Awaiting Sigil</div>
                    <div class="qr-wrapper"><div id="qr-code"></div></div>
                    <div class="qr-hint">WhatsApp &rarr; Linked Devices &rarr; Offer Sigil</div>
                    <button class="btn btn-danger" onclick="stopEngine()">Abandon Summoning</button>
                </div>
            \`;
            setTimeout(() => {
                const el = document.getElementById('qr-code');
                if (el && qrCode) {
                    new QRCode(el, {
                        text: qrCode, width: 220, height: 220,
                        colorDark: '#050505', /* Pitch black for camera scanning */
                        colorLight: '#d3ccc0', /* Bone/Ash background */
                        correctLevel: QRCode.CorrectLevel.L
                    });
                }
            }, 50);
        }

        function renderConnected() {
            setEngineBar('CONNECTED');
            document.getElementById('main-card').innerHTML = \`
                <div class="state-view">
                    <div class="status-badge badge-online">🔥 Kindled</div>
                    <div class="connected-box">
                        <div class="icon">📜</div>
                        <h3>Covenant Established</h3>
                        <p>The link is forged. Ready to channel souls from the Gestor.</p>
                    </div>
                    <div class="send-row">
                        <input id="test-msg" type="text" placeholder="Inscribe a phantom message..." />
                        <button id="send-btn" onclick="sendTest()">Invoke</button>
                    </div>
                    <div style="font-size: 0.8rem; color: var(--muted); margin-bottom: 1.5rem; font-style: italic;">
                        \${TEST_PHONE ? 'Targeting: ' + TEST_PHONE : '⚠️ No MENSAJERO_PHONE configured'}
                    </div>
                    <button class="btn btn-danger" onclick="stopEngine()">Sever the Link</button>
                </div>
            \`;
        }

        function renderAuthFailed() {
            setEngineBar('AUTH_FAILED');
            document.getElementById('main-card').innerHTML = \`
                <div class="state-view">
                    <div class="status-badge badge-offline">🩸 Curse</div>
                    <div class="state-icon">🛡️</div>
                    <div class="state-title">Sigil Rejected</div>
                    <div class="state-sub">The old blood refused the connection. Kindle the flame and offer your sigil anew.</div>
                    <button class="btn btn-primary" onclick="startEngine()">Rekindle Flame</button>
                </div>
            \`;
        }

        // ── Actions ────────────────────────────────────────────────
        async function startEngine() {
            renderStarting();
            await fetch('/api/session/start', { method: 'POST', headers: API_HEADERS });
        }

        async function stopEngine() {
            if (!confirm('Sever the connection?\\nYou will need to offer your sigil again to return.')) return;
            await fetch('/api/session', { method: 'DELETE', headers: API_HEADERS });
            await update();
        }

        async function sendTest() {
            const input = document.getElementById('test-msg');
            const btn   = document.getElementById('send-btn');
            const msg   = input?.value.trim();
            if (!msg) return;
            if (!TEST_PHONE) {
                alert('The abyss is silent: No MENSAJERO_PHONE defined in .env');
                return;
            }
            btn.textContent = 'Invoking...';
            btn.disabled = true;
            try {
                await fetch('/api/sendText', {
                    method: 'POST',
                    headers: API_HEADERS,
                    body: JSON.stringify({ chatId: TEST_PHONE, text: msg })
                });
            } finally {
                if (input) input.value = '';
                if (btn) { btn.textContent = 'Invoke'; btn.disabled = false; }
            }
        }

        // ── Polling loop ───────────────────────────────────────────
        let lastStatus = null;
        let lastQr = null;

        async function update() {
            try {
                const res    = await fetch('/api/status');
                const data   = await res.json();
                const status = data.session?.status;
                const qr     = data.session?.qrCode;

                // Only re-render if the status changed, OR if we are in QR mode and the code changed
                const statusChanged = status !== lastStatus;
                const qrChanged = status === 'AWAITING_QR' && qr !== lastQr;

                if (statusChanged || qrChanged) {
                    if      (status === 'CONNECTED')   renderConnected();
                    else if (status === 'AWAITING_QR') renderQR(qr);
                    else if (status === 'STARTING')    renderStarting();
                    else if (status === 'AUTH_FAILED') renderAuthFailed();
                    else                               renderOffline(data.session?.reason);
                    
                    lastStatus = status;
                    lastQr = qr;
                }
            } catch (e) {
                console.error('The abyss stares back (poll failed)', e);
            }
        }

        setInterval(update, 2500);
        update();
    </script>
</body>
</html>
`;