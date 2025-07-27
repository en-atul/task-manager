# Task Manager API

A robust RESTful API for managing projects and tasks with user authentication, role-based access control, and real-time collaboration features.

## 🚀 Features

### Authentication & Security
- **JWT-based Authentication** with access and refresh tokens
- **Role-based Access Control** (Owner, Editor, Viewer)
- **Session Management** with device tracking
- **Password Security** with Spring Security
- **Token Refresh** mechanism for seamless user experience

### Project Management
- **Create and manage projects** with detailed information
- **Team collaboration** with member management
- **Role-based permissions** within projects
- **Project ownership** and transfer capabilities

### Task Management
- **Create, update, and delete tasks** within projects
- **Task assignment** to team members
- **Due date management** with flexible scheduling
- **Task status tracking** (Pending, In Progress, Completed, Cancelled)
- **Task filtering** by project, assignee, and status

### User Management
- **User registration and authentication**
- **Profile management** with user details
- **Active session tracking** across devices
- **Role management** with different permission levels

## 🛠 Technology Stack

- **Backend Framework**: Spring Boot 3.5.3
- **Language**: Java 17
- **Database**: PostgreSQL
- **ORM**: Spring Data JPA with Hibernate
- **Security**: Spring Security with JWT
- **Documentation**: OpenAPI 3 (Swagger UI)
- **Build Tool**: Maven
- **Containerization**: Docker & Docker Compose
- **Validation**: Bean Validation (Jakarta)
- **Utilities**: Lombok, ModelMapper

## 📋 Prerequisites

- Java 17 or higher
- Maven 3.6+
- Docker and Docker Compose
- PostgreSQL (if running locally without Docker)

## 🚀 Quick Start

### Option 1: Using Docker Compose (Recommended)

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd task-manager
   ```

2. **Start the database**
   ```bash
   docker-compose up -d db
   ```

3. **Run the application**
   ```bash
   ./mvnw spring-boot:run
   ```

4. **Access the application**
   - API Base URL: `http://localhost:3001`
   - Swagger UI: `http://localhost:3001/swagger-ui.html`
   - Database: `localhost:5335`

### Option 2: Local Development

1. **Install PostgreSQL** and create a database named `task-manager`

2. **Update database configuration** in `src/main/resources/application.properties`:
   ```properties
   spring.datasource.url=jdbc:postgresql://localhost:5432/task-manager
   spring.datasource.username=your_username
   spring.datasource.password=your_password
   ```

3. **Run the application**
   ```bash
   ./mvnw spring-boot:run
   ```

## 📚 API Documentation

### Base URL
```
http://localhost:3001/api/v1
```

### Authentication Endpoints

