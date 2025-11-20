# Database Setup Guide

## Prerequisites
- XAMPP installed with Apache and MySQL running
- phpMyAdmin accessible (usually at http://localhost/phpmyadmin)

## Step 1: Create Database

1. Open phpMyAdmin in your browser: `http://localhost/phpmyadmin`
2. Click on "SQL" tab
3. Copy and paste the entire contents of `app/src/main/java/com/hans/i221271_i220889/php_backend/socially_api/schema.sql`
4. Click "Go" to execute

The script will:
- Create the `socially` database
- Create all necessary tables (users, sessions, stories, posts, likes, comments, messages, follows, notifications, user_status, screenshot_reports)
- Set up all indexes and foreign keys

## Step 2: Deploy Backend API

1. Copy the entire `php_backend/socially_api` folder to your XAMPP `htdocs` directory:
   ```
   C:\xampp\htdocs\socially_api\
   ```

2. Your folder structure should look like:
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
   │   │   ├── login.php
   │   │   ├── logout.php
   │   │   └── signup.php
   │   ├── posts\
   │   ├── stories\
   │   ├── messages\
   │   ├── follow\
   │   ├── profile\
   │   └── search\
   └── schema.sql
   ```

3. Verify database connection in `config/conn.php`:
   ```php
   $host = 'localhost';
   $dbname = 'socially';
   $username = 'root';
   $password = ''; // Empty password for XAMPP default
   ```

## Step 3: Configure Android App

1. Open `app/src/main/java/com/hans/i221271_i220889/network/ApiConfig.kt`

2. Set the correct `BASE_URL`:
   - For Android Emulator: `http://10.0.2.2/socially_api/`
   - For Physical Device: `http://YOUR_PC_IP/socially_api/`
   
   To find your PC's IP:
   - Open Command Prompt
   - Run: `ipconfig`
   - Look for "IPv4 Address" (usually something like 192.168.1.x)

## Step 4: Test API Endpoints

You can test if the API is working by visiting these URLs in your browser:

### Test Database Connection
```
http://localhost/socially_api/config/conn.php
```
Should show: "Database connected successfully"

### Test Signup Endpoint (using Postman or browser console)
```
POST http://localhost/socially_api/routes/auth/signup.php
Content-Type: application/json

{
  "username": "testuser",
  "email": "test@example.com",
  "password": "password123",
  "full_name": "Test User"
}
```

### Test Login Endpoint
```
POST http://localhost/socially_api/routes/auth/login.php
Content-Type: application/json

{
  "username": "testuser",
  "password": "password123"
}
```

## Common Issues & Fixes

### Issue 1: "Database not found" error
**Solution**: Make sure you ran the schema.sql script in phpMyAdmin

### Issue 2: "Access denied for user 'root'" error
**Solution**: Check your MySQL credentials in `config/conn.php`

### Issue 3: "Connection refused" from Android app
**Solution**: 
- For Emulator: Use `http://10.0.2.2/socially_api/`
- For Physical Device: Make sure your PC and phone are on the same WiFi network, and use your PC's IP address

### Issue 4: "Cannot connect to database" error
**Solution**: Make sure Apache and MySQL are running in XAMPP Control Panel

### Issue 5: TIMESTAMP errors when creating tables
**Solution**: Already fixed! The schema.sql now uses `TIMESTAMP NULL DEFAULT NULL` for expires_at columns

## Step 5: Create Uploads Directory

1. Create an `uploads` folder in your socially_api directory:
   ```
   C:\xampp\htdocs\socially_api\uploads\
   ```

2. Create subdirectories:
   ```
   C:\xampp\htdocs\socially_api\uploads\
   ├── profile_pictures\
   ├── posts\
   ├── stories\
   └── messages\
   ```

3. Make sure these folders have write permissions

## Testing Checklist

- [ ] phpMyAdmin accessible
- [ ] Database `socially` created
- [ ] All tables created successfully
- [ ] API folder copied to htdocs
- [ ] Can access http://localhost/socially_api/config/conn.php
- [ ] Uploads folder created with subdirectories
- [ ] ApiConfig.kt has correct BASE_URL
- [ ] XAMPP Apache and MySQL services running

## Database Schema Overview

### Core Tables
- **users**: User accounts and profiles
- **sessions**: Active login sessions with tokens
- **posts**: User posts with media
- **stories**: 24-hour stories
- **likes**: Post likes
- **comments**: Post comments
- **messages**: Direct messages between users
- **follows**: Follow relationships (with pending/accepted status)
- **notifications**: User notifications
- **user_status**: Online/offline status
- **screenshot_reports**: Screenshot detection reports

### Default Settings
- Database: `socially`
- Username: `root`
- Password: (empty)
- Port: 3306 (default MySQL port)
- Character Set: utf8mb4
- Collation: utf8mb4_unicode_ci

---

**Now you're ready to run the Socially app with the new PHP/MySQL backend!** 🚀

