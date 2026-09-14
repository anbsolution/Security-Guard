package com.securityguard.app

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.securityguard.app.core.animation.AnimationLevel
import com.securityguard.app.core.animation.AnimationManager
import com.securityguard.app.core.audio.AudioEvent
import com.securityguard.app.core.audio.AudioManager
import com.securityguard.app.core.haptics.HapticManager
import com.securityguard.app.core.database.*
import com.securityguard.app.feature.onboarding.OnboardingStep
import com.securityguard.app.feature.shifts.ShiftManager
import com.securityguard.app.feature.rounds.RoundRepository
import com.securityguard.app.feature.rounds.RoundEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class MainActivity : ComponentActivity() {
    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        AudioManager.init(this)
        HapticManager.init(this)
        setContent {
            SecurityGuardRoot(
                openRoundId = intent.getLongExtra("open_round_id", -1L),
                requestNotifications = {
                    if (android.os.Build.VERSION.SDK_INT >= 33) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            )
        }
    }
}

@Composable
private fun SecurityGuardRoot(openRoundId: Long = -1L, requestNotifications: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as SecurityGuardApplication
    var setupComplete by remember { mutableStateOf(app.getSharedPreferences("app_state", Context.MODE_PRIVATE).getBoolean("setup_complete", false)) }
    var unlocked by remember { mutableStateOf(!app.pinManager.hasPin()) }
    var loading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        loading = false
        if (!setupComplete) unlocked = true
    }

    MaterialTheme {
        when {
            loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            !setupComplete -> OnboardingScreen(onFinished = { setupComplete = true; unlocked = true })
            !unlocked -> LockScreen(onUnlock = { unlocked = true })
            else -> MainApp(initialRoundId = openRoundId, onLock = { unlocked = false })
        }
    }
}

@Composable
private fun OnboardingScreen(onFinished: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as SecurityGuardApplication
    val prefs = remember { app.getSharedPreferences("onboarding", Context.MODE_PRIVATE) }
    val scope = rememberCoroutineScope()
    var stepIndex by remember { mutableIntStateOf(prefs.getInt("step", 0).coerceIn(0, 10)) }
    var name by remember { mutableStateOf(prefs.getString("name", "") ?: "") }
    var guardId by remember { mutableStateOf(prefs.getString("guard_id", "") ?: "") }
    var site by remember { mutableStateOf(prefs.getString("site", "") ?: "") }
    var joining by remember { mutableStateOf(prefs.getString("joining", LocalDate.now().toString()) ?: LocalDate.now().toString()) }
    var salary by remember { mutableStateOf(prefs.getString("salary", "20000") ?: "20000") }
    var salaryType by remember { mutableStateOf(prefs.getString("salary_type", "MONTHLY") ?: "MONTHLY") }
    var weeklyOffCount by remember { mutableIntStateOf(prefs.getInt("weekly_off_count", 1)) }
    var pin by remember { mutableStateOf("") }
    var pinConfirm by remember { mutableStateOf("") }
    var nightRounds by remember { mutableStateOf(prefs.getBoolean("night_rounds", true)) }
    var checkpointCount by remember { mutableIntStateOf(prefs.getInt("checkpoint_count", 4)) }
    var lateAfter by remember { mutableIntStateOf(prefs.getInt("late_after", 1)) }
    var repeatEvery by remember { mutableIntStateOf(prefs.getInt("repeat_every", 5)) }
    var error by remember { mutableStateOf<String?>(null) }
    val step = OnboardingStep.entries[stepIndex]

    fun saveStep(next: Int) {
        prefs.edit().putInt("step", next).putString("name", name).putString("guard_id", guardId)
            .putString("site", site).putString("joining", joining).putString("salary", salary)
            .putString("salary_type", salaryType).putInt("weekly_off_count", weeklyOffCount)
            .putBoolean("night_rounds", nightRounds).putInt("checkpoint_count", checkpointCount)
            .putInt("late_after", lateAfter).putInt("repeat_every", repeatEvery).apply()
        stepIndex = next
        HapticManager.click(); AudioManager.emit(AudioEvent.ButtonClick)
    }

    Scaffold(topBar = {
        TopAppBar(title = { Text("Setup • ${step.title}") })
    }) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).padding(horizontal = 20.dp)) {
            LinearProgressIndicator((stepIndex + 1) / 11f, Modifier.fillMaxWidth())
            Spacer(Modifier.height(18.dp))
            AnimatedContent(stepIndex, transitionSpec = { fadeIn().togetherWith(fadeOut()) }, label = "onboarding") { current ->
                LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    item {
                        when (OnboardingStep.entries[current]) {
                            OnboardingStep.WELCOME -> WelcomeStep()
                            OnboardingStep.PROFILE -> ProfileStep(name, { name = it }, guardId, { guardId = it }, site, { site = it }, joining, { joining = it })
                            OnboardingStep.SALARY -> SalaryStep(salary, { salary = it }, salaryType, { salaryType = it })
                            OnboardingStep.WEEKLY_OFF -> WeeklyOffStep(weeklyOffCount) { weeklyOffCount = it }
                            OnboardingStep.PIN -> PinStep(pin, { pin = it }, pinConfirm, { pinConfirm = it }, error)
                            OnboardingStep.SHIFT -> ShiftStep()
                            OnboardingStep.CHECKPOINTS -> CheckpointStep(checkpointCount) { checkpointCount = it }
                            OnboardingStep.ROUND_SETTINGS -> RoundSettingsStep(nightRounds, { nightRounds = it }, lateAfter, { lateAfter = it }, repeatEvery, { repeatEvery = it })
                            OnboardingStep.ALERTS -> AlertStep()
                            OnboardingStep.PERMISSIONS -> PermissionStep(requestNotifications = {
                                if (android.os.Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                                    requestNotifications()
                                }
                            }, alarmsReady = (context.getSystemService(Context.ALARM_SERVICE) as AlarmManager).canScheduleExactAlarms())
                            OnboardingStep.REVIEW -> ReviewStep(name, site, salary, salaryType, weeklyOffCount, nightRounds, checkpointCount, lateAfter, repeatEvery)
                        }
                    }
                }
            }
            if (error != null && step != OnboardingStep.PIN) Text(error!!, color = MaterialTheme.colorScheme.error)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (stepIndex > 0) OutlinedButton(onClick = { saveStep(stepIndex - 1) }, Modifier.weight(1f)) { Text("Back") }
                val last = stepIndex == 10
                Button(onClick = {
                    error = null
                    if (step == OnboardingStep.PROFILE && (name.isBlank() || site.isBlank() || joining.isBlank())) { error = "Name, duty site and joining date are required."; return@Button }
                    if (step == OnboardingStep.SALARY && (salary.toLongOrNull() ?: 0L) <= 0) { error = "Enter a valid salary."; return@Button }
                    if (step == OnboardingStep.PIN) {
                        if (pin.length !in 4..6 || !pin.all(Char::isDigit)) { error = "PIN must contain 4–6 digits."; return@Button }
                        if (pin != pinConfirm) { error = "PIN confirmation does not match."; return@Button }
                        app.pinManager.setPin(pin)
                    }
                    if (!last) saveStep(stepIndex + 1) else {
                        scope.launch(Dispatchers.IO) {
                            val now = System.currentTimeMillis()
                            app.database.guardDao().upsert(GuardEntity(1, name.trim(), guardId.trim().ifBlank { null }, site.trim(), joining, null, null, null, null, now, now))
                            val basic = (salary.toLongOrNull() ?: 20000L) * 100L
                            app.database.salaryDao().insert(SalarySettingsEntity(0, joining, salaryType, basic, if (salaryType == "HOURLY") basic / 30L / 8L else 0L, true, 0L, "FIXED", "EXACT", "CONFIGURED", "CONFIGURED", "50_PERCENT", 26, now, now))
                            val day = ShiftEntity(0, "Day Shift", "DAY", "08:00", "20:00", false, false, 60, 1, 5, false, true, now, now)
                            val night = ShiftEntity(0, "Night Shift", "NIGHT", "20:00", "08:00", true, nightRounds, 60, lateAfter, repeatEvery, true, true, now, now)
                            if (app.database.shiftDao().active().none { it.name == day.name }) app.database.shiftDao().insert(day)
                            if (app.database.shiftDao().active().none { it.name == night.name }) app.database.shiftDao().insert(night)
                            repeat(checkpointCount.coerceIn(0, 20)) { i -> app.database.checkpointDao().insert(CheckpointEntity(0, "Checkpoint ${i + 1}", i + 1, true, now, now)) }
                            prefs.edit().clear().apply()
                            app.getSharedPreferences("app_state", Context.MODE_PRIVATE).edit().putBoolean("setup_complete", true).apply()
                            withContext(kotlinx.coroutines.Dispatchers.Main) { HapticManager.success(); AudioManager.emit(AudioEvent.Success); onFinished() }
                        }
                    }
                }, Modifier.weight(1f)) { Text(if (last) "Finish Setup" else "Continue") }
            }
            Spacer(Modifier.height(10.dp))
        }
    }
}

