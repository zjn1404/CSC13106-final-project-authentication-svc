# Authentication Service Integration Guide

## Service Configuration
| Property | Value |
|----------|-------|
| **Base URL** | `http://localhost:8081` |
| **API Prefix** | `/api/v1/auth` |
| **Token Type** | Bearer JWT |
| **Access Token Expiration** | 1 hour (3600000ms) |
| **Refresh Token Expiration** | 24 hours (86400000ms) |

## Quick Reference
| Action | Method | Endpoint | Auth |
|--------|--------|----------|------|
| Register | POST | `/api/v1/auth/register` | ❌ |
| Login | POST | `/api/v1/auth/login` | ❌ |
| Refresh Token | POST | `/api/v1/auth/refresh-token` | ❌ |
| Get Profile | GET | `/api/v1/auth/me` | ✅ |
| Change Password | POST | `/api/v1/auth/change-password` | ✅ |
| Logout | POST | `/api/v1/auth/logout` | ✅ |

## Public Endpoints

### Register User
**POST** `/api/v1/auth/register`
```json
{ "email": "user@example.com", "password": "password123", "firstName": "John", "lastName": "Doe" }
```

### Login
**POST** `/api/v1/auth/login`
```json
{ "email": "user@example.com", "password": "password123" }
```

### Refresh Token
**POST** `/api/v1/auth/refresh-token`
```json
{ "refreshToken": "eyJhbGciOiJIUzUxMiJ9..." }
```

### Auth Response Format (Register/Login/Refresh)
```json
{
  "success": true,
  "message": "...",
  "data": {
    "accessToken": "eyJhbGciOiJIUzUxMiJ9...",
    "refreshToken": "eyJhbGciOiJIUzUxMiJ9...",
    "tokenType": "Bearer",
    "expiresIn": 3600000,
    "user": { "id": "...", "email": "...", "firstName": "...", "lastName": "..." }
  },
  "timestamp": "2025-12-13T10:00:00.000Z"
}
```

## Protected Endpoints
> **Header Required:** `Authorization: Bearer <accessToken>`

### Get Profile
**GET** `/api/v1/auth/me`
```json
{
  "success": true,
  "data": {
    "id": "userId123", "email": "user@example.com",
    "firstName": "John", "lastName": "Doe", "enabled": true,
    "createdAt": "2025-12-13T10:00:00.000Z", "updatedAt": "2025-12-13T10:00:00.000Z"
  }
}
```

### Change Password
**POST** `/api/v1/auth/change-password`
```json
{ "currentPassword": "password123", "newPassword": "newPassword456", "confirmPassword": "newPassword456" }
```

### Logout
**POST** `/api/v1/auth/logout`
```json
{ "success": true, "message": "Logged out successfully" }
```

## Error Responses
| HTTP Status | Description |
|-------------|-------------|
| 400 | Bad Request (validation errors, email exists) |
| 401 | Unauthorized (invalid credentials, expired token) |
| 403 | Forbidden (blacklisted token) |
| 404 | Not Found (user not found) |

```json
{ "success": false, "message": "Error description", "timestamp": "..." }
```

## JWT Token
- **Algorithm:** HS512
- **Claims:** `sub` (email), `iat` (issued at), `exp` (expiration)
- **Validation:** Signature, Expiration, Blacklist check

## API Gateway Integration

### Route Configuration (Spring Cloud Gateway)
```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: auth-public
          uri: http://localhost:8081
          predicates:
            - Path=/api/v1/auth/register,/api/v1/auth/login,/api/v1/auth/refresh-token
        - id: auth-protected
          uri: http://localhost:8081
          predicates:
            - Path=/api/v1/auth/me,/api/v1/auth/change-password,/api/v1/auth/logout
          filters:
            - AuthenticationFilter
```

### Token Validation Filter
```java
public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");
    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
        return onError(exchange, HttpStatus.UNAUTHORIZED);
    }
    return webClient.get()
        .uri("http://localhost:8081/api/v1/auth/me")
        .header("Authorization", authHeader)
        .retrieve()
        .bodyToMono(ApiResponse.class)
        .flatMap(response -> {
            if (response.isSuccess()) {
                ServerHttpRequest request = exchange.getRequest().mutate()
                    .header("X-User-Id", response.getData().getId())
                    .header("X-User-Email", response.getData().getEmail())
                    .build();
                return chain.filter(exchange.mutate().request(request).build());
            }
            return onError(exchange, HttpStatus.UNAUTHORIZED);
        });
}
```

### Headers for Downstream Services
| Header | Description |
|--------|-------------|
| `X-User-Id` | Authenticated user's ID |
| `X-User-Email` | Authenticated user's email |
| `X-User-FirstName` | User's first name |
| `X-User-LastName` | User's last name |

## Environment Variables
| Variable | Description |
|----------|-------------|
| `MONGODB_URI` | MongoDB connection string |
| `JWT_SECRET` | Secret key for JWT signing (Base64) |
| `JWT_ACCESS_TOKEN_EXPIRATION` | Access token TTL in milliseconds |
| `JWT_REFRESH_TOKEN_EXPIRATION` | Refresh token TTL in milliseconds |

