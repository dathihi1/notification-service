# Notification Service

A multi-channel notification service built with Spring Boot 3.x and Java 17.

## 🚀 Features

- **Multi-Channel Support**: EMAIL, SMS, PUSH, WEBHOOK, IN_APP
- **Priority Queue Processing**: URGENT > HIGH > NORMAL > LOW
- **Retry Logic**: Exponential backoff with max retry limits
- **Redis Integration**: High-performance queue management
- **Database Persistence**: Full audit trail and history
- **Template System**: Reusable notification templates
- **Real-time Monitoring**: Queue statistics and health checks
- **Scheduled Notifications**: Future delivery support
- **Automatic Cleanup**: Old notification cleanup

## 📁 Architecture

```
┌─────────────────────────────────────────┐
│              REST API             │
│    ┌───────────────────────────┐    │
│    │   NotificationController    │
│    │   NotificationService      │
│    │   RedisEmailQueueService │
│    │   NotificationProcessor   │
│    │   NotificationWorker     │
│    └───────────────────────────┘    │
│                                     │
│    ┌───────────────────────────┐    │
│    │    Strategy Pattern        │
│    │  NotificationSender Interface │
│    │  ├─ EmailNotificationSender │
│    │  ├─ SmsNotificationSender  │
│    │  ├─ PushNotificationSender │
│    │  └─ WebhookNotificationSender │
│    └───────────────────────────┘    │
│                                     │
│    ┌───────────────────────────┐    │
│    │         Data Layer         │
│    │  ├─ NotificationRepository  │
│    │  └─ EmailMessageRepository │
│    └───────────────────────────┘    │
│                                     │
│    ┌───────────────────────────┐    │
│    │         Queue Layer        │
│    │  └─ RedisEmailQueueService │
│    └───────────────────────────┘    │
└─────────────────────────────────────────┘
```

## 🛠️ Technologies

- **Java 17** - Modern Java features and performance
- **Spring Boot 3.x** - Latest Spring Boot framework
- **Spring Data JPA** - Database operations and queries
- **Spring Data Redis** - Queue management and caching
- **Jackson** - JSON serialization/deserialization
- **Lombok** - Reduced boilerplate code
- **MySQL Connector** - Database connectivity
- **Maven** - Build and dependency management

## 📊 Database Schema

### notifications table
```sql
CREATE TABLE notifications (
    id VARCHAR(36) PRIMARY KEY,
    recipient_type VARCHAR(20) NOT NULL,
    recipient VARCHAR(500) NOT NULL,
    channel_type VARCHAR(50) NOT NULL,
    title VARCHAR(200) NOT NULL,
    content TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    priority VARCHAR(20) NOT NULL DEFAULT 'NORMAL',
    template_id VARCHAR(100),
    metadata TEXT,
    retry_count INT DEFAULT 0,
    max_retries INT DEFAULT 3,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    sent_at TIMESTAMP NULL,
    error VARCHAR(1000)
);

-- Indexes for performance --
CREATE INDEX idx_notifications_status ON notifications(status);
CREATE INDEX idx_notifications_recipient ON notifications(recipient);
CREATE INDEX idx_notifications_channel ON notifications(channel_type);
CREATE INDEX idx_notifications_created_at ON notifications(created_at);
CREATE INDEX idx_notifications_priority_status ON notifications(priority, status);
```

## 🔧 Configuration

### Application Properties
```properties
# Server Configuration
server.port=8080

# Database Configuration
spring.datasource.url=jdbc:mysql://localhost:3306/notification_service
spring.datasource.username=root
spring.datasource.password=password
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
spring.datasource.hibernate.ddl-auto=update
spring.datasource.show-sql=true

# Redis Configuration
spring.redis.host=localhost
spring.redis.port=6379
spring.redis.timeout=2000ms
spring.redis.lettuce.pool.max-active=8
spring.redis.lettuce.pool.max-idle=8

# Email Configuration
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=${EMAIL_USERNAME}
spring.mail.password=${EMAIL_PASSWORD}
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true

# Thread Pool Configuration
notification.thread-pool.core-size=3
notification.thread-pool.max-size=10
notification.thread-pool.queue-capacity=50
notification.thread-pool.keep-alive=60s

# Processing Configuration
notification.batch-size=50
notification.cleanup.interval=3600000
notification.cleanup.retention-days=7
```

## 🚀 Quick Start

### Prerequisites
- Java 17+
- Maven 3.6+
- MySQL 8.0+
- Redis 6.0+

### Build & Run
```bash
# Clone repository
git clone <repository-url>
cd notification-service

# Configure environment
cp src/main/resources/application-example.properties src/main/resources/application.properties

# Build project
mvn clean package

# Run application
java -jar target/notification-service-0.0.1-SNAPSHOT.jar

# With Docker
docker build -t notification-service .
docker run -p 8080:8080 notification-service
```

## 📚 API Endpoints

### Send Notification
```http
POST /api/notifications/send
Content-Type: application/json

{
  "recipientType": "EMAIL",
  "recipient": "user@example.com",
  "channelType": "EMAIL",
  "title": "Welcome!",
  "content": "Welcome to our service",
  "priority": "HIGH",
  "templateId": "welcome-template",
  "metadata": {
    "userName": "John Doe",
    "productCode": "PROD_001"
  }
}
```

### Get Notifications
```http
GET /api/notifications?status=PENDING&limit=10
GET /api/notifications/recipient/user@example.com?limit=20
GET /api/notifications/stats
```

### Queue Management
```http
GET /api/notifications/queue/stats
GET /api/notifications/queue/retry/{id}
POST /api/notifications/queue/scheduled
```

## 📈 Monitoring

### Health Check
```http
GET /api/notifications/health
```

### Queue Statistics
```json
{
  "urgent": 5,
  "high": 12,
  "normal": 23,
  "low": 3,
  "processing": 2,
  "failed": 1,
  "completed": 45,
  "totalSent": 45,
  "successRate": "95.24%"
}
```

## 🧪 Testing

### Send Test Email
```bash
curl -X POST http://localhost:8080/api/notifications/send \
  -H "Content-Type: application/json" \
  -d '{
    "recipientType": "EMAIL",
    "recipient": "test@example.com",
    "channelType": "EMAIL",
    "title": "Test Email",
    "content": "This is a test email",
    "priority": "NORMAL"
  }'
```

### Send Batch SMS
```bash
curl -X POST http://localhost:8080/api/notifications/send \
  -H "Content-Type: application/json" \
  -d '[
    {
      "recipientType": "PHONE",
      "recipient": "+84901234567",
      "channelType": "SMS",
      "title": "Test SMS 1",
      "content": "This is test SMS 1",
      "priority": "HIGH"
    },
    {
      "recipientType": "PHONE",
      "recipient": "+84901234568",
      "channelType": "SMS",
      "title": "Test SMS 2",
      "content": "This is test SMS 2",
      "priority": "NORMAL"
    }
  ]'
```

## 🔄 Operations

### Schedule Notification
```bash
curl -X POST http://localhost:8080/api/notifications/scheduled \
  -H "Content-Type: application/json" \
  -d '{
    "recipientType": "EMAIL",
    "recipient": "user@example.com",
    "channelType": "EMAIL",
    "title": "Scheduled Welcome Email",
    "content": "Welcome to our service!",
    "priority": "HIGH",
    "scheduledFor": "2025-01-01T10:00:00"
  }'
```

### Retry Failed Notification
```bash
curl -X POST http://localhost:8080/api/notifications/queue/retry/123e4567-e89b-12d3-a456-4266-555b555b