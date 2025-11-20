hpackage com.hans.i221271_i220889.network

object ApiConfig {
    // Change this to your actual server IP when testing
    // For local XAMPP: http://10.0.2.2/socially_api/ (Android Emulator)
    // For physical device: http://YOUR_PC_IP/socially_api/
    const val BASE_URL = "http://127.0.0.1/socially_api/"
    
    const val UPLOADS_BASE_URL = "${BASE_URL}uploads/"
    
    // Timeout configurations
    const val CONNECT_TIMEOUT = 30L
    const val READ_TIMEOUT = 30L
    const val WRITE_TIMEOUT = 30L
}

