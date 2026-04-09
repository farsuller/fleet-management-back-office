FROM gradle:8.14.3-jdk21 AS build

WORKDIR /app

COPY . .

RUN chmod +x ./gradlew && ./gradlew :composeApp:wasmJsBrowserDistribution --no-daemon

FROM node:20-alpine AS runtime

WORKDIR /app

RUN npm install -g serve

COPY --from=build /app/composeApp/build/dist/wasmJs/productionExecutable ./public
COPY docker/start.sh ./start.sh

RUN chmod +x ./start.sh

ENV PORT=10000

EXPOSE 10000

CMD ["/app/start.sh"]