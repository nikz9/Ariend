package com.example.ariend

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HomeScreen()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen() {
    val context = LocalContext.current
    val pref = context.getSharedPreferences("Ariend", Context.MODE_PRIVATE)
    val id = pref.getString("ID","")

    var newMessage by remember { mutableStateOf("") }
    var loading by remember {mutableStateOf(false)}

    val coroutineScope = rememberCoroutineScope()
    val dbHandler = DBHandler(context)

    val msgList: List<MsgModel> = dbHandler.getAllMessages()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        TopAppBar(
            title = { Text(text = "Ariend")},
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = Color(0xFF00BFA5),
            ),
            actions = {
                IconButton(
                    onClick = {
                        val builder = AlertDialog.Builder(context)
                        builder.setTitle("Reset Ariend")
                        builder.setPositiveButton("Yes") { _, _ ->
                            loading = true
                            val inputData = ReqReset(id.toString())

                            coroutineScope.launch(Dispatchers.IO) {
                                try {
                                    val apiService = RetrofitInstance.apiService
                                    val res = apiService.reset(inputData)

                                    if (res.isSuccessful) {
                                        loading = false
                                        dbHandler.deleteDb()
                                    } else {
                                        loading = false
                                        popupp(context)
                                    }
                                } catch (e: Exception) {
                                    loading = false
                                    e.printStackTrace()
                                }
                            }
                        }
                        builder.setNegativeButton("No") { _, _ -> }
                        builder.show()
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color.White, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        tint = Color(0xFF00BFA5),
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                IconButton(
                    onClick = {
                        val builder = AlertDialog.Builder(context)
                        builder.setTitle("Log Out")
                        builder.setPositiveButton("Yes") { _, _ ->
                            pref.edit().putBoolean("LOGIN_KEY", false).apply()
                            pref.edit().putString("ID","").apply()
                            dbHandler.deleteDb()
                            val intent = Intent(context, LoginActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            context.startActivity(intent)
                        }
                        builder.setNegativeButton("No") { _, _ -> }
                        builder.show()
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color.White, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Home,
                        contentDescription = null,
                        tint = Color(0xFF00BFA5),
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
            }
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
        ) {
            items(msgList.size) { index ->
                ChatMessage(message = msgList[index])
            }
        }

        ChatInput(
            loading = loading,
            newMessage = newMessage,
            onNewMessageChange = { newMessage = it },
            onSendClick = {
                loading = true
                dbHandler.addMessage(0, newMessage)
                val inputData = ReqMessage(id.toString(), newMessage)

                if (newMessage.isNotEmpty()) {
                    coroutineScope.launch(Dispatchers.IO) {
                        try {
                            val apiService = RetrofitInstance.apiService
                            val res = apiService.message(inputData)

                            if (res.isSuccessful) {
                                loading = false
                                newMessage = ""
                                dbHandler.addMessage(1, res.body()?.res.toString())
                            } else {
                                loading = false
                                newMessage = ""
                                popupp(context)
                            }
                        } catch (e: Exception) {
                            loading = false
                            newMessage = ""
                            e.printStackTrace()
                        }
                    }
                }
            },
        )
    }
}

@Composable
fun ChatInput(
    loading: Boolean,
    newMessage: String,
    onNewMessageChange: (String) -> Unit,
    onSendClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.padding(16.dp),
        shadowElevation = 4.dp,
        shape = RoundedCornerShape(30.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicTextField(
                value = newMessage,
                onValueChange = { onNewMessageChange(it) },
                textStyle = TextStyle(fontSize = 16.sp),
                keyboardOptions = KeyboardOptions.Default.copy(
                    imeAction = ImeAction.Send
                ),
                keyboardActions = KeyboardActions(
                    onSend = {
                        onSendClick()
                    }
                ),
                modifier = Modifier
                    .weight(1f)
                    .padding(8.dp)
            )

            IconButton(
                onClick = {
                    onSendClick()
                },
                modifier = Modifier
                    .size(48.dp)
                    .background(Color(0xFF00BFA5), CircleShape)
            ) {
                if (loading) {
                    CircularProgressIndicator(
                        color = Color.Blue,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ChatMessage(message: MsgModel) {
    if(message.id == 0) {
        Row (
            horizontalArrangement = Arrangement.End,
            modifier = Modifier.fillMaxWidth()
        ) {
            Surface(
                modifier = Modifier
                    .padding(start = 100.dp, end = 10.dp, top = 10.dp),
                shadowElevation = 4.dp,
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFE8E8E8)
            ) {
                Box(
                    modifier = Modifier
                        .padding(8.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = message.message,
                        fontSize = 16.sp
                    )
                }
            }
        }
    } else {
        Row (
            horizontalArrangement = Arrangement.Start,
            modifier = Modifier.fillMaxWidth()
        ) {
            Surface(
                modifier = Modifier
                    .padding(start = 10.dp, end = 100.dp, top = 10.dp),
                shadowElevation = 4.dp,
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFE8E8E8)
            ) {
                Box(
                    modifier = Modifier
                        .padding(8.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = message.message,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}

suspend fun popupp(context: Context) {
    withContext(Dispatchers.Main) {
        Toast.makeText(context, "Something went wrong try again later!", Toast.LENGTH_LONG).show()
    }
}