@Composable private fun WelcomeStep() { Column { Text("Welcome to Security Guard", fontSize = 30.sp); Text("Offline-first duty, attendance, patrol rounds and salary tracking in one app."); Spacer(Modifier.height(12.dp)); Card { Column(Modifier.padding(18.dp)) { Text("One guard • Multiple shifts • Local data • Backup & restore"); Text("No GPS or camera is required.") } } } }
@Composable private fun ProfileStep(name:String,onName:(String)->Unit,id:String,onId:(String)->Unit,site:String,onSite:(String)->Unit,joining:String,onJoining:(String)->Unit){ Field("Full Name *",name,onName); Field("Guard ID",id,onId); Field("Duty Site / Post *",site,onSite); Field("Joining Date (YYYY-MM-DD) *",joining,onJoining) }
@Composable private fun SalaryStep(value:String,onValue:(String)->Unit,type:String,onType:(String)->Unit){ Text("Salary setup",fontSize=24.sp); Field("Basic Salary (₹)",value,onValue,KeyboardType.Number); Text("Salary Type"); Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){ listOf("MONTHLY","DAILY","HOURLY","CUSTOM").forEach{ FilterChip(selected=type==it,onClick={onType(it)},label={Text(it)}) } } }
@Composable private fun WeeklyOffStep(count:Int,onChange:(Int)->Unit){ Text("Weekly Off",fontSize=24.sp); Text("Choose the recurring number of weekly-off days. Actual salary treatment is configured separately."); Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){ (0..4).forEach{ FilterChip(selected=count==it,onClick={onChange(it)},label={Text(if(it==0) "None" else "$it")}) } } }
@Composable private fun PinStep(pin:String,onPin:(String)->Unit,confirm:String,onConfirm:(String)->Unit,error:String?){ Text("Create App PIN",fontSize=24.sp); Text("4–6 digits. The PIN is stored as a secure verifier, never as plain text."); Field("PIN",pin,onPin,KeyboardType.Number); Field("Confirm PIN",confirm,onConfirm,KeyboardType.Number); if(error!=null) Text(error,color=MaterialTheme.colorScheme.error) }
@Composable private fun ShiftStep(){ Text("Shift Profiles",fontSize=24.sp); Card { Column(Modifier.padding(16.dp)){Text("☀ Day Shift  08:00–20:00 • Rounds OFF"); Text("🌙 Night Shift  20:00–08:00 • Rounds configurable"); Text("You can add Overtime and Custom shifts later.")}} }
@Composable private fun CheckpointStep(count:Int,onChange:(Int)->Unit){ Text("Checkpoints",fontSize=24.sp); Text("How many default checkpoints should the Night Shift use?"); Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){ listOf(0,2,4,6,8).forEach{FilterChip(selected=count==it,onClick={onChange(it)},label={Text("$it")})} } }
@Composable private fun RoundSettingsStep(enabled:Boolean,onEnabled:(Boolean)->Unit,late:Int,onLate:(Int)->Unit,repeat:Int,onRepeat:(Int)->Unit){ Text("Round Settings",fontSize=24.sp); Row(verticalAlignment=Alignment.CenterVertically){Text("Night Shift rounds enabled",Modifier.weight(1f));Switch(enabled,onEnabled)}; Field("Late alert after (minutes)",late.toString(),{onLate((it.toIntOrNull()?:1).coerceIn(1,60))},KeyboardType.Number); Field("Repeat late alert every (minutes)",repeat.toString(),{onRepeat((it.toIntOrNull()?:5).coerceIn(1,60))},KeyboardType.Number) }
@Composable private fun AlertStep(){ Text("Alerts & Vibration",fontSize=24.sp); Text("Late rounds can trigger notification + vibration. Normal round due times do not alert. These settings remain configurable later.") }
@Composable private fun PermissionStep(requestNotifications:()->Unit,alarmsReady:Boolean){ Text("Permissions",fontSize=24.sp); val context=LocalContext.current; val notificationOk=android.os.Build.VERSION.SDK_INT<33 || ContextCompat.checkSelfPermission(context,Manifest.permission.POST_NOTIFICATIONS)==PackageManager.PERMISSION_GRANTED; Text("Notifications: ${if(notificationOk) "Ready" else "Not granted"}"); Text("Exact alarms: ${if(alarmsReady) "Ready" else "System setting may be required"}"); Button(onClick=requestNotifications){Text("Request Notifications")}; Text("Permission denial does not block core offline attendance features.") }
@Composable private fun ReviewStep(name:String,site:String,salary:String,type:String,off:Int,rounds:Boolean,cp:Int,late:Int,repeat:Int){ Text("Review Setup",fontSize=24.sp); listOf("Guard: $name","Site: $site","Salary: ₹$salary • $type","Weekly off: $off day(s)","Night rounds: ${if(rounds) "ON" else "OFF"}","Checkpoints: $cp","Late alert: ${late}m • Repeat ${repeat}m").forEach{Text("✓ $it")}; Text("Finish Setup saves the configuration and opens the dashboard. It does not check you in automatically.") }
@Composable private fun Field(label:String,value:String,onValue:(String)->Unit,keyboard:KeyboardType=KeyboardType.Text){ OutlinedTextField(value,onValue,Modifier.fillMaxWidth(),label={Text(label)},singleLine=true,keyboardOptions=KeyboardOptions(keyboardType=keyboard)) }

