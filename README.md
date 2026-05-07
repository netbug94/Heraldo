# Heraldo: Personal Automation Engine ⚙️

A rock-solid, containerized microservice architecture for personal task automation.
Composed of a Kotlin backend (`heraldo-gestor`) that monitors Google APIs, and a Node.js/Puppeteer microservice (`heraldo-mensajero`) that dispatches real-time alerts directly to WhatsApp.

---

## 📋 Prerequisites
- **Docker** and **Docker Compose** installed on your server.
- A **Google Cloud Console** project with the Google Tasks and Calendar APIs enabled, and an OAuth 2.0 Client ID (Desktop App) downloaded as `credentials.json`.
- A free **GitHub Gist** containing your timezone string (e.g., `America/Hermosillo`).

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
- `API_KEY` and `HERALDO_INTERNAL_TOKEN`: Generate a random secure string (e.g., `openssl rand -hex 32`) and paste the exact same string in both fields.
- `MENSAJERO_PHONE`: Your target WhatsApp number (include country code, e.g., `+52...`).
- `GIST_TIMEZONE_URL`: The **Raw** URL to your timezone GitHub Gist (make sure the URL ends with `/raw`).

### 3. Add Google Credentials
Drop your Google OAuth credentials into the Gestor's config folder. (Docker will automatically mount this into the container).
```bash
mkdir -p heraldo-gestor/config
# Upload your credentials.json into the heraldo-gestor/config/ folder
```

### 4. Start the Engine
Boot up the Docker stack in the background:
```bash
docker compose up -d --build
```

### 5. Authenticate Services (One-Time Setup)

#### Step A: WhatsApp Authentication
1. Open `http://<your-server-ip>:3000` in your web browser.
2. Click **Start WhatsApp Engine**.
3. Open WhatsApp on your phone, go to Linked Devices, and scan the QR code that appears on the screen.

#### Step B: Google Authentication
Because the Google OAuth flow redirects to `localhost:8888` by default, authenticating on a remote headless server requires a quick SSH tunnel trick.

1. On your **local laptop**, create an SSH tunnel to your server:
   ```bash
   ssh -L 8888:localhost:8888 your_user@your_server_ip
   ```
2. On your server, check the Gestor daemon logs for the Google Auth link:
   ```bash
   docker compose logs -f gestor
   ```
3. Copy the Google authentication URL from the logs and open it in your **local laptop's web browser**.
4. Log into your Google account and grant permissions.
5. Google will redirect your browser to `http://localhost:8888`. Because of the SSH tunnel you created in Step 1, this traffic will securely pass to your remote server, and Heraldo Gestor will successfully capture the auth tokens!

---

## 🛠️ Useful Operations

**Check System Logs:**
```bash
docker compose logs -f                # View all logs
docker compose logs -f mensajero      # View only WhatsApp engine logs
docker compose logs -f gestor         # View only Kotlin backend logs
```

**Restart After Updating `.env` or Code:**
```bash
git pull
docker compose up -d --build
```

**Completely Reset WhatsApp Session:**
If your WhatsApp disconnects permanently, just go to `http://<your-server-ip>:3000`, click **Disconnect**, and scan a new QR code. There is no need to restart Docker.

**Completely Reset Google Session:**
```bash
# Delete the stored tokens
rm -rf heraldo-gestor/tokens/*
# Restart the gestor to trigger a new auth flow
docker compose restart gestor
```
