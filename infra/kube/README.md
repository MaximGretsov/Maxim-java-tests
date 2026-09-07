# Развёртывание NBank в Kubernetes с использованием Helm

## Используемые инструменты

- Minikube
- Kubernetes
- Helm
- Docker

## Развёрнутые сервисы

| Сервис | Тип Service | Внутренний порт | NodePort |
|---|---|---:|---:|
| backend | NodePort | 4111 | 30111 |
| frontend | NodePort | 80 | 31310 |
| postgres | ClusterIP | 5432 | — |
| selenoid | NodePort | 4444 | 30444 |
| selenoid-ui | NodePort | 8080 | 31403 |

PostgreSQL используется backend-сервисом внутри Kubernetes-кластера и наружу через NodePort не публикуется.

## Запуск Minikube

```bash
minikube start --driver=docker
```

Проверка состояния кластера:

```bash
minikube status
kubectl get nodes
```

## Проверка Helm Chart

```bash
helm lint ./infra/kube/nbank-chart
helm template nbank ./infra/kube/nbank-chart
```

Результат проверки:

```text
1 chart(s) linted, 0 chart(s) failed
```

## Установка приложения через Helm

Первичная установка:

```bash
helm install nbank ./infra/kube/nbank-chart
```

Обновление существующего релиза:

```bash
helm upgrade nbank ./infra/kube/nbank-chart
```

Проверка:

```bash
helm list
```

Результат:

```text
NAME    NAMESPACE   REVISION   STATUS     CHART         APP VERSION
nbank   default     4          deployed   nbank-0.0.1   1.0.0
```

## Состояние Pod

Команда:

```bash
kubectl get pods
```

Результат:

```text
NAME                           READY   STATUS    RESTARTS
backend-d56dc8fbf-qxstd        1/1     Running   0
frontend-5bd596dd46-kzpqk      1/1     Running   0
postgres-7bf794b84f-x9tzw      1/1     Running   0
selenoid-5f854fb56d-59c9f      1/1     Running   0
selenoid-ui-744c5d768b-pvb8x   1/1     Running   0
```

Все сервисы успешно запущены.

## Kubernetes Services

Команда:

```bash
kubectl get svc
```

Результат:

```text
NAME          TYPE        PORT(S)
backend       NodePort    4111:30111/TCP
frontend      NodePort    80:31310/TCP
postgres      ClusterIP   5432/TCP
selenoid      NodePort    4444:30444/TCP
selenoid-ui   NodePort    8080:31403/TCP
```

## ConfigMap

Используются следующие ConfigMap:

```text
frontend-nginx-config
postgres-init
selenoid-config
```

Проверка:

```bash
kubectl get configmap
```

### frontend-nginx-config

Используется для конфигурации nginx во frontend.

API-запросы проксируются в Kubernetes Service `backend`:

```nginx
location /api/ {
    proxy_pass http://backend:4111;
}
```

Проверка:

```bash
kubectl describe configmap frontend-nginx-config
```

### selenoid-config

Содержит конфигурацию браузеров для Selenoid.

Проверка:

```bash
kubectl describe configmap selenoid-config
```

### postgres-init

Содержит SQL-скрипт первоначальной инициализации PostgreSQL.

## Secret

Для хранения данных подключения к PostgreSQL используется Kubernetes Secret:

```text
postgres-secret
```

Тип Secret:

```text
Opaque
```

Проверка:

```bash
kubectl get secret
```

Значения Secret в README не выводятся.

## PostgreSQL

PostgreSQL развёрнут отдельным Deployment и доступен внутри Kubernetes-кластера по адресу:

```text
postgres:5432
```

Backend подключается к базе данных по адресу:

```text
jdbc:postgresql://postgres:5432/nbank
```

Проверка созданных таблиц:

```bash
kubectl exec deployment/postgres -- psql -U postgres -d nbank -c "\dt"
```

## Логи сервисов

Просмотр логов:

```bash
kubectl logs deployment/backend --tail=30
kubectl logs deployment/frontend --tail=30
kubectl logs deployment/postgres --tail=30
kubectl logs deployment/selenoid --tail=30
kubectl logs deployment/selenoid-ui --tail=30
```

Примеры успешной работы сервисов.

Backend:

```text
Login successful for user 'admin' with role 'ADMIN'
```

PostgreSQL:

```text
database system is ready to accept connections
```

Selenoid:

```text
SESSION_CREATED
```

Selenoid UI:

```text
Listening on :8080
```

## Port Forward

Для локального доступа к backend:

```bash
kubectl port-forward svc/backend 4111:4111
```

Проверка health endpoint:

```bash
curl http://localhost:4111/actuator/health
```

Результат:

```json
{"status":"UP","groups":["liveness","readiness"]}
```

Для frontend:

```bash
kubectl port-forward --address 0.0.0.0 svc/frontend 3000:80
```

Для Selenoid:

```bash
kubectl port-forward svc/selenoid 4444:4444
```

Для PostgreSQL:

```bash
kubectl port-forward svc/postgres 5433:5432
```

## Масштабирование Deployment

Backend был масштабирован с одной до двух реплик:

```bash
kubectl scale deployment backend --replicas=2
```

Проверка Pod:

```bash
kubectl get pods -l app=backend
```

Результат:

```text
NAME                      READY   STATUS    RESTARTS
backend-d56dc8fbf-g6qnp   1/1     Running   0
backend-d56dc8fbf-qxstd   1/1     Running   0
```

Проверка Deployment:

```bash
kubectl get deployment backend
```

Результат:

```text
NAME      READY   UP-TO-DATE   AVAILABLE
backend   2/2     2            2
```

После проверки количество реплик было возвращено к исходному:

```bash
kubectl scale deployment backend --replicas=1
```

## Структура Helm Chart

```text
infra/kube/nbank-chart/
├── Chart.yaml
├── values.yaml
├── files/
│   ├── 01-init-db.sql
│   └── browsers.json
└── templates/
    ├── backend.yaml
    ├── frontend-config.yaml
    ├── frontend.yaml
    ├── postgres-configmap.yaml
    ├── postgres-secret.yaml
    ├── postgres.yaml
    ├── selenoid-config.yaml
    ├── selenoid-ui.yaml
    └── selenoid.yaml
```

## Результат

Приложение NBank развёрнуто в Minikube с использованием Helm.

В Kubernetes запущены:

- backend;
- frontend;
- PostgreSQL;
- Selenoid;
- Selenoid UI.

Для конфигурации используются ConfigMap и Secret. Проверены состояние Pod и Service, логи сервисов, port-forward и масштабирование Deployment.