@Composable private fun LockScreen(onUnlock:()->Unit){ val context=LocalContext.current; val app=context.applicationContext as SecurityGuardApplication; var pin by remember{mutableStateOf("")}; var error by remember{mutableStateOf(false)}; Column(Modifier.fillMaxSize().padding(28.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center){ Icon(Icons.Default.Lock,null,Modifier.size(64.dp)); Text("Security Guard",fontSize=30.sp); Text("Enter PIN"); Spacer(Modifier.height(16.dp)); OutlinedTextField(pin,{v->pin=v.filter(Char::isDigit).take(6)},label={Text("PIN")},singleLine=true,keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number)); if(error) Text("Incorrect PIN",color=MaterialTheme.colorScheme.error); Spacer(Modifier.height(12.dp)); Button(onClick={if(app.pinManager.verify(pin)){error=false;HapticManager.success();onUnlock()}else{error=true;HapticManager.error();AudioManager.emit(AudioEvent.Error)}},enabled=pin.length>=4){Text("Unlock")}} }

@Composable
fun MainApp(initialRoundId: Long = -1L, onLock:()->Unit){
    val context = LocalContext.current
    val app = context.applicationContext as SecurityGuardApplication
    var tab by remember{mutableIntStateOf(if(initialRoundId > 0L) 2 else 0)}
    var refresh by remember{mutableIntStateOf(0)}
    var showShiftSelection by remember{mutableStateOf(false)}
    val titles=listOf("Home","Attendance","Rounds","Salary","More")
    Scaffold(topBar={TopAppBar(title={Text("Security Guard")})},bottomBar={
        NavigationBar{titles.forEachIndexed{i,t->NavigationBarItem(selected=tab==i,onClick={tab=i;HapticManager.click();AudioManager.emit(AudioEvent.ButtonClick)},icon={Icon(when(i){0->Icons.Default.Home;1->Icons.Default.Event;2->Icons.Default.Shield;3->Icons.Default.AccountBalance;else->Icons.Default.Settings},null)},label={Text(t)})}}
    }){p->
        AnimatedContent(tab,transitionSpec={fadeIn().togetherWith(fadeOut())},modifier=Modifier.padding(p),label="screen"){when(it){
            0->Home(refresh=refresh,onSelectShift={showShiftSelection=true},onRefresh={refresh++})
            1->Attendance()
            2->Rounds()
            3->Salary()
            else->More(onLock=onLock,onRefresh={refresh++})
        }}}
    }
    if(showShiftSelection) ShiftSelectionDialog(onDismiss={showShiftSelection=false},onSelected={refresh++;showShiftSelection=false})
}

