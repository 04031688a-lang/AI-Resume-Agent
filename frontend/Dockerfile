# ============================================================
# AI Resume Agent 前端镜像（Nginx 托管 + API 反向代理）
# 构建：docker build -t resume-agent-frontend ./frontend
# ============================================================
FROM node:22-alpine AS build
WORKDIR /app

COPY package*.json ./
# 国内网络可追加：--registry=https://registry.npmmirror.com
RUN npm install

COPY . .
RUN npm run build

FROM nginx:alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
