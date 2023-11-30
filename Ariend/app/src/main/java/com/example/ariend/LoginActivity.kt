package com.example.ariend

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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


class LoginActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pref = getSharedPreferences("Ariend", Context.MODE_PRIVATE)
        if (pref.getBoolean("LOGIN_KEY", false)) {
            val intent = Intent(this, HomeActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        } else {
            setContent {
                LoginScreen(pref)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(pref: SharedPreferences) {
    val context = LocalContext.current
    val dbHandler = DBHandler(context)

    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    var user by remember {mutableStateOf(false)}
    var pass by remember {mutableStateOf(false)}
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

        Button(
            onClick = {
                loading = true
                if (username.isNotEmpty() && password.isNotEmpty()) {
                    val inputData = ReqLogin(username, password)

                    coroutineScope.launch(Dispatchers.IO) {
                        try {
                            val apiService = RetrofitInstance.apiService
                            val res = apiService.login(inputData)

                            if (res.isSuccessful) {
                                loading = false
                                pref.edit().putBoolean("LOGIN_KEY", true).apply()
                                pref.edit().putString("ID", username).apply()
                                for ((no, i) in res.body()!!.res.withIndex()) {
                                    if ((no+1)%2 == 0) {
                                        dbHandler.addMessage(1, i)
                                    } else {
                                        dbHandler.addMessage(0, i)
                                    }
                                }
                                val intent = Intent(context, HomeActivity::class.java)
                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                context.startActivity(intent)
                            } else {
                                loading = false
                                user = true
                                pass = true
                                popUp(context)
                            }
                        } catch (e: Exception) {
                            loading = false
                            e.printStackTrace()
                        }
                    }
                } else {
                    loading = false
                    user = true
                    pass = true
                    Toast.makeText(context, "Username and/or Password required", Toast.LENGTH_LONG).show()
                }
            },
            modifier = Modifier
                .padding(16.dp),
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
                 Text("Login")
        }

        Button(
            onClick = {
                context.startActivity(Intent(context, RegisterActivity::class.java))
            },
            modifier = Modifier
                .padding(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF00BFA5),
                contentColor = Color.Black,
            ),
        ) {
            Text("Register")
        }
    }
}

suspend fun popUp(context: Context) {
    withContext(Dispatchers.Main) {
        Toast.makeText(context, "Username and/or Password incorrect", Toast.LENGTH_LONG).show()
    }
}