# Task Manager Chart

This Helm chart deploys the Task Manager application with configurable database options for different environments.

## Features

- **Dual Deployment Strategy**: Supports both internal PostgreSQL database and external database configurations
- **Environment-Specific Configurations**: Separate values files for test and production environments
- **Helm Hooks for Dependency Management**: Proper deployment ordering using Helm hooks instead of init containers
- **Database Readiness Check**: Automatic database readiness verification before app deployment
- **Flexible Configuration**: Easy switching between internal and external database setups

## Architecture

### Test Environment
- Uses internal PostgreSQL database deployment
- Helm hooks ensure proper deployment order: ConfigMap/Secrets → Database → Readiness Check → App
- All components deployed within the same namespace

### Production Environment
- Uses external database (configure your production database URL)
- No internal database deployment
- App connects directly to external database

## Helm Hooks Implementation

This chart uses Helm hooks to ensure proper deployment ordering:

### Hook Weights (Lower numbers execute first):
- **-7**: Database Secrets
- **-6**: Database ConfigMap  
- **-5**: Database PVC
- **-4**: Database Deployment
- **-3**: Database Service
- **4**: Database Readiness Check Job
- **5**: App Deployment
- **6**: App Service

### Hook Types:
- **pre-install/pre-upgrade**: Database components created before app
- **post-install/post-upgrade**: App components created after database is ready
- **hook-delete-policy**: Readiness check job is deleted after successful completion

## Quick Start

### Test Environment (with internal database)

```bash
# Install the chart for test environment
helm install taskmanager-test ./taskmanager-chart -f values-test.yaml

# Upgrade existing installation
helm upgrade taskmanager-test ./taskmanager-chart -f values-test.yaml

# Uninstall
helm uninstall taskmanager-test
```

### Production Environment (with external database)

```bash
# First, update the production values file with your actual database credentials
# Edit values-prod.yaml and set your external database URL and credentials

# Install the chart for production environment
helm install taskmanager-prod ./taskmanager-chart -f values-prod.yaml

# Upgrade existing installation
helm upgrade taskmanager-prod ./taskmanager-chart -f values-prod.yaml

# Uninstall
helm uninstall taskmanager-prod
```

## Configuration

### Database Configuration

#### Internal Database (Test Environment)
```yaml
database:
  enabled: true
  image:
    repository: postgres
    tag: "latest"
  name: "task-manager-test"
  username: "root"
  password: "root"
  persistence:
    size: "1Gi"
```

#### External Database (Production Environment)
```yaml
database:
  enabled: false

externalDatabase:
  enabled: true
  url: "jdbc:postgresql://your-production-db-host:5432/task-manager-prod"
  username: "your-db-username"
  password: "your-db-password"
```

### App Configuration

```yaml
image:
  repository: taskmanager-app
  tag: "latest"
  pullPolicy: IfNotPresent

service:
  type: NodePort  # or LoadBalancer for production
  port: 8080

resources:
  limits:
    cpu: 1000m
    memory: 1Gi
  requests:
    cpu: 500m
    memory: 512Mi
```

## Components Deployed

### When `database.enabled: true` (Test Environment)
- **Database ConfigMap & Secrets**: Created first (hook-weight: -7, -6)
- **PostgreSQL PVC**: Persistent volume claim (hook-weight: -5)
- **PostgreSQL Deployment**: Database deployment (hook-weight: -4)
- **Database Service**: Headless service for database communication (hook-weight: -3)
- **Database Readiness Job**: Verifies database is ready (hook-weight: 4)
- **App Deployment**: Application deployment (hook-weight: 5)
- **App Service**: Service exposing the application (hook-weight: 6)

### When `database.enabled: false` (Production Environment)
- **App Deployment**: Application connecting to external database
- **App Service**: Service exposing the application

## Environment Variables

### Internal Database Mode
- `DB_HOST`: Database host (defaults to `<release-name>-postgres`)
- `DB_NAME`: Database name
- `DB_SSL_MODE`: SSL mode configuration
- `DB_CHANNEL_BINDING`: Channel binding configuration
- `SPRING_DATASOURCE_USERNAME`: Database username
- `SPRING_DATASOURCE_PASSWORD`: Database password