> Check the [Swagger UI](http://localhost:3001/swagger-ui.html) for API documentation

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/auth/register` | Register a new user |
| POST | `/auth/login` | User login |
| POST | `/auth/logout` | User logout |
| POST | `/auth/refresh` | Refresh access token |
| GET | `/auth/me` | Get current user profile |
| GET | `/auth/sessions` | Get user active sessions |

### Project Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/project/create` | Create a new project |
| GET | `/project/user` | Get all user's projects |
| POST | `/project/{id}/members` | Add member to project |
| DELETE | `/project/{id}/members/{memberId}` | Remove member from project |
| PUT | `/project/{id}/members/{memberId}/role` | Change member role |
| DELETE | `/project/{id}` | Delete project |

### Task Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/task/create` | Create a new task |
| GET | `/task/{id}` | Get task by ID |
| GET | `/task/project/{projectId}` | Get all tasks in project |
| GET | `/task/project/{projectId}/user/{userId}` | Get tasks assigned to user |
| PUT | `/task/{id}` | Update task |
| PUT | `/task/{id}/assign/{assigneeId}` | Assign task to user |
| PUT | `/task/{id}/due-date` | Update task due date |
| DELETE | `/task/{id}` | Delete task |

## 🔐 Authentication

### Register a new user
```bash
curl -X POST http://localhost:3001/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "password123",
    "firstName": "John",
    "lastName": "Doe"
  }'
```

### Login
```bash
curl -X POST http://localhost:3001/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "password123"
  }'
```

### Using the API with authentication
```bash
curl -X GET http://localhost:3001/api/v1/project/user \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

## 📊 Data Models

### User
- `id`: Unique identifier
- `email`: User email (unique)
- `firstName`: First name
- `lastName`: Last name
- `password`: Encrypted password
- `roles`: User roles (ADMIN, USER)
- `createdAt`: Account creation timestamp

### Project
- `id`: Unique identifier
- `name`: Project name
- `createdBy`: Project creator (User)
- `members`: List of project members
- `tasks`: List of project tasks
- `createdAt`: Project creation timestamp

### Task
- `id`: Unique identifier
- `title`: Task title
- `description`: Task description
- `status`: Task status (PENDING, IN_PROGRESS, COMPLETED, CANCELLED)
- `dueDate`: Task due date
- `project`: Associated project
- `createdBy`: Task creator
- `assignee`: Task assignee
- `createdAt`: Task creation timestamp
- `updatedAt`: Last update timestamp

### ProjectMember
- `id`: Unique identifier
- `project`: Associated project
- `user`: Project member
- `role`: Member role (OWNER, EDITOR, VIEWER)
- `joinedAt`: Member join timestamp

## 🔒 Security & Permissions

### User Roles
- **ADMIN**: Full system access
- **USER**: Standard user access

### Project Roles
- **OWNER**: Full project control, can delete project, manage members
- **EDITOR**: Can create/edit tasks, manage members, cannot delete project
- **VIEWER**: Can view tasks and project details, cannot modify

### Permission Matrix

| Action | OWNER | EDITOR | VIEWER |
|--------|-------|--------|--------|
| View project | ✅ | ✅ | ✅ |
| Create tasks | ✅ | ✅ | ❌ |
| Edit tasks | ✅ | ✅ | ❌ |
| Delete tasks | ✅ | ✅ | ❌ |
| Add members | ✅ | ✅ | ❌ |
| Remove members | ✅ | ✅ | ❌ |
| Change member roles | ✅ | ✅ | ❌ |
| Delete project | ✅ | ❌ | ❌ |

## 🧪 Testing

### Run tests
```bash
./mvnw test
```

### Test coverage
```bash
./mvnw jacoco:report
```

## 📦 Build & Deployment

### Build JAR file
```bash
./mvnw clean package
```

### Run JAR file
```bash
java -jar target/task-manager-0.0.1-SNAPSHOT.jar
```

### Docker build
```bash
docker build -t task-manager .
docker run -p 3001:3001 task-manager
```

## 🔧 Configuration

### Environment Variables
- `SERVER_PORT`: Application port (default: 3001)
- `SPRING_DATASOURCE_URL`: Database connection URL
- `SPRING_DATASOURCE_USERNAME`: Database username
- `SPRING_DATASOURCE_PASSWORD`: Database password
- `AUTH_TOKEN_JWT_SECRET`: JWT secret key
- `AUTH_TOKEN_EXPIRATION_IN_MILS`: Access token expiration (default: 30 minutes)
- `AUTH_TOKEN_REFRESH_EXPIRATION_IN_MILS`: Refresh token expiration (default: 7 days)

### Database Configuration
The application uses PostgreSQL with the following default settings:
- **Host**: localhost
- **Port**: 5335 (Docker) / 5432 (Local)
- **Database**: task-manager
- **Username**: task-manager
- **Password**: password

## 📝 API Response Format

All API responses follow a consistent format:

```json
{
  "message": "Success message",
  "data": {
    // Response data
  },
  "timestamp": "2024-01-01T12:00:00Z"
}
```

### Error Response Format
```json
{
  "message": "Error message",
  "data": null,
  "timestamp": "2024-01-01T12:00:00Z"
}
```

## 📚 Learning Project

This is a personal learning project built while studying Spring Boot. It demonstrates:

- **Spring Boot fundamentals** and best practices
- **RESTful API design** with proper HTTP methods and status codes
- **Spring Security** implementation with JWT authentication
- **Spring Data JPA** for database operations
- **Role-based access control** and authorization
- **Docker** containerization
- **API documentation** with OpenAPI/Swagger
- **Clean architecture** with proper separation of concerns

Feel free to explore the codebase to learn from the implementation!




 