@Composable
private fun Home(refresh:Int,onSelectShift:()->Unit,onRefresh:()->Unit){
    val context=LocalContext.current
    val app=context.applicationContext as SecurityGuardApplication
    val manager=remember{ShiftManager(app.database)}
    var selected by remember(refresh){mutableStateOf<ShiftEntity?>(null)}
    var active by remember(refresh){mutableStateOf<ShiftSessionEntity?>(null)}
    var loading by remember{mutableStateOf(true)}
    var message by remember{mutableStateOf<String?>(null)}
    val scope=rememberCoroutineScope()
    LaunchedEffect(refresh){loading=true;selected=manager.selectedShiftId()?.let{app.database.shiftDao().get(it)};active=manager.activeSession();loading=false}
    LazyColumn(Modifier.fillMaxSize().padding(18.dp),verticalArrangement=Arrangement.spacedBy(4.dp)){
        item{Text("Good Morning, Guard",fontSize=28.sp);Text("Today’s duty overview",color=MaterialTheme.colorScheme.onSurfaceVariant);Spacer(Modifier.height(12.dp))}
        item{
            CardItem("Today’s Shift",selected?.let{"${it.name} • ${it.startTime}–${it.endTime}"} ?: "Not selected — select before Check-In","🕘",onSelectShift)
        }
        item{
            val dutyText=when{active!=null->"${selected?.name ?: "Active Shift"} • Checked In";else->"Not Checked In"}
            CardItem("Attendance",dutyText,"✓")
        }
        item{
            if(active==null){
                Button(onClick={scope.launch{try{manager.startShift();message="Check-In successful";HapticManager.success();AudioManager.emit(AudioEvent.CheckIn);onRefresh()}catch(e:Exception){message=e.message?:"Unable to Check-In";HapticManager.error();AudioManager.emit(AudioEvent.Error)}}},enabled=selected!=null,modifier=Modifier.fillMaxWidth()){Text("Slide to Check-In")}
            }else{
                Button(onClick={scope.launch{try{manager.checkOut();message="Check-Out successful";HapticManager.success();AudioManager.emit(AudioEvent.CheckOut);onRefresh()}catch(e:Exception){message=e.message?:"Unable to Check-Out";HapticManager.error();AudioManager.emit(AudioEvent.Error)}}},modifier=Modifier.fillMaxWidth()){Text("Slide to Check-Out")}
            }
        }
        if(message!=null)item{Text(message!!,color=MaterialTheme.colorScheme.primary)}
        item{CardItem("Patrolling",if(selected?.roundsEnabled==true)"Enabled for this shift" else "Not Applicable for this shift","🛡")}
        if (active != null && selected?.roundsEnabled == true) {
            item { RoundHomeSummary(sessionId = active!!.id, refresh = refresh) }
        }
        item{CardItem("Salary","Configured from salary settings","₹")}
        item{Spacer(Modifier.height(14.dp));Text("Global Experience",fontSize=20.sp)}
        item{CardItem("Animation","Automatic state transitions","✨")}
        item{CardItem("Audio","BGM/SFX configurable","🔊")}
        item{CardItem("Haptic","Important actions only","📳")}
    }
}

