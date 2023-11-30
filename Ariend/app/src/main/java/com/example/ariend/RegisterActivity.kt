package com.example.ariend

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RegisterActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pref = getSharedPreferences("Ariend", Context.MODE_PRIVATE)
        setContent {
            RegisterScreen(pref)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(pref: SharedPreferences) {
    val context = LocalContext.current

    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    val passwordPattern = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{6,}\$".toRegex()

    var user by remember {mutableStateOf(false)}
    var pass by remember {mutableStateOf(false)}
    var conpass by remember {mutableStateOf(false)}
    var loading by remember {mutableStateOf(false)}

    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(painter = painterResource(R.mipmap.ic_launcher_foreground), contentDescription = "")

        Text(
            "Ariend",
            color = Color.Black,
            style = MaterialTheme.typography.displayMedium,
            modifier = Modifier.padding(16.dp)
        )

        OutlinedTextField(
            value = username,
            isError = user,
            onValueChange = {
                username = it
                user = username.isEmpty()
            },
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            label = { Text("Username") }
        )

        OutlinedTextField(
            value = password,
            isError = pass,
            visualTransformation = PasswordVisualTransformation(),
            onValueChange = {
                password = it
                pass = password.isEmpty()
            },
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            label = { Text("Password") }
        )

        OutlinedTextField(
            value = confirmPassword,
            isError = conpass,
            visualTransformation = PasswordVisualTransformation(),
            onValueChange = {
                confirmPassword = it
                conpass = confirmPassword.isEmpty()
            },
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            label = { Text("Confirm Password") }
        )

        Button(
            onClick = {
                loading = true
                if (password == confirmPassword) {
                    if (passwordPattern.matches(password)) {
                        val inputData = ReqLogin(username, password)

                        coroutineScope.launch(Dispatchers.IO) {
                            try {
                                val apiService = RetrofitInstance.apiService
                                val res = apiService.register(inputData)

                                if (res.isSuccessful) {
                                    loading = false
                                    pref.edit().putBoolean("LOGIN_KEY", true).apply()
                                    pref.edit().putString("ID", username).apply()
                                    val intent = Intent(context, HomeActivity::class.java)
                                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                    context.startActivity(intent)
                                } else {
                                    loading = false
                                    user = true
                                    popup(context)
                                }
                            } catch (e: Exception) {
                                loading = false
                                e.printStackTrace()
                            }
                        }
                    } else {
                        loading = false
                        pass = true
                        conpass = true
                        Toast.makeText(context, "Username and/or Password should be atleast 6 words long, alphanumeric, one cap and one small", Toast.LENGTH_LONG).show()
                    }
                } else {
                    loading = false
                    pass = true
                    conpass = true
                    Toast.makeText(context, "Password and Confirm Password does not match", Toast.LENGTH_LONG).show()
                }
            },
            modifier = Modifier.padding(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF00BFA5),
                contentColor = Color.Black,
            ),
        ) {
            if (loading)
                CircularProgressIndicator(
                    color = Color.Blue,
                    modifier = Modifier.size(24.dp)
                )
            else
                Text("Register")
        }
    }
}

suspend fun popup(context: Context) {
    withContext(Dispatchers.Main) {
        Toast.makeText(context, "Username taken try another", Toast.LENGTH_LONG).show()
    }
}