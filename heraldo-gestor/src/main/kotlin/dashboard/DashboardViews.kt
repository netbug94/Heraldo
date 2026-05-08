package com.netbug94.dashboard

import com.netbug94.tasks.TaskData

internal object DashboardViews {

    fun renderIndex(tasks: List<TaskData>, currentZone: String, formattedTime: String, authLink: String?): String {
        val taskListHtml = tasks.joinToString("") { task ->
            val (statusClass, statusText, icon) = when {
                task.mensajeroDone -> Triple("sent", "Fulfilled", "🔥")
                else -> Triple("waiting", "Awaiting", "🌑")
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

        return """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Heraldo Gestor</title>
                <!-- Gothic Typography -->
                <link rel="preconnect" href="https://fonts.googleapis.com">
                <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
                <link href="https://fonts.googleapis.com/css2?family=Cinzel:wght@400;600;700&family=Cormorant+Garamond:ital,wght@0,400;0,600;1,400&display=swap" rel="stylesheet">
                <link rel="icon" href="data:image/svg+xml,<svg xmlns=%22http://www.w3.org/2000/svg%22 viewBox=%220 0 100 100%22><text y=%22.9em%22 font-size=%2290%22>🔥</text></svg>">
                <!-- External Stylesheet -->
                <link rel="stylesheet" href="/static/styles.css">
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <div>
                            <h1>Heraldo Gestor</h1>
                            <div class="stats">
                                Bearing ${tasks.size} lingering souls today<br>
                                <div class="zone-badge">Realm: $currentZone ($formattedTime)</div>
                            </div>
                        </div>
                        <div class="btn-group">
                            <a href="/sync-zone" class="btn btn-secondary">Align to Stars</a>
                            <a href="/sync" class="btn">Stoke the Flame</a>
                        </div>
                    </div>
                    
                    $authBannerHtml
                    
                    <ul>
                        $finalList
                    </ul>
                </div>
                
                <button id="scroll-top" onclick="window.scrollTo({top:0,behavior:'smooth'})" title="Ascend">▲</button>
                
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
                        btn.classList.toggle('visible', window.scrollY > 200);
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
                <meta http-equiv="refresh" content="1.5;url=/" />
                <title>Heraldo Gestor - Traversing the Fog...</title>
                <link rel="preconnect" href="https://fonts.googleapis.com">
                <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
                <link href="https://fonts.googleapis.com/css2?family=Cinzel:wght@400&display=swap" rel="stylesheet">
            </head>
            <body style="background:#000000; color:$colorClass; font-family:'Cinzel', serif; display:flex; justify-content:center; align-items:center; height:100vh; margin:0; overflow: hidden;">
                <h2 style="font-weight:400; font-size: clamp(2rem, 5vw, 4rem); letter-spacing: 12px; text-align: center; text-shadow: 0 0 30px $colorClass; opacity: 0; animation: fadeInOut 1.5s ease-in-out forwards;">
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