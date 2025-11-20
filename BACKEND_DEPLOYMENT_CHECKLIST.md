# Backend Deployment Checklist

## ✅ Current Configuration

**Backend IP Address**: `192.168.100.8`  
**API Base URL**: `http://192.168.100.8/socially_api/`  
**Database**: `socially` (root/no password)

---

## 📋 Setup Steps

### 1. Deploy PHP Backend to XAMPP

Copy the API folder to your XAMPP htdocs directory:

```
Source: app/src/main/java/com/hans/i221271_i220889/php_backend/socially_api/
Destination: C:\xampp\htdocs\socially_api\
```

**Folder structure should be:**
```
C:\xampp\htdocs\socially_api\
├── config\
│   └── conn.php
├── helpers\
│   ├── auth.php
│   ├── input.php
│   ├── media.php
│   └── response.php
├── routes\
│   ├── auth\
│   ├── posts\
│   ├── stories\
│   ├── messages\
│   ├── follow\
│   ├── profile\
│   └── search\
├── uploads\
│   ├── profile_pictures\
│   ├── posts\
│   ├── stories\
│   └── messages\
└── schema.sql
```

### 2. Create Database

1. Open phpMyAdmin: `http://localhost/phpmyadmin`
2. Click "SQL" tab
3. Copy entire contents of `schema.sql`
4. Click "Go" to execute
5. Verify `socially` database is created with all tables

### 3. Create Uploads Directories

Create these folders in your socially_api directory:
```bash
C:\xampp\htdocs\socially_api\uploads\profile_pictures\
C:\xampp\htdocs\socially_api\uploads\posts\
C:\xampp\htdocs\socially_api\uploads\stories\
C:\xampp\htdocs\socially_api\uploads\messages\
```

### 4. Verify Backend Configuration

Check `config/conn.php`:
```php
$host = 'localhost';
$dbname = 'socially';
$username = 'root';
$password = ''; // Empty for default XAMPP
```

### 5. Test Backend Connection

Open in browser: `http://192.168.100.8/socially_api/config/conn.php`

**Expected output**: "Database connected successfully"

If you see errors, check:
- [ ] Apache is running in XAMPP
- [ ] MySQL is running in XAMPP
- [ ] Database `socially` exists
- [ ] Your PC's firewall allows connections on port 80

### 6. Test API Endpoints

Use Postman or browser console to test:

#### Test Signup
```
POST http://192.168.100.8/socially_api/routes/auth/signup.php
Content-Type: application/json

{
  "username": "testuser",
  "email": "test@example.com",
  "password": "test123",
  "full_name": "Test User"
}
```

**Expected response:**
```json
{
  "success": true,
  "message": "User registered successfully",
  "data": {
    "user_id": 1,
    "username": "testuser",
    "token": "..."
  }
}
```

#### Test Login
```
POST http://192.168.100.8/socially_api/routes/auth/login.php
Content-Type: application/json

{
  "username": "testuser",
  "password": "test123"
}
```

### 7. Network Requirements

For physical device testing:
- [ ] Your PC and Android device are on the **same WiFi network**
- [ ] PC IP address is `192.168.100.8`
- [ ] Windows Firewall allows incoming connections on port 80
- [ ] XAMPP Apache is configured to listen on all interfaces (not just localhost)

**To verify your PC's IP:**
```bash
ipconfig
# Look for "IPv4 Address" under your active network adapter
```

### 8. Android App Configuration

The app is already configured with your IP:
- `ApiConfig.kt` → `BASE_URL = "http://192.168.100.8/socially_api/"`

If you need to change it later, edit:
```
app/src/main/java/com/hans/i221271_i220889/network/ApiConfig.kt
```

---

## 🔧 Troubleshooting

### Issue: "Failed to connect" error in app
**Solutions:**
1. Verify XAMPP Apache is running
2. Test in browser: `http://192.168.100.8/socially_api/config/conn.php`
3. Check firewall settings
4. Ensure both devices are on same WiFi

### Issue: "Database connection failed"
**Solutions:**
1. Verify MySQL is running in XAMPP
2. Check database name is exactly `socially`
3. Check credentials in `config/conn.php`

### Issue: "404 Not Found"
**Solutions:**
1. Verify folder is at `C:\xampp\htdocs\socially_api\`
2. Check file paths match exactly
3. Restart Apache in XAMPP

### Issue: "Permission denied" when uploading
**Solutions:**
1. Create `uploads` folder and subdirectories
2. Ensure folders have write permissions
3. Check Apache has permission to write to htdocs

### Issue: "Invalid JSON response"
**Solutions:**
1. Check for PHP errors in response
2. Verify Content-Type header is set correctly
3. Check PHP error logs in `C:\xampp\apache\logs\error.log`

---

## 📱 Testing the App

### From Android Emulator:
- Will NOT work with `192.168.100.8`
- Use `http://10.0.2.2/socially_api/` instead

### From Physical Device:
- ✅ Works with `http://192.168.100.8/socially_api/`
- Ensure device is on same WiFi network

### Quick Test Flow:
1. Launch app
2. Create account (Sign up)
3. Login with credentials
4. Create a post
5. View feed
6. Like/comment
7. Search users
8. Send messages

---

## 🎯 API Endpoint Reference

Base URL: `http://192.168.100.8/socially_api/`

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/routes/auth/signup.php` | Register new user |
| POST | `/routes/auth/login.php` | User login |
| POST | `/routes/auth/logout.php` | User logout |
| GET | `/routes/profile/get_profile.php` | Get own profile |
| GET | `/routes/profile/get_user_profile.php?user_id=X` | Get user profile |
| POST | `/routes/posts/create_post.php` | Create new post |
| GET | `/routes/posts/get_feed.php` | Get feed posts |
| POST | `/routes/posts/like_post.php` | Like a post |
| POST | `/routes/stories/create_story.php` | Create story |
| GET | `/routes/stories/get_all_stories.php` | Get all stories |
| POST | `/routes/messages/send_message.php` | Send message |
| GET | `/routes/messages/get_messages.php?receiver_id=X` | Get messages |
| POST | `/routes/follow/follow_user.php` | Follow user |
| GET | `/routes/search/search_users.php?query=X` | Search users |

---

**Backend deployment configuration complete!** 🚀

**Next**: Deploy PHP files to XAMPP and test endpoints before running the app.

