package com.hazemjday.phogene

import android.app.DatePickerDialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.hazemjday.phogene.databinding.ActivityAuthBinding
import com.hazemjday.phogene.databinding.LayoutLoginBinding
import com.hazemjday.phogene.databinding.LayoutRegisterBinding
import java.text.SimpleDateFormat
import java.util.*

class AuthActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var dbRef: DatabaseReference
    private lateinit var authView: ActivityAuthBinding
    private lateinit var loginView: LayoutLoginBinding
    private lateinit var registerView: LayoutRegisterBinding
    private lateinit var calendar : Calendar
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        authView = ActivityAuthBinding.inflate(layoutInflater)
        loginView = LayoutLoginBinding.bind(authView.login.root)
        registerView = LayoutRegisterBinding.bind(authView.register.root)
        setContentView(authView.root)
        auth= FirebaseAuth.getInstance()
        dbRef = FirebaseDatabase.getInstance().reference.child("users")
        authView.login.cirLoginButton.setOnClickListener{
            login()
        }
        authView.login.toRegister.setOnClickListener{
            goToRegister()
        }

        authView.register.cirLoginButton.setOnClickListener(){
            register()
        }
        authView.register.toLogin.setOnClickListener{
            goToLogin()
        }
        calendar = Calendar.getInstance()
        val datePickerDialog = DatePickerDialog(
            this,
            { _, year, monthOfYear, dayOfMonth ->
                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, monthOfYear)
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                val format = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                registerView.editTextBirthDate.setText(format.format(calendar.time))
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        registerView.editTextBirthDate.setOnClickListener {
            datePickerDialog.show()
        }
        loginView.forgetPassword.setOnClickListener{
            showForgetPasswordDialog()
        }

    }


    private fun login(){
        val email=authView.login.editTextEmail.text.toString()
        val password=authView.login.editTextPassword.text.toString()

        if(email.isEmpty() || password.isEmpty()){
            Toast.makeText(applicationContext,"Email and password can not be empty !", Toast.LENGTH_LONG).show()
            return
        }
        auth.signInWithEmailAndPassword(email,password).addOnCompleteListener { task ->
            if(task.isSuccessful){
                startActivity(Intent(this,MainActivity::class.java))
                finish()
            }
        }.addOnFailureListener { exception ->
            Toast.makeText(applicationContext,exception.localizedMessage, Toast.LENGTH_LONG).show()
        }
    }
    private fun register(){
        val email=registerView.editTextEmail.text.toString()
        val password=registerView.editTextPassword.text.toString()
        val name=registerView.editTextName.text.toString()
        val birthDate=registerView.editTextBirthDate.text.toString()
        if(email.isEmpty() || password.isEmpty()){
            Toast.makeText(applicationContext,"Email and password can not be empty !",Toast.LENGTH_LONG).show()
            return
        }
        val user = User(name,email, birthDate)


        auth.createUserWithEmailAndPassword(email,password).addOnCompleteListener { task ->
            if(task.isSuccessful){
                dbRef.child(auth.uid!!).setValue(user)
                val intent= Intent(this,MainActivity::class.java)
                startActivity(intent)
                finish()
            }
        }.addOnFailureListener { exception ->
            Toast.makeText(applicationContext,exception.localizedMessage,Toast.LENGTH_LONG).show()
        }
    }
    private fun goToRegister(){
        loginView.root.visibility=View.GONE
        registerView.root.visibility=View.VISIBLE
    }
    private fun goToLogin(){
        registerView.root.visibility=View.GONE
        loginView.root.visibility=View.VISIBLE
    }
    private fun showForgetPasswordDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.layout_forget, null)
        val editTextEmail = dialogView.findViewById<EditText>(R.id.editTextEmail)
        val buttonConfirm = dialogView.findViewById<Button>(R.id.buttonConfirm)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        buttonConfirm.setOnClickListener {
            val email = editTextEmail.text.toString().trim()
            if (email.isNotEmpty() && Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                auth.sendPasswordResetEmail(email)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(this,"Email sent to $email",Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(this,"Email invalid !",Toast.LENGTH_LONG).show()
                        }
                    }
                dialog.dismiss()
            } else {
                editTextEmail.error = "Please enter a valid email address"
            }
        }

        dialog.show()
    }
}