### External Database Mode
- `SPRING_DATASOURCE_URL`: External database connection URL
- `SPRING_DATASOURCE_USERNAME`: External database username
- `SPRING_DATASOURCE_PASSWORD`: External database password

## Health Checks

The application includes configurable health checks:

```yaml
livenessProbe:
  httpGet:
    path: /actuator/health
    port: http
  initialDelaySeconds: 60
  periodSeconds: 10

readinessProbe:
  httpGet:
    path: /actuator/health
    port: http
  initialDelaySeconds: 30
  periodSeconds: 5
```

## Scaling

### Manual Scaling
Set the `replicaCount` in your values file:

```yaml
replicaCount: 3
```

### Auto Scaling (Production)
Enable horizontal pod autoscaling:

```yaml
autoscaling:
  enabled: true
  minReplicas: 2
  maxReplicas: 10
  targetCPUUtilizationPercentage: 70
```

## Security

### Database Credentials
Database credentials are stored as Kubernetes secrets and mounted securely into the application pods.

### Image Pull Secrets
If using a private registry, configure image pull secrets:

```yaml
imagePullSecrets:
  - name: my-registry-secret
```

## Troubleshooting

### Check Pod Status
```bash
kubectl get pods -l app.kubernetes.io/name=helm-chart
```

### Check Database Connectivity
```bash
# For internal database
kubectl exec -it <app-pod-name> -- pg_isready -h <release-name>-postgres -p 5432

# Check logs
kubectl logs <app-pod-name>
```

### Database Initialization Issues
If the database is not initializing properly, check:
1. PVC storage class availability
2. Database image pull permissions
3. Resource limits and requests

## Values Files

- `values.yaml`: Default values
- `values-test.yaml`: Test environment configuration
- `values-prod.yaml`: Production environment configuration

## Customization

You can override any value by creating a custom values file or using the `--set` flag:

```bash
helm install my-release ./taskmanager-chart \
  --set database.enabled=false \
  --set externalDatabase.url="jdbc:postgresql://my-db:5432/mydb"

## Preview Generated YAML

Before installing the chart, you can preview the generated Kubernetes manifests using `helm template`:

### Preview All Resources
```bash
# Preview all resources for test environment
helm template test-release ./taskmanager-chart -f values-test.yaml

# Preview all resources for production environment
helm template prod-release ./taskmanager-chart -f values-prod.yaml
```

### Preview Specific Components
```bash
# Preview app deployment only
helm template test-release ./taskmanager-chart -f values-test.yaml --show-only templates/app-deployment.yaml

# Preview database deployment only
helm template test-release ./taskmanager-chart -f values-test.yaml --show-only templates/db-deployment.yaml

# Preview database readiness job
helm template test-release ./taskmanager-chart -f values-test.yaml --show-only templates/db-ready-job.yaml

# Preview app service
helm template test-release ./taskmanager-chart -f values-test.yaml --show-only templates/app-service.yaml

# Preview database configmap
helm template test-release ./taskmanager-chart -f values-test.yaml --show-only templates/db-configmap.yaml

# Preview database secrets
helm template test-release ./taskmanager-chart -f values-test.yaml --show-only templates/db-secrets.yaml
```

### Preview with Custom Values
```bash
# Preview with custom values
helm template test-release ./taskmanager-chart \
  --set database.enabled=true \
  --set replicaCount=3 \
  --show-only templates/app-deployment.yaml

# Preview production with external database
helm template prod-release ./taskmanager-chart \
  --set database.enabled=false \
  --set externalDatabase.url="jdbc:postgresql://my-prod-db:5432/mydb" \
  --show-only templates/app-deployment.yaml
```

### Save Generated YAML to File
```bash
# Save all resources to a file
helm template test-release ./taskmanager-chart -f values-test.yaml > generated-manifests.yaml

# Save specific component to a file
helm template test-release ./taskmanager-chart -f values-test.yaml \
  --show-only templates/app-deployment.yaml > app-deployment.yaml
``` 