@Composable
private fun RoundHomeSummary(sessionId: Long, refresh: Int) {
    val context = LocalContext.current
    val app = context.applicationContext as SecurityGuardApplication
    var rounds by remember(refresh) { mutableStateOf<List<RoundEntity>>(emptyList()) }
    LaunchedEffect(refresh, sessionId) { rounds = app.database.roundDao().forSession(sessionId) }
    val active = rounds.firstOrNull { it.status == "IN_PROGRESS" }
    val late = rounds.firstOrNull { it.status == "LATE" }
    val pending = rounds.count { it.status == "SCHEDULED" || it.status == "LATE" }
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = if(late != null) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(if(active != null) "Round In Progress" else if(late != null) "Late Round" else "Patrolling Progress", style = MaterialTheme.typography.titleMedium)
            Text(if(active != null) "Complete checkpoints to finish the current round." else "Pending rounds: $pending")
            if (rounds.isNotEmpty()) {
                val completed = rounds.count { it.status == "COMPLETED" }
                LinearProgressIndicator(progress = { completed.toFloat()/rounds.size.coerceAtLeast(1) }, Modifier.fillMaxWidth())
                Text("$completed / ${rounds.size} rounds completed", fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun ShiftSelectionDialog(onDismiss:()->Unit,onSelected:()->Unit){
    val context=LocalContext.current
    val app=context.applicationContext as SecurityGuardApplication
    val manager=remember{ShiftManager(app.database)}
    var shifts by remember{mutableStateOf<List<ShiftEntity>>(emptyList())}
    var selectedId by remember{mutableStateOf<Long?>(null)}
    var error by remember{mutableStateOf<String?>(null)}
    val scope=rememberCoroutineScope()
    LaunchedEffect(Unit){shifts=manager.activeShifts();selectedId=manager.selectedShiftId()}
    AlertDialog(onDismissRequest=onDismiss,title={Text("Select Today’s Shift")},text={
        Column(verticalArrangement=Arrangement.spacedBy(8.dp)){
            Text("Shift selection is separate from Check-In.")
            shifts.forEach{shift->Card(onClick={selectedId=shift.id},modifier=Modifier.fillMaxWidth()){
                Row(Modifier.padding(14.dp),verticalAlignment=Alignment.CenterVertically){RadioButton(selected=selectedId==shift.id,onClick={selectedId=shift.id});Column{Text(shift.name,fontSize=17.sp);Text("${shift.startTime}–${shift.endTime} • ${if(shift.roundsEnabled)"Rounds ON" else "Rounds OFF"}",fontSize=13.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)}}
            }}
            if(error!=null)Text(error!!,color=MaterialTheme.colorScheme.error)
        }
    },confirmButton={Button(onClick={scope.launch{try{manager.selectTodayShift(selectedId?:error("Select a shift"));HapticManager.success();AudioManager.emit(AudioEvent.Success);onSelected()}catch(e:Exception){error=e.message}}},enabled=selectedId!=null){Text("Select")}},dismissButton={TextButton(onClick=onDismiss){Text("Cancel")}})
}

@Composable
fun More(onLock:()->Unit,onRefresh:()->Unit){
    val context=LocalContext.current
    val app=context.applicationContext as SecurityGuardApplication
    val manager=remember{ShiftManager(app.database)}
    var showAdd by remember{mutableStateOf(false)}
    var shifts by remember{mutableStateOf<List<ShiftEntity>>(emptyList())}
    val scope=rememberCoroutineScope()
    LaunchedEffect(showAdd){shifts=manager.activeShifts()}
    LazyColumn(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(2.dp)){
        item{Text("More",fontSize=28.sp)}
        item{CardItem("Shift Management","Day / Night / Overtime / Custom","⚙",onClick={showAdd=true})}
        item{Text("Active Shift Profiles",fontSize=20.sp,modifier=Modifier.padding(top=12.dp,bottom=4.dp))}
        items(shifts.size){i->val sh=shifts[i];CardItem(sh.name,"${sh.startTime}–${sh.endTime} • ${if(sh.roundsEnabled)"Rounds ON" else "Rounds OFF"}",if(sh.type=="NIGHT")"🌙" else if(sh.type=="DAY")"☀" else "⏱")}
        item{CardItem("Checkpoint Settings","Assign checkpoints per shift","✓")}
        item{CardItem("Reports & Export","PDF • Excel • CSV","▤")}
        item{CardItem("Backup & Restore","Protected .sgback workflow","☁")}
        item{CardItem("Security / PIN","App Lock • Auto Lock • Biometric","🔒")}
        item{CardItem("Audio & Animation","BGM • SFX • Haptic • Motion","♪")}
        item{Button(onClick=onLock){Text("Lock App Now")}}
    }
    if(showAdd) AddShiftDialog(onDismiss={showAdd=false},onSaved={showAdd=false;onRefresh()})
}

@Composable
private fun AddShiftDialog(onDismiss:()->Unit,onSaved:()->Unit){
    val context=LocalContext.current
    val app=context.applicationContext as SecurityGuardApplication
    val manager=remember{ShiftManager(app.database)}
    var name by remember{mutableStateOf("")};var start by remember{mutableStateOf("09:00")};var end by remember{mutableStateOf("17:00")}
    var type by remember{mutableStateOf("CUSTOM")};var rounds by remember{mutableStateOf(false)};var cross by remember{mutableStateOf(false)};var error by remember{mutableStateOf<String?>(null)}
    val scope=rememberCoroutineScope()
    AlertDialog(onDismissRequest=onDismiss,title={Text("Add Shift")},text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)){
        Field("Shift name *",name,{name=it});Field("Start (HH:MM)",start,{start=it});Field("End (HH:MM)",end,{end=it});
        Text("Type");Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){listOf("DAY","NIGHT","OVERTIME","CUSTOM").forEach{FilterChip(selected=type==it,onClick={type=it},label={Text(it)})}}
        Row(verticalAlignment=Alignment.CenterVertically){Text("Rounds enabled",Modifier.weight(1f));Switch(rounds,{rounds=it})}
        Row(verticalAlignment=Alignment.CenterVertically){Text("Cross midnight",Modifier.weight(1f));Switch(cross,{cross=it})}
        if(error!=null)Text(error!!,color=MaterialTheme.colorScheme.error)
    }},confirmButton={Button(onClick={scope.launch{try{manager.addShift(name,type,start,end,cross,rounds);HapticManager.success();AudioManager.emit(AudioEvent.Success);onSaved()}catch(e:Exception){error=e.message?:"Invalid shift"}}},enabled=name.isNotBlank()){Text("Save")}},dismissButton={TextButton(onClick=onDismiss){Text("Cancel")}})
}

