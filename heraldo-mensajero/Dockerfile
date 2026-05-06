FROM node:20-slim

# Install latest chromium and dependencies for Puppeteer
RUN apt-get update \
    && apt-get install -y chromium dumb-init \
    fonts-ipafont-gothic fonts-wqy-zenhei fonts-thai-tlwg fonts-kacst fonts-freefont-ttf libxss1 \
    --no-install-recommends \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Tell Puppeteer to skip downloading Chromium and use the system one
ENV PUPPETEER_SKIP_CHROMIUM_DOWNLOAD=true
ENV PUPPETEER_EXECUTABLE_PATH=/usr/bin/chromium

COPY package.json package-lock.json ./
RUN npm ci

COPY tsconfig.json ./
COPY src ./src

RUN npm run build

EXPOSE 3000

ENTRYPOINT ["/usr/bin/dumb-init", "--"]
CMD ["npm", "start"]
