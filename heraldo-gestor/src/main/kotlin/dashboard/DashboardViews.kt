package com.netbug94.dashboard

import com.netbug94.tasks.TaskData

internal object DashboardViews {

    fun renderIndex(tasks: List<TaskData>, currentZone: String, formattedTime: String, authLink: String?): String {
        val taskListHtml = tasks.joinToString("") { task ->
            // Dark Souls thematic statuses
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
                <a href="$authLink" target="_blank" class="btn-auth">Restore Covenant</a>
            </div>
            """
        } else ""

        return """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Heraldo Gestor - The Abyss</title>
                <meta http-equiv="refresh" content="30">
                <!-- Gothic Typography -->
                <link rel="preconnect" href="https://fonts.googleapis.com">
                <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
                <link href="https://fonts.googleapis.com/css2?family=Cinzel:wght@400;600;700&family=Cormorant+Garamond:ital,wght@0,400;0,600;1,400&display=swap" rel="stylesheet">
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
                            <button onclick="openTemplateModal()" class="btn btn-secondary">Forge Decree</button>
                            <a href="/sync-zone" class="btn btn-secondary">Align Abyss</a>
                            <a href="/sync" class="btn">Stoke the Flame</a>
                        </div>
                    </div>
                    
                    $authBannerHtml
                    
                    <ul>
                        $finalList
                    </ul>
                </div>
                
                <button id="scroll-top" onclick="window.scrollTo({top:0,behavior:'smooth'})" title="Ascend">▲</button>

                <!-- Gothic Modal -->
                <div id="medieval-modal" class="modal-overlay">
                    <div class="modal-content">
                        <h3 id="modal-title">Forge New Decree</h3>
                        <p id="modal-message"></p>
                        <input type="text" id="modal-input" placeholder="Inscribe your will..." autocomplete="off">
                        <div class="modal-actions">
                            <button class="btn btn-secondary" onclick="closeModal()">Abandon</button>
                            <button class="btn" onclick="confirmModal()">Inscribe</button>
                        </div>
                    </div>
                </div>
                
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

                    // Custom Modal Logic
                    let modalCallback = null;

                    function showModal(title, message, isPrompt, callback) {
                        document.getElementById('medieval-modal').style.display = 'flex';
                        document.getElementById('modal-title').innerText = title;
                        document.getElementById('modal-message').innerText = message;
                        const input = document.getElementById('modal-input');
                        
                        if (isPrompt) {
                            input.style.display = 'block';
                            input.value = '';
                            input.focus();
                        } else {
                            input.style.display = 'none';
                        }
                        modalCallback = callback;
                    }

                    function closeModal() {
                        document.getElementById('medieval-modal').style.display = 'none';
                        modalCallback = null;
                    }

                    function confirmModal() {
                        const input = document.getElementById('modal-input');
                        const result = input.style.display === 'block' ? input.value.trim() : true;
                        
                        if (input.style.display === 'block' && !result) {
                            input.focus();
                            return; // Don't allow empty submission
                        }
                        
                        closeModal();
                        if (modalCallback) modalCallback(result);
                    }

                    function openTemplateModal() {
                        showModal("Forge New Decree", "Inscribe the words to be carried by the phantoms:", true, function(newTemplate) {
                            if (newTemplate) {
                                document.body.innerHTML = '<div style="background:#050505; color:#c87a2a; display:flex; justify-content:center; align-items:center; height:100vh; font-family:\'Cinzel\', serif; letter-spacing: 6px; text-transform: uppercase;"><h2 style="font-weight: 400; text-shadow: 0 0 20px rgba(200, 122, 42, 0.4);">Inscribing Decree...</h2></div>';
                                fetch('/sync-env', {
                                    method: 'POST',
                                    headers: { 'Content-Type': 'application/json' },
                                    body: JSON.stringify({ template: newTemplate })
                                }).then(() => {
                                    window.location.href = '/';
                                }).catch(err => {
                                    showModal("Curse", "The inscription failed: " + err, false, function() {
                                        window.location.reload();
                                    });
                                });
                            }
                        });
                    }
                </script>
            </body>
            </html>
        """.trimIndent()
    }

    // Cinematic "YOU DIED" / "BONFIRE LIT" style loading screens
    fun renderLoadingState(message: String): String {
        val (clearMessage, colorClass) = when(message) {
            "Summoning Decrees..." -> "SUMMONING PHANTOMS" to "#c87a2a" // Bonfire Gold
            "Aligning Astrolabe..." -> "ALIGNING THE ABYSS" to "#8a867e" // Ash Gray
            "The Ravens have been Dispatched!" -> "DECREE FORGED" to "#7a1a1a" // Blood Red
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