@Composable fun CardItem(title:String,sub:String,icon:String,onClick:()->Unit={}){Card(Modifier.fillMaxWidth().padding(vertical=6.dp),onClick=onClick){Row(Modifier.padding(18.dp),verticalAlignment=Alignment.CenterVertically){Text(icon,fontSize=28.sp);Spacer(Modifier.width(14.dp));Column{Text(title,fontSize=18.sp);Text(sub,fontSize=14.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)}}}}
@Composable
fun Attendance(){
    val context = LocalContext.current
    val app = context.applicationContext as SecurityGuardApplication
    val scope = rememberCoroutineScope()
    var refresh by remember { mutableIntStateOf(0) }
    var records by remember(refresh) { mutableStateOf<List<AttendanceEntity>>(emptyList()) }
    var sessions by remember(refresh) { mutableStateOf<List<ShiftSessionEntity>>(emptyList()) }
    var showManual by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val today = LocalDate.now()
    val monthStart = today.withDayOfMonth(1).toString()
    val monthEnd = today.withDayOfMonth(today.lengthOfMonth()).toString()

    LaunchedEffect(refresh) {
        records = withContext(Dispatchers.IO) { app.database.attendanceDao().range(monthStart, monthEnd) }
        sessions = withContext(Dispatchers.IO) {
            app.database.shiftSessionDao().forDate(today.toString())
        }
    }

    val statusIcon: (String) -> String = { when(it) {
        "PRESENT" -> "✓"; "ABSENT" -> "!"; "PAID_LEAVE" -> "P"; "UNPAID_LEAVE" -> "U"
        "HALF_DAY" -> "½"; "WEEKLY_OFF" -> "W"; else -> "•"
    }}

    LazyColumn(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text("Attendance", fontSize = 28.sp)
            Text("Shift-session based attendance • $monthStart to $monthEnd",
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Today", fontSize = 20.sp)
                    if (sessions.isEmpty()) {
                        Text("No shift session recorded today.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        sessions.forEach { session ->
                            val shift = app.database.shiftDao().get(session.shiftId)
                            Text("${shift?.name ?: "Shift"} • ${session.sessionType} • ${session.status}")
                            Text("Check-in: ${formatDateTime(session.actualCheckIn)}   Check-out: ${formatDateTime(session.actualCheckOut)}",
                                fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
        item {
            Button(onClick = { showManual = true }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Edit, null); Spacer(Modifier.width(8.dp)); Text("+ Add Attendance")
            }
        }
        item { Text("This Month", fontSize = 20.sp) }
        if (records.isEmpty()) {
            item { CardItem("No attendance records", "Records will appear here after Check-In or manual entry.", "📅") }
        } else {
            items(records, key = { it.id }) { record ->
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(statusIcon(record.status), fontSize = 24.sp)
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(record.attendanceDate, fontSize = 17.sp)
                            Text(record.status.replace('_',' ') + " • " + record.source,
                                color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                            record.note?.takeIf { it.isNotBlank() }?.let {
                                Text(it, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }
        item {
            Text("Attendance rules", fontSize = 20.sp)
            Text("Present is linked to a shift session. Weekly Off is separate from Absent. Leave can be Paid or Unpaid. Half Day is an explicit status.",
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        message?.let { item { Text(it, color = MaterialTheme.colorScheme.primary) } }
    }

    if (showManual) {
        ManualAttendanceDialog(
            onDismiss = { showManual = false },
            onSaved = { text ->
                showManual = false
                message = text
                refresh++
                HapticManager.success()
                AudioManager.emit(AudioEvent.Success)
            }
        )
    }
}

@Composable
private fun ManualAttendanceDialog(onDismiss: () -> Unit, onSaved: (String) -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as SecurityGuardApplication
    val scope = rememberCoroutineScope()
    var date by remember { mutableStateOf(LocalDate.now().toString()) }
    var status by remember { mutableStateOf("PRESENT") }
    var note by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val statuses = listOf("PRESENT", "ABSENT", "PAID_LEAVE", "UNPAID_LEAVE", "HALF_DAY", "WEEKLY_OFF")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Attendance") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Field("Date (YYYY-MM-DD)", date, { date = it })
                Text("Status")
                LazyColumn(Modifier.heightIn(max = 190.dp)) {
                    items(statuses) { item ->
                        FilterChip(
                            selected = status == item,
                            onClick = { status = item; error = null },
                            label = { Text(item.replace('_',' ')) },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                        )
                    }
                }
                Field("Reason / Note", note, { note = it })
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val parsed = runCatching { LocalDate.parse(date) }.getOrNull()
                if (parsed == null) { error = "Enter a valid date."; return@TextButton }
                scope.launch {
                    try {
                        val now = System.currentTimeMillis()
                        val id = withContext(Dispatchers.IO) {
                            app.database.attendanceDao().insert(
                                AttendanceEntity(
                                    shiftSessionId = null,
                                    attendanceDate = parsed.toString(),
                                    status = status,
                                    source = "MANUAL",
                                    note = note.ifBlank { null },
                                    createdAt = now,
                                    updatedAt = now
                                )
                            )
                        }
                        if (id > 0) onSaved("Manual attendance saved with reason/source recorded.")
                    } catch (e: Exception) {
                        error = e.message ?: "Unable to save attendance."
                        HapticManager.error(); AudioManager.emit(AudioEvent.Error)
                    }
                }
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun Rounds(){
    val context = LocalContext.current
    val app = context.applicationContext as SecurityGuardApplication
    val shiftManager = remember { ShiftManager(app.database) }
    val roundRepo = remember { RoundRepository(app.database.roundDao(), app.database.roundCheckpointDao(), app.database.checkpointDao(), app.database.shiftCheckpointDao()) }
    var rounds by remember { mutableStateOf<List<RoundEntity>>(emptyList()) }
    var checkpoints by remember { mutableStateOf<List<RoundCheckpointEntity>>(emptyList()) }
    var activeSession by remember { mutableStateOf<ShiftSessionEntity?>(null) }
    var activeRound by remember { mutableStateOf<RoundEntity?>(null) }
    var selectedShift by remember { mutableStateOf<ShiftEntity?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    suspend fun refresh(){
        activeSession = shiftManager.activeSession()
        selectedShift = shiftManager.selectedShiftId()?.let { app.database.shiftDao().get(it) }
        val session = activeSession
        val shift = selectedShift
        if (session != null && shift?.roundsEnabled == true) {
            roundRepo.syncStatuses(session, shift)
            rounds = app.database.roundDao().forSession(session.id)
            activeRound = app.database.roundDao().active()?.takeIf { it.shiftSessionId == session.id }
            checkpoints = activeRound?.let { app.database.roundCheckpointDao().forRound(it.id) } ?: emptyList()
        } else {
            rounds = emptyList(); checkpoints = emptyList(); activeRound = null
        }
    }

    LaunchedEffect(Unit) {
        refresh()
        while (true) { delay(15_000); refresh() }
    }

    fun startRound(round: RoundEntity){
        scope.launch {
            busy = true
            try {
                roundRepo.start(round.id)
                message = "Round started — late alert stopped."
                HapticManager.click(); AudioManager.emit(AudioEvent.RoundStart)
                refresh()
            } catch(e: Exception) { message = e.message ?: "Unable to start round"; HapticManager.error(); AudioManager.emit(AudioEvent.Error) }
            busy = false
        }
    }

    LazyColumn(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text("Rounds & Patrolling", fontSize = 28.sp)
            Text("Only enabled for the selected shift.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (activeSession == null) {
            item { CardItem("No active shift", "Check-In with a round-enabled shift to start patrolling.", "🛡") }
        } else if (selectedShift?.roundsEnabled != true) {
            item { CardItem("Not Applicable", "Rounds are OFF for this shift.", "✓") }
        } else {
            val current = activeRound
            if (current != null) {
                item {
                    Card(
                        Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("ACTIVE ROUND", style = MaterialTheme.typography.labelLarge)
                            Text("Started ${formatTime(current.actualStart)}", fontSize = 20.sp)
                            val done = checkpoints.count { it.status == "COMPLETED" }
                            Text("Checkpoints $done / ${checkpoints.size}")
                            LinearProgressIndicator(
                                progress = { if (checkpoints.isEmpty()) 1f else done.toFloat()/checkpoints.size },
                                Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
                item { Text("Checkpoints", fontSize = 20.sp) }
                items(checkpoints, key={it.id}) { cp ->
                    val completed = cp.status == "COMPLETED"
                    Card(onClick = {
                        if (completed || busy) return@Card
                        scope.launch {
                            busy = true
                            try {
                                roundRepo.completeCheckpoint(current.id, cp.checkpointId)
                                HapticManager.click(); AudioManager.emit(AudioEvent.CheckpointComplete)
                                refresh()
                            } catch(e: Exception) { message=e.message ?: "Unable to complete checkpoint"; HapticManager.error(); AudioManager.emit(AudioEvent.Error) }
                            busy = false
                        }
                    }, Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(16.dp), verticalAlignment=Alignment.CenterVertically) {
                            Icon(if(completed) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked, null)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(cp.checkpointNameSnapshot, fontSize=17.sp)
                                Text(if(completed) "Completed • ${formatTime(cp.completedAt)}" else "Tap to complete", fontSize=13.sp, color=MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
                item {
                    val allDone = checkpoints.isNotEmpty() && checkpoints.all { it.status == "COMPLETED" }
                    SlideActionButton(label = if(allDone) "Slide to Complete" else "Complete all checkpoints to finish", enabled = allDone && !busy) {
                        scope.launch {
                            busy=true
                            try { roundRepo.complete(current.id); message="Round completed successfully"; HapticManager.success(); AudioManager.emit(AudioEvent.RoundComplete); refresh() }
                            catch(e:Exception){ message=e.message ?: "Complete all checkpoints first"; HapticManager.error(); AudioManager.emit(AudioEvent.Error) }
                            busy=false
                        }
                    }
                }
            } else {
                val now = System.currentTimeMillis()
                val next = rounds.firstOrNull { it.status == "SCHEDULED" || it.status == "LATE" }
                if(next == null) item { CardItem("No pending rounds", "All scheduled rounds are completed or missed.", "✓") }
                else {
                    val isLate = next.status == "LATE"
                    item {
                        Card(
                            Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = if(isLate) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(Modifier.padding(18.dp), verticalArrangement=Arrangement.spacedBy(7.dp)) {
                                Text(if(isLate) "LATE ROUND" else "NEXT ROUND", style=MaterialTheme.typography.labelLarge)
                                Text(formatDateTime(next.scheduledTime), fontSize=21.sp)
                                Text(if(isLate) "Late alert is active until you start this round." else "No alert at due time. Alert starts only after the configured late threshold.", color=MaterialTheme.colorScheme.onSurfaceVariant)
                                SlideActionButton(label = "Slide to Start Round", enabled = !busy) { startRound(next) }
                            }
                        }
                    }
                }
                item {
                    Text("Round History", fontSize=20.sp)
                    rounds.filter { it.status == "COMPLETED" || it.status == "MISSED" }.takeLast(8).reversed().forEach { r ->
                        CardItem("${r.status} • ${formatDateTime(r.scheduledTime)}", if(r.status=="COMPLETED") "Duration ${r.durationSeconds ?: 0}s" else "This round cannot be started now", if(r.status=="COMPLETED") "✓" else "!")
                    }
                }
            }
        }
        if(message != null) item { Text(message!!, color=MaterialTheme.colorScheme.primary) }
        item { OutlinedButton(onClick={AudioManager.emit(AudioEvent.RoundStart);HapticManager.click()}, enabled=!busy){Text("Test Round Start SFX")} }
    }
}

@Composable
private fun SlideActionButton(label: String, enabled: Boolean, onConfirmed: () -> Unit) {
    var value by remember(label) { mutableFloatStateOf(0f) }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        Slider(value = value, onValueChange = { value = it }, enabled = enabled, onValueChangeFinished = {
            if (value >= 0.92f) { value = 0f; onConfirmed() }
        }, valueRange = 0f..1f)
        Text(if(enabled) "Slide fully to confirm" else "Action unavailable", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun formatTime(epoch: Long?): String = epoch?.let {
    LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(it), ZoneId.systemDefault()).toLocalTime().withSecond(0).toString()
} ?: "—"

private fun formatDateTime(epoch: Long?): String = epoch?.let { LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(it), ZoneId.systemDefault()).format(java.time.format.DateTimeFormatter.ofPattern("dd MMM • HH:mm")) } ?: "—"

private fun formatDateTime(epoch: Long): String = LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(epoch), ZoneId.systemDefault())
    .format(java.time.format.DateTimeFormatter.ofPattern("dd MMM • HH:mm"))
@Composable
fun Salary(){
    val context=LocalContext.current
    val app=context.applicationContext as SecurityGuardApplication
    val scope=rememberCoroutineScope()
    val today=LocalDate.now()
    val start=today.withDayOfMonth(1)
    val end=today.withDayOfMonth(today.lengthOfMonth())
    var result by remember{mutableStateOf<com.securityguard.app.feature.payroll.PayrollCalculator.Result?>(null)}
    var payroll by remember{mutableStateOf<PayrollEntity?>(null)}
    var message by remember{mutableStateOf<String?>(null)}
    var busy by remember{mutableStateOf(false)}
    LaunchedEffect(Unit){
        payroll=withContext(Dispatchers.IO){app.database.payrollDao().find(start.toString(),end.toString())}
    }
    fun money(v:Long)=String.format("₹%,.2f",v/100.0)
    LazyColumn(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
        item{Text("Salary & Payroll",fontSize=28.sp);Text("${start} → ${end}",color=MaterialTheme.colorScheme.onSurfaceVariant)}
        item{Button(onClick={scope.launch{busy=true;message=null;try{result=withContext(Dispatchers.IO){com.securityguard.app.feature.payroll.PayrollCalculator(app.database).calculate(start,end)};message="Payroll calculation refreshed";HapticManager.success();AudioManager.emit(AudioEvent.Success)}catch(e:Exception){message=e.message?:"Unable to calculate payroll";HapticManager.error();AudioManager.emit(AudioEvent.Error)};busy=false}},enabled=!busy,modifier=Modifier.fillMaxWidth()){Text(if(busy)"Calculating…" else "Calculate Payroll")}}
        result?.let{r->
            item{Card(Modifier.fillMaxWidth()){Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(7.dp)){
                Text("Payroll Summary",fontSize=20.sp)
                Text("Basic Salary  ${money(r.basic)}")
                Text("Regular Pay   ${money(r.regular)}")
                Text("Overtime      ${money(r.overtime)}")
                Text("Bonus         ${money(r.bonus)}")
                Text("Allowance     ${money(r.allowance)}")
                Text("Advance       -${money(r.advance)}")
                Text("Deduction     -${money(r.deduction)}")
                HorizontalDivider()
                Text("Gross Salary  ${money(r.gross)}",fontSize=18.sp)
                Text("Net Salary    ${money(r.net)}",fontSize=22.sp,fontWeight=FontWeight.Bold)
            }}}
            item{Button(onClick={scope.launch{
                val now=System.currentTimeMillis();val rr=r
                withContext(Dispatchers.IO){
                    val existing=app.database.payrollDao().find(start.toString(),end.toString())
                    if(existing==null){
                        val id=app.database.payrollDao().insert(PayrollEntity(periodStart=start.toString(),periodEnd=end.toString(),basicSalaryPaise=rr.basic,regularPayPaise=rr.regular,overtimePayPaise=rr.overtime,bonusPaise=rr.bonus,allowancePaise=rr.allowance,advancePaise=rr.advance,deductionPaise=rr.deduction,grossSalaryPaise=rr.gross,netSalaryPaise=rr.net,status="DRAFT",paymentStatus="UNPAID",paymentDate=null,paymentMode=null,paymentNote=null,createdAt=now,updatedAt=now))
                        app.database.payrollVersionDao().insert(PayrollVersionEntity(payrollId=id,versionNumber=1,calculatedAt=now,grossSalaryPaise=rr.gross,netSalaryPaise=rr.net,reason="Initial calculation",createdAt=now))
                        app.database.auditLogDao().insert(AuditLogEntity(action="CREATE",entityType="PAYROLL",entityId=id.toString(),oldValue=null,newValue="DRAFT",reason="Initial payroll calculation",createdAt=now))
                    }
                }; payroll=withContext(Dispatchers.IO){app.database.payrollDao().find(start.toString(),end.toString())};message="Payroll saved as DRAFT";HapticManager.success();AudioManager.emit(AudioEvent.Success)
            }},modifier=Modifier.fillMaxWidth()){Text("Save as DRAFT")}}
        }
        payroll?.let{p->item{CardItem("Payroll Status","${p.status} • ${p.paymentStatus} • Net ${money(p.netSalaryPaise)}","▣")}}
        message?.let{item{Text(it,color=MaterialTheme.colorScheme.primary)}}
        item{CardItem("Overtime","Calculated separately from OVERTIME shift sessions","⏱")}
        item{CardItem("Workflow","DRAFT → REVIEW → FINAL → CLOSED • Reopen requires reason","🔒")}
    }
}
