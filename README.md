# mini-erp

一个基于 Spring Boot 4 的迷你 ERP 后端项目，目前包含用户创建、登录认证、JWT 鉴权、MyBatis-Plus 数据访问和 PostgreSQL 数据库接入。

## 技术栈

- Java 21
- Spring Boot 4.0.5
- Spring MVC
- Spring Security
- MyBatis-Plus 3.5.15
- PostgreSQL
- Druid 连接池
- JJWT 0.13.0
- Lombok
- Log4j2

## 项目结构

```text
src/main/java/com/muzhi/minierp
├── annotation      # 自定义注解，例如 OpenApi
├── config          # Spring Security、MyBatis-Plus 配置
├── controller      # 接口层
├── entity          # 数据库实体
├── enums           # 枚举
├── exception       # 业务异常与全局异常处理
├── handler         # MyBatis-Plus 字段填充
├── mapper          # Mapper 接口
├── model           # 请求、响应、登录用户模型
├── security        # JWT 与认证授权相关逻辑
└── service         # 业务服务
```

## 环境要求

- JDK 21+
- PostgreSQL 13+
- Maven 3.9+，或直接使用项目内置的 Maven Wrapper

macOS / Linux 如果 `mvnw` 没有执行权限，可以先执行：

```bash
chmod +x mvnw
```

## 配置说明

主配置文件：

```text
src/main/resources/application.yaml
src/main/resources/application-dev.yaml
```

开发环境默认端口：

```yaml
server:
  port: 8081
```

数据库配置示例：

```yaml
spring:
  datasource:
    type: com.alibaba.druid.pool.DruidDataSource
    driver-class-name: org.postgresql.Driver
    url: jdbc:postgresql://127.0.0.1:5432/mini_erp
    username: your_username
    password: your_password
```

JWT 配置示例：

```yaml
security:
  jwt:
    issuer: https://www.muzhi.store
    secret: your-strong-jwt-secret
    expire-minutes: 60
```

生产环境建议通过环境变量或部署平台的 Secret 配置覆盖敏感信息，例如：

```bash
export SPRING_DATASOURCE_URL=jdbc:postgresql://127.0.0.1:5432/mini_erp
export SPRING_DATASOURCE_USERNAME=your_username
export SPRING_DATASOURCE_PASSWORD=your_password
export SECURITY_JWT_SECRET=your-strong-jwt-secret
```

## 数据库说明

当前项目使用 PostgreSQL，并在 MyBatis-Plus 分页插件中配置了数据库类型：

```java
new PaginationInnerInterceptor(DbType.POSTGRE_SQL)
```

逻辑删除字段使用 PostgreSQL `boolean` 类型时，删除值应使用 `true` / `false`：

```yaml
mybatis-plus:
  global-config:
    db-config:
      logic-delete-value: true
      logic-not-delete-value: false
```

注意 PostgreSQL 不会自动把 `boolean` 与 `0` / `1` 互相比较，SQL 条件应写成：

```sql
where is_deleted = false
```

而不是：

```sql
where is_deleted = 0
```

## 启动项目

使用 Maven Wrapper 启动：

```bash
./mvnw spring-boot:run
```

启动成功后，服务地址为：

```text
http://localhost:8081
```

## 构建与测试

运行测试：

```bash
./mvnw test
```

打包：

```bash
./mvnw clean package
```

运行构建产物：

```bash
java -jar target/mini-erp-0.0.1-SNAPSHOT.jar
```

## 接口示例

### 创建用户

```http
POST /user/create
Content-Type: application/json
Authorization: Bearer <token>
```

```json
{
  "username": "admin",
  "password": "123456",
  "realName": "管理员",
  "mobile": "13800000000"
}
```

### 用户登录

登录接口标记了 `@OpenApi`，不需要携带 JWT。

```http
POST /auth/login
Content-Type: application/json
```

```json
{
  "username": "admin",
  "password": "123456"
}
```

响应中的 `token` 用于访问受保护接口：

```http
Authorization: Bearer <token>
```

### 获取当前用户

```http
GET /auth/getCurrentUser
Authorization: Bearer <token>
```

## 开发约定

- 公开接口使用 `@OpenApi` 标记。
- 受保护接口需要在请求头中携带 `Authorization: Bearer <token>`。
- 用户密码入库前会经过用户名加盐和 `PasswordEncoder` 编码。
- `gmtCreate`、`gmtModified`、`createUser`、`updateUser` 由 MyBatis-Plus 自动填充。
- 逻辑删除字段 `is_deleted` 建议在 PostgreSQL 中使用 `boolean` 类型，并在 Java 实体中使用 `boolean` / `Boolean`。

