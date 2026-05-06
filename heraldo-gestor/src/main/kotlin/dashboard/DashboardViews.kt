package com.netbug94.dashboard

import com.netbug94.tasks.TaskData

internal object DashboardViews {

    fun renderIndex(tasks: List<TaskData>, currentZone: String, formattedTime: String, authLink: String?): String {
        val taskListHtml = tasks.joinToString("") { task ->
            // Keep status text clear and simple, but use cool emojis
            val (statusClass, statusText, icon) = when {
                task.mensajeroDone -> Triple("sent", "Sent", "📜")
                else -> Triple("waiting", "Pending", "⏳")
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
            """<div class="empty-state">🕸️ No tasks scheduled for today.</div>"""
        } else {
            taskListHtml
        }

        val authBannerHtml = if (authLink != null) {
            """
            <div class="auth-banner">
                <div class="auth-banner-text">
                    <h3>🚨 Google Auth Required</h3>
                    <p>Background sync is paused. Please grant Google Authorization to resume.</p>
                </div>
                <a href="$authLink" target="_blank" class="btn-auth">Authorize Google</a>
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
                <link href="https://fonts.googleapis.com/css2?family=Cinzel:wght@500;700&family=Cormorant+Garamond:ital,wght@0,400;0,600;0,700;1,400&display=swap" rel="stylesheet">
                <!-- External Stylesheet -->
                <link rel="stylesheet" href="/static/styles.css">
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <div>
                            <h1>🏰 Heraldo Gestor</h1>
                            <div class="stats">
                                Tracking ${tasks.size} active missives for today<br>
                                <div class="zone-badge">🌍 Timezone: $currentZone ($formattedTime)</div>
                            </div>
                        </div>
                        <div class="btn-group">
                            <button onclick="openTemplateModal()" class="btn btn-secondary">Edit Template</button>
                            <a href="/sync-zone" class="btn btn-secondary">Sync Time</a>
                            <a href="/sync" class="btn">Refresh Tasks</a>
                        </div>
                    </div>
                    
                    $authBannerHtml
                    
                    <ul>
                        $finalList
                    </ul>
                </div>
                
                <button id="scroll-top" onclick="window.scrollTo({top:0,behavior:'smooth'})" title="Back to top">↑</button>

                <!-- Custom Medieval Modal -->
                <div id="medieval-modal" class="modal-overlay">
                    <div class="modal-content">
                        <h3 id="modal-title">Edit Message Template</h3>
                        <p id="modal-message"></p>
                        <input type="text" id="modal-input" placeholder="Enter template variables..." autocomplete="off">
                        <div class="modal-actions">
                            <button class="btn btn-secondary" onclick="closeModal()">Cancel</button>
                            <button class="btn" onclick="confirmModal()">Save Template</button>
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
                        showModal("Edit Message Template", "Enter the new text template for WhatsApp messages:", true, function(newTemplate) {
                            if (newTemplate) {
                                document.body.innerHTML = '<div style="background:#e8dfca; color:#2c241b; display:flex; justify-content:center; align-items:center; height:100vh; font-family:\'Cinzel\', serif;"><h2>Drafting Decree...</h2></div>';
                                fetch('/sync-env', {
                                    method: 'POST',
                                    headers: { 'Content-Type': 'application/json' },
                                    body: JSON.stringify({ template: newTemplate })
                                }).then(() => {
                                    window.location.href = '/';
                                }).catch(err => {
                                    showModal("Error", "Failed to update template: " + err, false, function() {
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

    // This interceptor fixes the texts sent from the backend routes
    fun renderLoadingState(message: String): String {
        val clearMessage = when(message) {
            "Summoning Decrees..." -> "Refreshing Tasks..."
            "Aligning Astrolabe..." -> "Updating Timezone..."
            "The Ravens have been Dispatched!" -> "Template Updated!"
            else -> message
        }

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
            <body style="background:#e8dfca; background-image: radial-gradient(circle at center, rgba(255,255,255,0.2) 0%, rgba(0,0,0,0.05) 100%); color:#2c241b; font-family:'Cinzel', serif; display:flex; justify-content:center; align-items:center; height:100vh; margin:0;">
                <h2 style="color: #2c241b; font-weight:700; border: 1px solid #c4b59d; padding: 20px 40px; background: #f4ecd8; box-shadow: 0 4px 15px rgba(0,0,0,0.06); position: relative;">
                    <div style="content: ''; position: absolute; width: 10px; height: 10px; border: 1px solid #c4b59d; top: 4px; left: 4px; border-right: none; border-bottom: none;"></div>
                    <div style="content: ''; position: absolute; width: 10px; height: 10px; border: 1px solid #c4b59d; bottom: 4px; right: 4px; border-left: none; border-top: none;"></div>
                    $clearMessage
                </h2>
            </body>
            </html>
        """.trimIndent()
    }
}