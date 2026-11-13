# Stage 1: Build static assets using Node.js
FROM node:24-alpine AS builder
WORKDIR /app

COPY package*.json ./
RUN npm ci

COPY . .
RUN npm run build

# Stage 2: Serve with Nginx
FROM nginx:alpine-slim
WORKDIR /usr/share/nginx/html

# Copy built assets
COPY --from=builder /app/dist .

# Copy custom nginx config
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
