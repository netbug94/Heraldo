package com.netbug94.dashboard

import com.netbug94.tasks.TaskData

internal object DashboardViews {

    fun renderIndex(tasks: List<TaskData>, currentZone: String, formattedTime: String, authLink: String?, currentTemplate: String): String {
        val taskListHtml = tasks.joinToString("") { task ->
            val (statusClass, statusText, icon) = when {
                task.mensajeroDone -> Triple("sent", "Fulfilled", "🪶")
                else -> Triple("waiting", "Awaiting", "⏳")
            }
            """
            <li class="task-item">
                <div class="task-info">
                    <span class="task-time">${task.dueTime}</span>
                    <span class="task-title">${task.title}</span>
                </div>
                <span class="badge $statusClass">$icon $statusText</span>
            </li>
            """.trimIndent()
        }

        val finalList = if (tasks.isEmpty()) {
            """<div class="empty-state">The fire fades... No souls to gather today.</div>"""
        } else {
            taskListHtml
        }

        val authBannerHtml = if (authLink != null) {
            """
            <div class="auth-banner">
                <div class="auth-banner-text">
                    <h3>The Covenant is Broken</h3>
                    <p>Background sync is severed. Offer your sigil (Google Auth) to restore the link.</p>
                </div>
                <a href="$authLink" target="_blank" class="btn-auth">Renew the Oath</a>
            </div>
            """
        } else ""

        // UPDATED: Subtle, collapsible, mobile-friendly template editor
        val templateFormHtml = """
            <details style="margin: 1.5rem 0; padding: 0.75rem 1rem; background: rgba(10, 10, 10, 0.4); border: 1px dashed #3a3835; border-radius: 4px; cursor: pointer;">
                <summary style="color: #8a867e; font-family: 'Cinzel', serif; font-size: 0.9rem; letter-spacing: 1px; text-transform: uppercase; outline: none; user-select: none;">
                    ⚙️ Inscribe Decree Template
                </summary>
                <form action="/template" method="POST" style="display: flex; gap: 0.5rem; margin-top: 1rem; flex-wrap: wrap; cursor: default;">
                    <input type="text" name="template" value="$currentTemplate" style="flex: 1 1 200px; background: #050505; color: #c87a2a; border: 1px solid #54514d; padding: 10px 12px; font-family: 'Cormorant Garamond', serif; font-size: 1.15rem; outline: none; box-sizing: border-box;">
                    <button type="submit" class="btn btn-secondary" style="flex: 0 1 auto; white-space: nowrap;">Seal Template</button>
                </form>
            </details>
        """.trimIndent()

        return """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
                <title>Heraldo Gestor</title>
                <link rel="preconnect" href="https://fonts.googleapis.com">
                <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
                <link href="https://fonts.googleapis.com/css2?family=Cinzel:wght@400;600;700&family=Cormorant+Garamond:ital,wght@0,400;0,600;1,400&display=swap" rel="stylesheet">
                <link rel="icon" href="data:image/svg+xml,<svg xmlns=%22http://www.w3.org/2000/svg%22 viewBox=%220 0 100 100%22><text y=%22.9em%22 font-size=%2290%22>⛓️</text></svg>">
                <link rel="stylesheet" href="/static/styles.css">
                
                <style>
                    @media (max-width: 600px) {
                        /* Stack the header text and buttons vertically on phones */
                        .header {
                            flex-direction: column !important;
                            align-items: flex-start !important;
                            gap: 1rem;
                        }
                        /* Make buttons full width on small screens */
                        .btn-group {
                            width: 100%;
                            display: flex;
                            flex-direction: column;
                            gap: 0.5rem;
                        }
                        .btn-group .btn {
                            width: 100%;
                            text-align: center;
                            box-sizing: border-box;
                        }
                        /* Prevent long titles from breaking the task list layout */
                        .task-item {
                            flex-direction: column !important;
                            align-items: flex-start !important;
                            gap: 0.75rem;
                        }
                        .badge {
                            align-self: flex-start !important;
                        }
                    }
                    /* Custom scrollbar for the template details to avoid layout shifts */
                    details > summary { list-style: none; }
                    details > summary::-webkit-details-marker { display: none; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header" style="display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap;">
                        <div>
                            <h1 style="margin-bottom: 0.2rem;">Heraldo Gestor</h1>
                            <div class="stats">
                                Bearing ${tasks.size} lingering souls today<br>
                                <div class="zone-badge" style="display: inline-block; margin-top: 0.5rem;">Realm: $currentZone ($formattedTime)</div>
                            </div>
                        </div>
                        <div class="btn-group">
                            <a href="/sync-zone" class="btn btn-secondary">Align to Stars</a>
                            <a href="/sync" class="btn">Stoke the Flame</a>
                        </div>
                    </div>
                    
                    $authBannerHtml
                    
                    $templateFormHtml
                    
                    <ul style="padding-left: 0; list-style: none;">
                        $finalList
                    </ul>
                </div>
                
                <button id="scroll-top" onclick="window.scrollTo({top:0,behavior:'smooth'})" title="Ascend" style="position: fixed; bottom: 20px; right: 20px; z-index: 100;">⏶</button>
                
                <script>
                    // WebSocket Logic
                    const wsProtocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
                    const ws = new WebSocket(wsProtocol + '//' + window.location.host + '/ws');
                    ws.onmessage = function(event) {
                        if (event.data === 'RELOAD') {
                            window.location.reload();
                        }
                    };

                    // Scroll to top Logic
                    const btn = document.getElementById('scroll-top');
                    window.addEventListener('scroll', () => {
                        btn.style.display = window.scrollY > 200 ? 'block' : 'none';
                    }, { passive: true });
                </script>
            </body>
            </html>
        """.trimIndent()
    }

    fun renderLoadingState(message: String): String {
        val (clearMessage, colorClass) = when(message) {
            "Summoning Decrees..." -> "SUMMONING PHANTOMS" to "#c87a2a" // Bonfire Gold
            "Aligning Astrolabe..." -> "CELESTIAL ALIGNMENT" to "#8a867e" // Ash Gray
            "The Ravens have been Dispatched!" -> "DECREE INSCRIBED" to "#7a1a1a" // Blood Red
            else -> message.uppercase() to "#c87a2a"
        }

        return """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <meta http-equiv="refresh" content="1.5;url=/" />
                <title>Heraldo Gestor - Traversing the Fog...</title>
                <link rel="preconnect" href="https://fonts.googleapis.com">
                <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
                <link href="https://fonts.googleapis.com/css2?family=Cinzel:wght@400&display=swap" rel="stylesheet">
            </head>
            <body style="background:#000000; color:$colorClass; font-family:'Cinzel', serif; display:flex; justify-content:center; align-items:center; height:100vh; margin:0; overflow: hidden; padding: 1rem;">
                <h2 style="font-weight:400; font-size: clamp(1.5rem, 6vw, 4rem); letter-spacing: clamp(4px, 2vw, 12px); text-align: center; text-shadow: 0 0 30px $colorClass; opacity: 0; animation: fadeInOut 1.5s ease-in-out forwards;">
                    $clearMessage
                </h2>
                <style>
                    @keyframes fadeInOut {
                        0% { opacity: 0; transform: scale(0.98); }
                        40% { opacity: 1; transform: scale(1); }
                        80% { opacity: 1; transform: scale(1); }
                        100% { opacity: 0; transform: scale(1.02); }
                    }
                </style>
            </body>
            </html>
        """.trimIndent()
    }
}