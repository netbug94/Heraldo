# Heraldo: Personal Automation Engine ⚙️

A rock-solid, containerized microservice architecture for personal task automation.
Composed of a Kotlin backend (`heraldo-gestor`) that monitors Google APIs, and a Node.js/Puppeteer microservice (`heraldo-mensajero`) that dispatches real-time alerts directly to WhatsApp.

---

## 📋 Prerequisites
- **Docker** and **Docker Compose** installed on your server.
- A **Google Cloud Console** project with the Google Tasks and Calendar APIs
  - Read and Write permission for  Tasks
  - Only Read permission for Calendar
- A **GitHub Gist** containing your timezone string (e.g., `America/Mexico_City`).

--

## 🚀 Plug-and-Play Deployment Guide

This repository is strictly configured to protect your sensitive data. Your API keys, WhatsApp sessions, and Google Credentials will **never** be committed to version control.

### 1. Clone the Repository
```bash
git clone [https://github.com/your-username/Heraldo.git](https://github.com/your-username/Heraldo.git)
cd Heraldo
```

### 2. Configure Environment
Create your environment file from the provided template:
```bash
cp .env.example .env
```
Open `.env` and fill in your details:
- **HERALDO_INTERNAL_TOKEN**: Generate a random secure string. Both services use this for internal auth.
- **MENSAJERO_PHONE**: Your target WhatsApp number (include country code, e.g., `+52...`).
- **GIST_TIMEZONE_URL**: The **Raw** URL to your timezone GitHub Gist.
- **GOOGLE_CREDENTIALS_JSON**: Paste your entire `credentials.json` content here inside single quotes: `'{"installed":...}'`.

### 3. Start the Engine
Navigate to the scripts folder and run the bootstrapper. Using `bash` directly bypasses any permission issues.
```bash
cd scripts
bash RISE.sh
```

### 4. Authenticate Services (One-Time Setup)

#### Step A: WhatsApp Authentication
1. Run `bash LOGS.sh` in the scripts folder.
2. Open WhatsApp on your phone, go to Linked Devices, and scan the ASCII QR code that appears in the terminal.

#### Step B: Google Authentication
1. Look for the **"🚨 GOOGLE AUTH REQUIRED"** link in the logs.
2. Copy the URL into your browser and authorize your account.
3. The browser will redirect to a "Site cannot be reached" page (localhost:8080). **Copy the code= value** from that URL's address bar.
4. Paste the code into your Heraldo Dashboard (`your-server-ip:8080`) to finalize the link.

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
