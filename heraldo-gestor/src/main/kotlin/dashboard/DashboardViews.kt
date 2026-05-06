package com.netbug94.dashboard

import com.netbug94.tasks.TaskData

internal object DashboardViews {

    fun renderIndex(tasks: List<TaskData>, currentZone: String, formattedTime: String, authLink: String?): String {
        val taskListHtml = tasks.joinToString("") { task ->
            val (statusClass, statusText, icon) = when {
                task.mensajeroDone -> Triple("sent", "Dispatched", "📜")
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
            """<div class="empty-state">🕸️ No decrees bear thy name this day.</div>"""
        } else {
            taskListHtml
        }

        val authBannerHtml = if (authLink != null) {
            """
            <div class="auth-banner">
                <div class="auth-banner-text">
                    <h3>🚨 Royal Seal Required</h3>
                    <p>The couriers are halted. Grant Google Authorization to resume thy background sync (ensure the magical SSH tunnel remains open).</p>
                </div>
                <a href="$authLink" target="_blank" class="btn-auth">Sign the Decree (Login)</a>
            </div>
            """
        } else ""

        return """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Heraldo Gestor Dashboard</title>
                <meta http-equiv="refresh" content="30">
                <!-- Medieval Typography -->
                <link rel="preconnect" href="https://fonts.googleapis.com">
                <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
                <link href="https://fonts.googleapis.com/css2?family=Cinzel:wght@600;700&family=Cormorant+Garamond:wght@400;600;700&display=swap" rel="stylesheet">
                <!-- External Stylesheet -->
                <link rel="stylesheet" href="/static/styles.css">
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <div>
                            <h1>🏰 Heraldo Gestor</h1>
                            <div class="stats">
                                Overseeing ${tasks.size} active commands for this day<br>
                                <div class="zone-badge">🌍 Realm: $currentZone ($formattedTime)</div>
                            </div>
                        </div>
                        <div class="btn-group">
                            <button onclick="openTemplateModal()" class="btn btn-secondary">Draft New Seal</button>
                            <a href="/sync-zone" class="btn btn-secondary">Align Astrolabe</a>
                            <a href="/sync" class="btn">Summon Tasks</a>
                        </div>
                    </div>
                    
                    $authBannerHtml
                    
                    <ul>
                        $finalList
                    </ul>
                </div>
                
                <button id="scroll-top" onclick="window.scrollTo({top:0,behavior:'smooth'})" title="Ascend">↑</button>

                <!-- Custom Medieval Modal -->
                <div id="medieval-modal" class="modal-overlay">
                    <div class="modal-content">
                        <h3 id="modal-title">Royal Decree</h3>
                        <p id="modal-message"></p>
                        <input type="text" id="modal-input" placeholder="Enter thy words..." autocomplete="off">
                        <div class="modal-actions">
                            <button class="btn btn-secondary" onclick="closeModal()">Abandon</button>
                            <button class="btn" onclick="confirmModal()">Seal it</button>
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

                    // Custom Modal Logic (replaces prompt/alert)
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
                        showModal("Draft New Seal", "Enter the new wording for the royal relay:", true, function(newTemplate) {
                            if (newTemplate) {
                                document.body.innerHTML = '<div style="background:#1a1614; color:#d4af37; display:flex; justify-content:center; align-items:center; height:100vh; font-family:\'Cinzel\', serif;"><h2>Dispatching ravens...</h2></div>';
                                fetch('/sync-env', {
                                    method: 'POST',
                                    headers: { 'Content-Type': 'application/json' },
                                    body: JSON.stringify({ template: newTemplate })
                                }).then(() => {
                                    window.location.href = '/';
                                }).catch(err => {
                                    showModal("Failure", "The ravens failed to depart: " + err, false, function() {
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

    fun renderLoadingState(message: String): String {
        return """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta http-equiv="refresh" content="1.5;url=/" />
                <title>Heraldo Gestor - Working...</title>
                <link rel="preconnect" href="https://fonts.googleapis.com">
                <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
                <link href="https://fonts.googleapis.com/css2?family=Cinzel:wght@600;700&display=swap" rel="stylesheet">
            </head>
            <body style="background:#1a1614; background-image: radial-gradient(circle at center, #2a2420 0%, #0d0b0a 100%); color:#d8cbb5; font-family:'Cinzel', serif; display:flex; justify-content:center; align-items:center; height:100vh; margin:0;">
                <h2 style="color: #d4af37; text-shadow: 2px 2px 4px #000; border: 2px solid #8b7355; padding: 20px 40px; background: #1e1917; box-shadow: 0 10px 30px rgba(0,0,0,0.8);">$message</h2>
            </body>
            </html>
        """.trimIndent()
    }
}