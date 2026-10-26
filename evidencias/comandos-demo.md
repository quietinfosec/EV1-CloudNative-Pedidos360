# Comandos de demostración

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-21'
$env:PATH="$env:JAVA_HOME\bin;$env:PATH"
.\scripts\rabbitmq-up.ps1
.\scripts\rabbitmq-status.ps1
docker exec pedidos360-rabbitmq rabbitmqctl -q cluster_status
docker exec pedidos360-rabbitmq rabbitmqctl -q list_exchanges name type durable
docker exec pedidos360-rabbitmq rabbitmqctl -q list_bindings source_name destination_name routing_key
docker exec pedidos360-rabbitmq rabbitmqctl -q list_queues name messages consumers

cd backend\rabbitmq-admin-service; .\mvnw.cmd -B test
cd ..\pedidos-service; .\mvnw.cmd -B test
cd ..\productos-service; .\mvnw.cmd -B test
cd ..\..\frontend\pedidos360-web; npm test; npm run build
```

No ejecutar `docker compose down -v` durante la defensa: elimina el estado y las evidencias del broker.
