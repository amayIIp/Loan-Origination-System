# ---------------------------------------------------------
# STAGE 1: BUILD (Compiles the Angular TypeScript to static HTML/JS)
# ---------------------------------------------------------
# Use a Node.js image to install dependencies and build the Angular app.
FROM node:18-alpine AS build

# Set the working directory inside the container.
WORKDIR /app

# Copy package.json files to install exact dependency versions.
COPY package*.json ./

# Install dependencies using 'npm ci' (clean install), which strictly follows package-lock.json.
RUN npm ci

# Copy the rest of the Angular source code.
COPY . .

# Compile the Angular app into highly optimized static files for production.
RUN npm run build --configuration=production

# ---------------------------------------------------------
# STAGE 2: RUNTIME (Serves the files using Nginx)
# ---------------------------------------------------------
# Use Nginx, a high-performance web server, to serve our static files.
FROM nginx:alpine

# Remove the default Nginx configuration file.
RUN rm /etc/nginx/conf.d/default.conf

# Copy our custom Nginx configuration file into the container.
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Copy the compiled static Angular files from the 'build' stage into Nginx's hosting folder.
# NOTE: In Angular 17, the build output is usually in dist/[project-name]/browser. 
# We assume 'frontend/browser' here. Adjust if your angular.json specifies a different output path.
COPY --from=build /app/dist/frontend/browser /usr/share/nginx/html

# Expose port 80 for normal web traffic.
EXPOSE 80

# Start Nginx in the foreground so the Docker container doesn't exit immediately.
CMD ["nginx", "-g", "daemon off;"]
