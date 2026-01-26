# Google OAuth 2.0 Integration Guide

## Overview

This guide explains how to integrate Google OAuth 2.0 login with the authentication service using the **Authorization Code Flow**.

## Configuration

### Google Client ID (for Frontend)
```
656999180278-c3qrr2hg7omdvl3vjqvqgb2n4aib33ag.apps.googleusercontent.com
```

### Redirect URI
```
http://localhost:3000/google/callback
```

---

## Frontend Implementation

### Step 1: Create Google OAuth URL

```typescript
const GOOGLE_CLIENT_ID = '656999180278-c3qrr2hg7omdvl3vjqvqgb2n4aib33ag.apps.googleusercontent.com';
const REDIRECT_URI = 'http://localhost:3000/google/callback';

const getGoogleOAuthURL = () => {
  const rootUrl = 'https://accounts.google.com/o/oauth2/v2/auth';
  
  const options = {
    client_id: GOOGLE_CLIENT_ID,
    redirect_uri: REDIRECT_URI,
    response_type: 'code',
    scope: [
      'https://www.googleapis.com/auth/userinfo.email',
      'https://www.googleapis.com/auth/userinfo.profile',
    ].join(' '),
    access_type: 'offline',
    prompt: 'consent',
  };

  const qs = new URLSearchParams(options);
  return `${rootUrl}?${qs.toString()}`;
};
```

### Step 2: Login Button Component

```tsx
// components/GoogleLoginButton.tsx
import React from 'react';

const GoogleLoginButton: React.FC = () => {
  const handleGoogleLogin = () => {
    const googleOAuthURL = getGoogleOAuthURL();
    window.location.href = googleOAuthURL;
  };

  return (
    <button 
      onClick={handleGoogleLogin}
      className="google-login-btn"
    >
      <img src="/google-icon.svg" alt="Google" />
      Continue with Google
    </button>
  );
};

export default GoogleLoginButton;
```

### Step 3: Callback Page

```tsx
// pages/google/callback.tsx (or app/google/callback/page.tsx for Next.js App Router)
import { useEffect } from 'react';
import { useRouter, useSearchParams } from 'next/navigation';

const GoogleCallback: React.FC = () => {
  const router = useRouter();
  const searchParams = useSearchParams();

  useEffect(() => {
    const code = searchParams.get('code');
    const error = searchParams.get('error');

    if (error) {
      console.error('Google OAuth error:', error);
      router.push('/login?error=google_auth_failed');
      return;
    }

    if (code) {
      handleGoogleCallback(code);
    }
  }, [searchParams]);

  const handleGoogleCallback = async (code: string) => {
    try {
      const response = await fetch('http://localhost:9000/api/v1/auth/google', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          code: code,
          redirectUri: 'http://localhost:3000/google/callback',
        }),
      });

      if (!response.ok) {
        throw new Error('Google authentication failed');
      }

      const data = await response.json();
      
      // Store tokens
      localStorage.setItem('accessToken', data.accessToken);
      localStorage.setItem('refreshToken', data.refreshToken);
      
      // Store user info (optional)
      localStorage.setItem('user', JSON.stringify(data.user));

      // Redirect to dashboard or home
      router.push('/dashboard');
    } catch (error) {
      console.error('Error during Google login:', error);
      router.push('/login?error=google_auth_failed');
    }
  };

  return (
    <div className="flex items-center justify-center min-h-screen">
      <div className="text-center">
        <div className="spinner" />
        <p>Signing in with Google...</p>
      </div>
    </div>
  );
};

export default GoogleCallback;
```

---

## API Reference

### POST `/api/v1/auth/google`

Authenticate user with Google OAuth authorization code.

**URL:** `http://localhost:9000/api/v1/auth/google` (via Gateway)

**Method:** `POST`

**Headers:**
```
Content-Type: application/json
```

**Request Body:**
```json
{
  "code": "4/0AX4XfWh...",
  "redirectUri": "http://localhost:3000/google/callback"
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `code` | string | Yes | Authorization code from Google OAuth redirect |
| `redirectUri` | string | No | Must match the redirect URI used in the OAuth flow |

**Success Response (200 OK):**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "expiresIn": 3600000,
  "user": {
    "id": "507f1f77bcf86cd799439011",
    "email": "user@gmail.com",
    "firstName": "John",
    "lastName": "Doe",
    "accountType": "FREE",
    "authProvider": "GOOGLE",
    "profilePictureUrl": "https://lh3.googleusercontent.com/a/..."
  }
}
```

**Error Responses:**

| Status | Error | Description |
|--------|-------|-------------|
| 400 | Bad Request | Invalid or missing authorization code |
| 401 | Unauthorized | Failed to exchange code for token |
| 500 | Internal Server Error | Server error during authentication |

---

## User Object Fields

| Field | Type | Description |
|-------|------|-------------|
| `id` | string | Unique user ID |
| `email` | string | User's email address |
| `firstName` | string | User's first name |
| `lastName` | string | User's last name |
| `accountType` | string | Account type: `FREE` or `VIP` |
| `authProvider` | string | Authentication provider: `LOCAL` or `GOOGLE` |
| `profilePictureUrl` | string | URL to user's Google profile picture |

---

## Account Linking Behavior

| Scenario | Behavior |
|----------|----------|
| **New user** | Creates new account with `authProvider: GOOGLE` |
| **Existing Google user** | Logs in, updates profile picture if changed |
| **Existing local user (same email)** | Links Google account, user can still use password |

---

## Example with Axios

```typescript
import axios from 'axios';

const API_BASE_URL = 'http://localhost:9000/api/v1';

interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  user: {
    id: string;
    email: string;
    firstName: string;
    lastName: string;
    accountType: 'FREE' | 'VIP';
    authProvider: 'LOCAL' | 'GOOGLE';
    profilePictureUrl: string | null;
  };
}

export const loginWithGoogle = async (code: string): Promise<AuthResponse> => {
  const response = await axios.post<AuthResponse>(`${API_BASE_URL}/auth/google`, {
    code,
    redirectUri: 'http://localhost:3000/google/callback',
  });
  return response.data;
};
```

---

## Testing with cURL

```bash
curl -X POST http://localhost:9000/api/v1/auth/google \
  -H "Content-Type: application/json" \
  -d '{
    "code": "YOUR_AUTHORIZATION_CODE",
    "redirectUri": "http://localhost:3000/google/callback"
  }'
```

---

## Notes

- The authorization code can only be used **once** and expires quickly (~10 minutes)
- The `redirectUri` must **exactly match** the one used when generating the OAuth URL
- Access tokens expire after 1 hour (3600000 ms)
- Refresh tokens expire after 24 hours (86400000 ms)

