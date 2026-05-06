// dashboard.ts
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

// 2. THE FIX: Tell the IDE that 'process' exists in this file context
declare const process: {
    env: {
        API_KEY?: string;
    };
};

export const getDashboardHtml = () => `
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Heraldo Mensajero</title>
    <script src="https://cdn.tailwindcss.com"></script>
    <script src="https://cdnjs.cloudflare.com/ajax/libs/qrcodejs/1.0.0/qrcode.min.js"></script>
    <style>
        body { background: #0f172a; color: #f8fafc; font-family: ui-sans-serif, system-ui, sans-serif; }
        .glass-card { background: rgba(30, 41, 59, 0.7); backdrop-filter: blur(10px); border: 1px solid rgba(51, 65, 85, 0.8); }
    </style>
</head>
<body class="p-4 md:p-8 pt-6 md:pt-10 flex flex-col items-center justify-center min-h-screen">
    <div class="max-w-2xl w-full mx-auto text-center">
        
        <!-- Header Section -->
        <div class="mb-10">
            <h1 class="text-4xl md:text-6xl font-extrabold bg-gradient-to-r from-purple-500 to-cyan-400 bg-clip-text text-transparent tracking-tight">
                ⚙️ Heraldo Mensajero
            </h1>
            <p class="text-slate-400 text-lg md:text-xl mt-3">Personal Communication Engine</p>
        </div>

        <div id="sys-stats" class="mb-8 text-sm md:text-base text-slate-400">
            <span class="animate-pulse">Scanning engine status...</span>
        </div>

        <!-- Main Card -->
        <div id="session-card" class="glass-card p-8 md:p-12 rounded-3xl shadow-2xl relative transition-all duration-300 max-w-lg mx-auto w-full">
            <!-- Content injected here -->
        </div>
        
    </div>

    <script>
        const headers = { 
            'Content-Type': 'application/json',
            'x-api-key': '${process.env.API_KEY}' 
        };

        async function startNode() {
            await fetch('/api/session/start', { method: 'POST', headers });
            await update();
        }

        async function killNode() {
            if (!confirm('Disconnect WhatsApp? You will need to scan the QR code again.')) return;
            await fetch('/api/session', { method: 'DELETE', headers });
            await update();
        }

        async function update() {
            try {
                const res = await fetch('/api/status');
                const data = await res.json();
                const state = data.session;
                
                let badgeClass = 'bg-slate-700 text-slate-300';
                let badgeIcon = '🔵';
                
                if (state.status === 'CONNECTED') { badgeClass = 'bg-emerald-500/20 text-emerald-400 border-emerald-500/30'; badgeIcon = '🟢'; }
                else if (state.status === 'AWAITING_QR') { badgeClass = 'bg-yellow-500/20 text-yellow-400 border-yellow-500/30'; badgeIcon = '🟡'; }
                else if (state.status === 'STARTING') { badgeClass = 'bg-purple-500/20 text-purple-400 border-purple-500/30 animate-pulse'; badgeIcon = '🟣'; }
                else if (state.status === 'OFFLINE' || state.status === 'AUTH_FAILED') { badgeClass = 'bg-red-500/20 text-red-400 border-red-500/30'; badgeIcon = '🔴'; }

                const isOffline = state.status === 'OFFLINE' || state.status === 'AUTH_FAILED';
                
                document.getElementById('sys-stats').innerHTML = isOffline ? 
                    '<span class="text-red-400">●</span> Engine Offline' : 
                    '<span class="text-emerald-400">●</span> Engine Active';

                const card = document.getElementById('session-card');
                
                if (isOffline) {
                    card.innerHTML = \`
                        <div class="flex flex-col items-center justify-center py-10">
                            <div class="text-6xl mb-6">🛑</div>
                            <h2 class="text-2xl font-bold mb-2">Engine is Offline</h2>
                            <p class="text-slate-400 mb-8">Start the engine to generate a WhatsApp QR code.</p>
                            <button onclick="startNode()" class="bg-cyan-600 hover:bg-cyan-500 text-white px-8 py-4 rounded-xl text-lg font-bold transition-colors w-full shadow-lg shadow-cyan-500/30">
                                Start WhatsApp Engine
                            </button>
                        </div>
                    \`;
                    return;
                }

                card.innerHTML = \`
                    <button onclick="killNode()" class="absolute top-4 right-4 bg-red-500/80 hover:bg-red-500 text-white text-xs px-3 py-1.5 rounded-lg opacity-80 hover:opacity-100 transition-opacity">
                        Disconnect
                    </button>
                    <div class="flex flex-col items-center mb-8">
                        <span class="px-4 py-2 rounded-full text-sm font-bold border \${badgeClass} shadow-sm mb-4">
                            \${badgeIcon} \${state.status}
                        </span>
                    </div>
                    <div class="flex flex-col items-center justify-center min-h-[250px]">
                        \${state.status === 'AWAITING_QR' ? '<div class="p-4 bg-white rounded-2xl shadow-2xl"><div id="qr-code"></div></div><p class="text-sm text-slate-400 mt-6 font-medium uppercase tracking-widest">Scan with WhatsApp</p>' : ''}
                        \${state.status === 'CONNECTED' ? '<div class="text-center w-full bg-slate-800/50 rounded-2xl p-8 border border-slate-700 shadow-inner"><div class="text-6xl mb-4">📱</div><h3 class="text-xl font-bold text-slate-200">Signal Established</h3><p class="text-slate-400 mt-2">Ready to receive tasks.</p></div>' : ''}
                        \${state.status === 'STARTING' ? '<div class="animate-spin rounded-full h-16 w-16 border-t-2 border-b-2 border-cyan-500 mb-4"></div><p class="text-slate-400">Booting Chromium Engine...</p>' : ''}
                    </div>
                \`;

                if (state.status === 'AWAITING_QR' && state.qrCode) {
                    setTimeout(() => {
                        const container = document.getElementById('qr-code');
                        if (container) {
                            container.innerHTML = '';
                            new QRCode(container, { text: state.qrCode, width: 220, height: 220, colorDark : "#0f172a", colorLight : "#ffffff", correctLevel : QRCode.CorrectLevel.L });
                        }
                    }, 50);
                }
            } catch (e) {
                console.error("Dashboard sync failed", e);
            }
        }
        
        setInterval(async () => { await update(); }, 2000); 
        update().catch(console.error);
    </script>
</body>
</html>
`;