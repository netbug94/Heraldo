# Heraldo: Personal Automation Engine ⚙️

A rock-solid, containerized microservice architecture for personal task automation.
Composed of a Kotlin/Ktor backend (`heraldo-gestor`) that monitors Google APIs, and a Node.js/Puppeteer microservice (`heraldo-mensajero`) that dispatches real-time alerts directly to WhatsApp.

---

## 📋 Prerequisites
- **Docker** and **Docker Compose** installed on your server.
- A **Google Cloud Console** project with the Google Tasks and Calendar APIs enabled.
  - Read and Write permission for Tasks.
  - Read-only permission for Calendar.
- A **GitHub Gist** containing your timezone string (e.g., `America/Mexico_City`).

---

## 🚀 Plug-and-Play Deployment Guide

This repository is strictly configured to protect your sensitive data. Your API keys, WhatsApp sessions, and Google Credentials will **never** be committed to version control.

### 1. Clone the Repository
```bash
git clone https://github.com/your-username/Heraldo.git
cd Heraldo
```

### 2. Configure Environment
Create your environment file from the provided template:
```bash
cp .env.example .env
```
Open `.env` and fill in your details:
```bash
nano .env
```

- **HERALDO_INTERNAL_TOKEN**: A secure password used for communication between the two services.
  - **Action**: Run `openssl rand -hex 32` in your terminal and paste the result as the value.
- **MENSAJERO_PHONE**: The WhatsApp number that will receive the alerts.
  - **Format**: Include country code without `+` or spaces (e.g., `5211234567890`).
- **GIST_TIMEZONE_URL**: The **Raw** URL to your timezone GitHub Gist.
  - **Action**: Create a [New Gist](https://gist.github.com/) containing only your timezone string (e.g., `America/Mexico_City`). Click **"Raw"** and copy that URL.
- **GOOGLE_CREDENTIALS_JSON**: Your Google Cloud OAuth client secret.
  - **Action**: Download your `credentials.json` from the Google Cloud Console (APIs & Services > Credentials). Paste the entire JSON content here **inside single quotes**: `'{"installed":...}'`.
  - **Note**: Ensure your Google project is set to "Production" in the OAuth Consent Screen (no verification or payment required) to avoid token expiration.
- **DASHBOARD_USER/PASSWORD**: Set a username and password to protect your web dashboards.

### 3. Start the Engine
Navigate to the `scripts/` folder and run the bootstrapper using `bash`:
```bash
cd scripts
bash RISE.sh
```

### 4. Authenticate Services (One-Time Setup)

#### Step A: WhatsApp Authentication
1. **UI Path**: Go to the web dashboard at `http://your-server-ip:3000`. Login and click **"Kindle the Flame"**.
2. **Terminal Path**: Run `bash LOGS.sh` in the `scripts/` folder.
3. **Scan**: Open WhatsApp on your phone, go to **Linked Devices**, and scan the QR code that appears (either on screen or in terminal).

#### Step B: Google Authentication
1. **UI Path**: Go to the web dashboard at `http://your-server-ip:8080`.
2. **Renew**: Click **"Renew the Oath"** (or **"Stoke the Flame"** in the header).
3. **Authorize**: Copy the generated URL into your browser and authorize your account.
4. **Redirect**: The browser will redirect to a "Site cannot be reached" page (usually `http://localhost:8080/Callback?code=...`).
5. **Finalize**: 
   - **Option 1 (Recommended)**: Use SSH Tunneling (`ssh -L 8080:localhost:8080 user@server-ip`) before clicking the link so the redirect works automatically.
   - **Option 2 (Manual)**: Change `localhost` to `your-server-ip` in the address bar of the "Site cannot be reached" page and hit Enter.

---

## 🛠️ Useful Operations (Inside scripts/ only)

Management is handled via simple `bash` commands to ensure zero-friction execution.

| Command | Description |
| :--- | :--- |
| **bash RISE.sh** | Builds and starts the entire Heraldo stack. |
| **bash LOGS.sh** | Streams live output from both services. |
| **bash LOCKBREAK.sh** | Clears stuck Google lockfiles and restarts services. |
| **bash NUKE.sh** | Hard reset: Wipes all containers, images, and volumes. |

---

## 🧹 Maintenance

**Update the System:**
```bash
git pull
cd scripts
bash RISE.sh
```

**Completely Reset Google Session:**
```bash
# Delete the stored tokens
rm -rf heraldo-gestor/tokens/*
# Restart the engine to trigger a new auth flow
cd scripts
bash RISE.sh
```