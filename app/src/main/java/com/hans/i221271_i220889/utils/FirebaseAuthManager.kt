package com.hans.i221271_i220889.utils

import android.content.Context
import android.content.Intent
import com.hans.i221271_i220889.LoginActivity
import com.hans.i221271_i220889.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class FirebaseAuthManager {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val database: FirebaseDatabase = FirebaseDatabase.getInstance()

    fun signUp(email: String, password: String, username: String, context: Context, onComplete: (Boolean, String?) -> Unit) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val fbUser = auth.currentUser
                    if (fbUser == null) {
                        onComplete(false, "User not available after signup")
                        return@addOnCompleteListener
                    }
                    val user = User(
                        userId = fbUser.uid,
                        username = username,
                        email = email,
                        profileImageUrl = "",
                        isOnline = true
                    )
                    database.reference.child("users").child(user.userId).setValue(user)
                        .addOnCompleteListener { saveTask ->
                            onComplete(saveTask.isSuccessful, saveTask.exception?.message)
                        }
                } else {
                    onComplete(false, task.exception?.message ?: "Signup failed")
                }
            }
    }

    fun signIn(email: String, password: String, context: Context, onComplete: (Boolean, String?) -> Unit) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onComplete(true, null)
                } else {
                    onComplete(false, task.exception?.message ?: "Login failed")
                }
            }
    }

    fun signOut(context: Context) {
        auth.signOut()
        val intent = Intent(context, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        context.startActivity(intent)
    }

    fun getCurrentUser(): User? {
        val fb = auth.currentUser ?: return null
        return User(
            userId = fb.uid, 
            email = fb.email ?: "",
            username = fb.displayName ?: "",
            profileImageUrl = "",
            isOnline = true
        )
    }

    fun isUserLoggedIn(): Boolean = auth.currentUser != null

    fun getUserData(userId: String, onComplete: (User?) -> Unit) {
        database.reference.child("users").child(userId).get()
            .addOnSuccessListener { snap ->
                onComplete(snap.getValue(User::class.java))
            }
            .addOnFailureListener { onComplete(null) }
    }

    fun updateUserProfile(user: User, onComplete: (Boolean) -> Unit) {
        database.reference.child("users").child(user.userId).setValue(user)
            .addOnCompleteListener { onComplete(it.isSuccessful) }
    }
}
