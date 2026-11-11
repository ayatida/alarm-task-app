package com.adishra.alarmtask

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import java.util.Calendar

val alarmList = mutableStateListOf<Alarm>()

class MainActivity : ComponentActivity() {
    private val cameraPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }
    private val notifPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Camera & Notification permissions maango
        cameraPermission.launch(Manifest.permission.CAMERA)
        if (Build.VERSION.SDK_INT >= 33) {
            notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            MaterialTheme {
                val navController = rememberNavController()
                NavHost(navController = navController, startDestination = "home") {
                    composable("home") { HomeScreen(navController) }
                    composable("add") { AddAlarmScreen(navController) }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: androidx.navigation.NavController) {
    val context = LocalContext.current
    Scaffold(
        topBar = { TopAppBar(title = { Text("My Alarms") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { navController.navigate("add") }) {
                Icon(Icons.Default.Add, contentDescription = "Add")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            
            // Quick Test Button
            Button(
                onClick = { scheduleTestAlarm(context) },
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
            ) {
                Text("⚡ Test Alarm in 10 Seconds")
            }

            if (alarmList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No alarms set. Tap + to add.", fontSize = 18.sp)
                }
            } else {
                LazyColumn {
                    items(alarmList) { alarm ->
                        Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(String.format("%02d:%02d", alarm.hour, alarm.minute), fontSize = 32.sp, fontWeight = FontWeight.Bold)
                                Text("Task: ${alarm.taskName}", color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAlarmScreen(navController: androidx.navigation.NavController) {
    var hour by remember { mutableStateOf(7) }
    var minute by remember { mutableStateOf(0) }
    var task by remember { mutableStateOf("Take Medicine") }
    val context = LocalContext.current

    Scaffold(topBar = { TopAppBar(title = { Text("Set Alarm") }) }) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp).fillMaxSize()) {
            Button(
                onClick = {
                    val cal = Calendar.getInstance()
                    TimePickerDialog(context, { _, h, m -> hour = h; minute = m }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Time: ${String.format("%02d:%02d", hour, minute)}")
            }
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = task, onValueChange = { task = it },
                label = { Text("Task to complete") }, modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = {
                    val newAlarm = Alarm(id = alarmList.size + 1, hour = hour, minute = minute, days = "Everyday", taskName = task)
                    alarmList.add(newAlarm)
                    scheduleAlarm(context, newAlarm)
                    navController.popBackStack()
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Save Alarm") }
        }
    }
}

fun scheduleTestAlarm(context: Context) {
    val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val intent = Intent(context, AlarmReceiver::class.java).apply { putExtra("TASK_NAME", "Test Task (10s)") }
    val pi = PendingIntent.getBroadcast(context, 999, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    val time = System.currentTimeMillis() + 10000
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) {
        Toast.makeText(context, "Please allow exact alarms in settings", Toast.LENGTH_LONG).show()
        return
    }
    am.setAlarmClock(AlarmManager.AlarmClockInfo(time, pi), pi)
    Toast.makeText(context, "Test Alarm set for 10s", Toast.LENGTH_SHORT).show()
}

fun scheduleAlarm(context: Context, alarm: Alarm) {
    val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) {
        Toast.makeText(context, "Please allow exact alarms", Toast.LENGTH_LONG).show()
        return
    }
    val calendar = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, alarm.hour)
        set(Calendar.MINUTE, alarm.minute)
        set(Calendar.SECOND, 0)
        if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
    }
    val intent = Intent(context, AlarmReceiver::class.java).apply { putExtra("TASK_NAME", alarm.taskName) }
    val pi = PendingIntent.getBroadcast(context, alarm.id, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    am.setAlarmClock(AlarmManager.AlarmClockInfo(calendar.timeInMillis, pi), pi)
    Toast.makeText(context, "Alarm set", Toast.LENGTH_SHORT).show()
}