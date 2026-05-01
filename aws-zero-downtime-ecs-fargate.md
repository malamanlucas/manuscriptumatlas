# Zero-Downtime Deployment en AWS ECS/Fargate

Para diseñar un proceso de despliegue sin tiempo de inactividad en una plataforma multi-servicio corriendo en AWS ECS/Fargate, aplicaría las siguientes estrategias:

## 1. Blue/Green Deployment con AWS CodeDeploy

Levantaría la nueva versión del servicio en paralelo (entorno *green*) mientras el entorno anterior (*blue*) continúa recibiendo tráfico. El Application Load Balancer (ALB) realiza el cambio de tráfico de forma gradual — por ejemplo, 10%, 50%, 100% — únicamente después de que los health checks de la nueva versión pasen correctamente. En caso de fallo, el rollback es automático sin intervención manual.

## 2. Rolling Update con configuración segura

Configuraría el deployment de la task definition de ECS con:
- `minimumHealthyPercent = 100%` — ECS no termina el contenedor antiguo hasta que el nuevo esté saludable y sirviendo tráfico
- `maximumPercent = 200%` — permite que ambas versiones corran en paralelo durante la transición

Esto garantiza que en ningún momento el servicio quede sin instancias disponibles.

## 3. Health Checks en dos capas

- **Container health check** (ECS): verifica que el proceso interno del contenedor está vivo
- **ALB health check**: verifica que el endpoint `/health` responde con HTTP 200 antes de enrutar tráfico real al nuevo contenedor

El tráfico solo se transfiere después de que ambas capas confirman que el servicio está listo.

## 4. ECS Deployment Circuit Breaker

Habilitaría el circuit breaker nativo de ECS. Si el nuevo despliegue no supera los health checks dentro del umbral configurado, ECS revierte automáticamente a la versión anterior estable, eliminando la necesidad de monitoreo manual durante el deploy.

## 5. Alta Disponibilidad Multi-AZ

Distribuiría las tasks de ECS/Fargate en al menos dos Availability Zones. Esto asegura que, si una zona falla durante el despliegue, el servicio continúa disponible en las demás zonas sin interrupción para el usuario final.
