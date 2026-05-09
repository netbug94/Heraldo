package com.netbug94.auth

internal object LoginView {

    fun renderLogin(error: String? = null): String {
        val errorBanner = if (error != null) {
            """<div class="error-banner">⚔️ $error</div>"""
        } else ""

        return """
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Heraldo Gestor — Enter</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Cinzel:wght@600;700&family=Cormorant+Garamond:wght@400;600;700&display=swap" rel="stylesheet">
    <style>
        *, *::before, *::after { box-sizing: border-box; margin: 0; padding: 0; }

        :root {
            --bg:      #1a1614;
            --surface: #1e1917;
            --gold:    #d4af37;
            --text:    #d8cbb5;
            --muted:   #a89f91;
            --border:  #8b7355;
            --danger:  #b71c1c;
        }

        body {
            font-family: 'Cormorant Garamond', serif;
            background: var(--bg);
            background-image: radial-gradient(circle at center, #2a2420 0%, #0d0b0a 100%);
            color: var(--text);
            min-height: 100dvh;
            display: flex;
            flex-direction: column;
            align-items: center;
            justify-content: center;
            padding: 2rem 1rem;
        }

        .crest {
            font-size: 3.5rem;
            margin-bottom: 0.5rem;
            filter: drop-shadow(0 0 16px rgba(212,175,55,0.4));
            animation: float 4s ease-in-out infinite;
        }

        @keyframes float {
            0%, 100% { transform: translateY(0); }
            50%       { transform: translateY(-8px); }
        }

        h1 {
            font-family: 'Cinzel', serif;
            font-size: clamp(1.5rem, 5vw, 2rem);
            color: var(--gold);
            text-shadow: 2px 2px 6px rgba(0,0,0,0.9);
            margin-bottom: 0.25rem;
            letter-spacing: 0.04em;
        }

        .subtitle {
            color: var(--muted);
            font-size: 1.05rem;
            font-style: italic;
            margin-bottom: 2rem;
        }

        .card {
            width: 100%;
            max-width: 380px;
            background: var(--surface);
            border: 3px double var(--border);
            border-radius: 4px;
            padding: 2rem 1.75rem;
            box-shadow: 0 20px 50px rgba(0,0,0,0.95), inset 0 0 20px rgba(0,0,0,0.5);
        }

        .error-banner {
            background: #3e1515;
            border: 1px solid var(--danger);
            border-left: 4px solid var(--danger);
            color: #ff8a80;
            padding: 0.65rem 1rem;
            font-family: 'Cinzel', serif;
            font-size: 0.8rem;
            letter-spacing: 0.03em;
            margin-bottom: 1.25rem;
        }

        label {
            display: block;
            font-family: 'Cinzel', serif;
            font-size: 0.78rem;
            font-weight: 700;
            letter-spacing: 0.06em;
            text-transform: uppercase;
            color: var(--muted);
            margin-bottom: 0.4rem;
        }

        input[type="text"],
        input[type="password"] {
            width: 100%;
            background: #2a2420;
            border: 1px solid var(--border);
            border-radius: 2px;
            padding: 0.7rem 0.9rem;
            color: var(--text);
            font-family: 'Cormorant Garamond', serif;
            font-size: 1.1rem;
            outline: none;
            transition: border-color 0.2s, box-shadow 0.2s;
            margin-bottom: 1.25rem;
        }

        input[type="text"]:focus,
        input[type="password"]:focus {
            border-color: var(--gold);
            box-shadow: 0 0 0 2px rgba(212,175,55,0.15);
        }

        button[type="submit"] {
            width: 100%;
            padding: 0.85rem 1.25rem;
            font-family: 'Cinzel', serif;
            font-size: 0.95rem;
            font-weight: 700;
            text-transform: uppercase;
            letter-spacing: 1.5px;
            cursor: pointer;
            border: 1px solid var(--gold);
            border-radius: 2px;
            background: linear-gradient(to bottom, #2a3d1c, #162008);
            color: #e6dfd1;
            box-shadow: 2px 2px 8px rgba(0,0,0,0.7);
            transition: all 0.2s ease;
        }

        button[type="submit"]:hover {
            background: linear-gradient(to bottom, #3a5228, #1e2e0a);
            color: #fff;
            transform: translateY(-1px);
            box-shadow: 2px 4px 12px rgba(0,0,0,0.8);
        }

        .divider {
            border: none;
            border-top: 1px dashed var(--border);
            margin: 1.5rem 0 1.25rem;
        }

        .footer-note {
            text-align: center;
            color: var(--muted);
            font-family: 'Cinzel', serif;
            font-size: 0.72rem;
            letter-spacing: 0.04em;
            margin-top: 1.75rem;
        }
    </style>
</head>
<body>
    <div class="crest">🏰</div>
    <h1>Heraldo Gestor</h1>
    <p class="subtitle">Present thy seal to enter</p>

    <div class="card">
        $errorBanner
        <form method="POST" action="/login" autocomplete="off">
            <label for="username">Scribe Name</label>
            <input id="username" type="text" name="username" autocomplete="username" required autofocus>

            <label for="password">Royal Seal</label>
            <input id="password" type="password" name="password" autocomplete="current-password" required>

            <hr class="divider">

            <button type="submit">Enter the Realm</button>
        </form>
    </div>

    <p class="footer-note">Heraldo Gestor &middot; Task Overseer</p>
</body>
</html>
        """.trimIndent()
    }
}
