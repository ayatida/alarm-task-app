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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        cameraPermission.launch(Manifest.permission.CAMERA)
        setContent {
            val navController = rememberNavController()
            NavHost(navController, "list") {
                composable("list") { AlarmListScreen(onAdd = { navController.navigate("add") }) }
                composable("add") { AlarmAddScreen(onDone = { navController.popBackStack() }) }
            }
        }
    }
}

@Composable
fun AlarmListScreen(onAdd: () -> Unit) {
    Scaffold(floatingActionButton = { FloatingActionButton(onClick = onAdd) { Icon(Icons.Default.Add, contentDescription = "Add") } }) { padding ->
        LazyColumn(modifier = Modifier.padding(padding)) {
            items(alarmList) { alarm -> Text("${alarm.hour}:${alarm.minute} - ${alarm.taskName}", modifier = Modifier.padding(16.dp)) }
        }
    }
}

@Composable
fun AlarmAddScreen(onDone: () -> Unit) {
    var hour by remember { mutableIntStateOf(8) }
    var minute by remember { mutableIntStateOf(0) }
    var taskName by remember { mutableStateOf("") }
    val context = LocalContext.current

    Scaffold(topBar = { TopAppBar(title = { Text("Set Alarm") }) }) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp).fillMaxSize()) {
            Button(
                onClick = {
                    val cal = Calendar.getInstance()
                    TimePickerDialog(context, { _, h, m -> hour = h; minute = m }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show()
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Time: ${String.format("%02d:%02d", hour, minute)}") }
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(value = taskName, onValueChange = { taskName = it }, label = { Text("Task Name") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(16.dp))
            Button(onClick = { 
                alarmList.add(Alarm(alarmList.size, hour, minute, taskName))
                onDone()
            }, modifier = Modifier.fillMaxWidth()) { Text("Save Alarm") }
        }
    }
}

fun scheduleTestAlarm(context: Context) {
    val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val intent = Intent(context, AlarmReceiver::class.java).apply { putExtra("TASK_NAME", "Test Task") }
    val pi = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
    am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 10000, pi)
    Toast.makeText(context, "Alarm in 10s", Toast.LENGTH_SHORT).show()
}
