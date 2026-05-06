package com.netbug94.dashboard

import com.netbug94.tasks.TaskData

internal object DashboardViews {

    // --- NEW: Added authLink parameter ---
    fun renderIndex(tasks: List<TaskData>, currentZone: String, formattedTime: String, authLink: String?): String {
        val taskListHtml = tasks.joinToString("") { task ->
            val (statusClass, statusText, icon) = when {
                task.mensajeroDone -> Triple("sent", "Sent", "✅")
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
            """<div class="empty-state">📭 No timed calendar events found for today.</div>"""
        } else {
            taskListHtml
        }

        // --- NEW: Dynamic Banner HTML ---
        val authBannerHtml = if (authLink != null) {
            """
            <div class="auth-banner">
                <div class="auth-banner-text">
                    <h3>🚨 Google Authentication Required</h3>
                    <p>Background sync is paused. Authorize to resume (ensure your SSH tunnel is active).</p>
                </div>
                <a href="$authLink" target="_blank" class="btn-auth">Login with Google</a>
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
                <link rel="stylesheet" href="/static/styles.css">
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <div>
                            <h1>🧠 Heraldo Gestor</h1>
                            <div class="stats">
                                Tracking ${tasks.size} active events for today<br>
                                <div class="zone-badge">🌍 $currentZone ($formattedTime)</div>
                            </div>
                        </div>
                        <div class="btn-group">
                            <button onclick="updateTemplate()" class="btn btn-secondary">Update Template</button>
                            <a href="/sync-zone" class="btn btn-secondary">Sync Zone</a>
                            <a href="/sync" class="btn">Sync Tasks</a>
                        </div>
                    </div>
                    
                    $authBannerHtml
                    
                    <ul>
                        $finalList
                    </ul>
                </div>
                
                <button id="scroll-top" onclick="window.scrollTo({top:0,behavior:'smooth'})" title="Back to top">↑</button>
                
                <script>
                    const btn = document.getElementById('scroll-top');
                    window.addEventListener('scroll', () => {
                        btn.classList.toggle('visible', window.scrollY > 200);
                    }, { passive: true });

                    function updateTemplate() {
                        const newTemplate = prompt("Enter new Relay Template:", "");
                        if (newTemplate) {
                            document.body.innerHTML = '<div style="background:#0f172a; color:#06b6d4; display:flex; justify-content:center; align-items:center; height:100vh; font-family:sans-serif;"><h2>Sending...</h2></div>';
                            fetch('/sync-env', {
                                method: 'POST',
                                headers: { 'Content-Type': 'application/json' },
                                body: JSON.stringify({ template: newTemplate })
                            }).then(() => {
                                window.location.href = '/';
                            }).catch(err => {
                                alert("Failed to update template: " + err);
                                window.location.reload();
                            });
                        }
                    }
                </script>
            </body>
            </html>
        """.trimIndent()
    }

    fun renderLoadingState(message: String): String {
        return """
            <html><head><meta http-equiv="refresh" content="1;url=/" /></head>
            <body style="background:#0f172a; color:#f8fafc; font-family:sans-serif; display:flex; justify-content:center; align-items:center; height:100vh; margin:0;">
            <h2 style="background: linear-gradient(to right, #8b5cf6, #06b6d4); -webkit-background-clip: text; -webkit-text-fill-color: transparent;">$message</h2>
            </body></html>
        """.trimIndent()
    }
}