package dev.glucoselog.phone
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.ComponentActivity
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.*
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@SuppressLint("SetTextI18n")
class MainActivity:ComponentActivity() {
 private lateinit var store:EntryStore
 private lateinit var body:LinearLayout
 private lateinit var status:TextView
 private lateinit var savedNotice:TextView
 private var lastSaved=""
 private lateinit var syncButton:Button
 private var form:EntryForm?=null
 private var draft:Bundle?=null
 private var editing:Entry?=null
 private var tab="log"
 private var syncing=false
 private var syncAgain=false
 private val notices=SyncNotices()
 private val navButtons=mutableMapOf<String,Button>()
 private var saving=false
 private val prefs by lazy { getSharedPreferences("settings",MODE_PRIVATE) }
 private var preferredUnit:GlucoseUnit
  get()=GlucoseUnit.valueOf(prefs.getString("unit","MG") ?: "MG")
  set(value) { prefs.edit().putString("unit",value.name).apply() }
 private val permissionRequest=registerForActivityResult(PermissionController.createRequestPermissionResultContract()) { granted ->
  if(granted.containsAll(HealthSync.permissions)) sync() else status.text="Saved locally. Allow blood glucose access when you're ready to sync."
 }
 override fun onCreate(savedInstanceState:Bundle?) {
  super.onCreate(savedInstanceState)
  store=EntryStore.get(this)
  lastSaved=savedInstanceState?.getString("lastSaved") ?: ""
  repeat(savedInstanceState?.getInt("noticeCount") ?: 0) { i ->
   savedInstanceState?.getBundle("notice$i")?.let { n ->
    val id=n.getString("id") ?: return@let
    val unit=GlucoseUnit.valueOf(n.getString("unit") ?: "MG")
    notices.add(Entry(id,n.getDouble("value"),unit,1,0,0,"",n.getLong("revision"),false,false))
   }
  }
  tab=savedInstanceState?.getString("tab") ?: "log"; draft=savedInstanceState?.getBundle("draft")
  val editId=savedInstanceState?.getString("editing")
  if(editId!=null) { editing=store.all().find { it.id==editId }; if(editing==null) draft=null }
  val root=Ui.column(this).apply { setBackgroundColor(Color.WHITE); setPadding(Ui.dp(this@MainActivity,20),0,Ui.dp(this@MainActivity,20),0) }
  androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(root) { v,insets ->
   val system=insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars() or androidx.core.view.WindowInsetsCompat.Type.ime())
   v.setPadding(Ui.dp(this,20)+system.left,system.top,Ui.dp(this,20)+system.right,system.bottom)
   form?.keyboardVisibilityChanged(insets.isVisible(androidx.core.view.WindowInsetsCompat.Type.ime()),lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED) && window.decorView.hasWindowFocus())
   insets
  }
  val heading=LinearLayout(this).apply { gravity=android.view.Gravity.CENTER_VERTICAL; setPadding(0,Ui.dp(this@MainActivity,8),0,Ui.dp(this@MainActivity,8)) }
  heading.addView(Ui.text(this,"Glucose Log",28f,true),LinearLayout.LayoutParams(0,-2,1f))
  heading.addView(Ui.button(this,"Settings") { settings() },LinearLayout.LayoutParams(-2,Ui.dp(this,48)))
  root.addView(heading)
  val navigation=LinearLayout(this)
  fun nav(label:String,which:String) {
   val button=Ui.button(this,label) { if(!saving) { if(tab=="log") draft=form?.snapshot(); tab=which; render() } }
   navButtons[which]=button
   navigation.addView(button,LinearLayout.LayoutParams(0,Ui.dp(this,54),1f).apply { marginEnd=Ui.dp(this@MainActivity,4); marginStart=Ui.dp(this@MainActivity,4); topMargin=Ui.dp(this@MainActivity,8); bottomMargin=Ui.dp(this@MainActivity,8) })
  }
  nav("New reading","log"); nav("History","history")
  status=Ui.text(this,"Checking Health Connect…",14f).apply { setTextColor(Ui.muted) }; root.addView(status)
  syncButton=Ui.button(this,"Connect Health Connect") { connectOrSync() }.apply { visibility=View.GONE }; root.addView(syncButton)
  savedNotice=Ui.text(this,lastSaved,15f,true).apply { setTextColor(Ui.teal); visibility=if(lastSaved.isEmpty()) View.GONE else View.VISIBLE }; root.addView(savedNotice)
  body=Ui.column(this)
  root.addView(ScrollView(this).apply { isFillViewport=true; addView(body) },LinearLayout.LayoutParams(-1,0,1f))
  root.addView(navigation)
  setContentView(root); root.requestApplyInsets(); render()
  if(!SyncJob.schedule(this)) status.text="Automatic retry is unavailable. Use Sync to Health Connect."
 }
 override fun onResume() { super.onResume(); if(::status.isInitialized) sync() }
 override fun onPause() { form?.keyboardVisibilityChanged(false,false); super.onPause() }
 override fun onWindowFocusChanged(hasFocus:Boolean) {
  super.onWindowFocusChanged(hasFocus)
  if(!hasFocus) form?.keyboardVisibilityChanged(false,false) else window.decorView.requestApplyInsets()
 }
 override fun onSaveInstanceState(out:Bundle) {
  out.putString("lastSaved",lastSaved)
  val pending=notices.pending(); out.putInt("noticeCount",pending.size)
  pending.forEachIndexed { i,e -> out.putBundle("notice$i",Bundle().apply { putString("id",e.id); putDouble("value",e.value); putString("unit",e.unit.name); putLong("revision",e.revision) }) }
  out.putString("tab",tab); out.putBundle("draft",if(tab=="log") form?.snapshot() else draft); out.putString("editing",editing?.id); super.onSaveInstanceState(out)
 }
 private fun render() {
  navButtons.forEach { (which,button) -> button.background=Ui.background(if(which==tab) Ui.teal else Ui.pale,Ui.dp(this,14).toFloat()); button.setTextColor(if(which==tab) Color.WHITE else Ui.ink) }
  body.removeAllViews()
  if(tab=="log") {
   form=EntryForm(this,editing,draft,preferredUnit) { save(it) }; body.addView(form)
   if(editing!=null) body.addView(Ui.button(this,"Cancel edit") { editing=null; draft=null; render() })
  } else { form=null; history() }
 }
 private fun save(e:Entry) {
  if(saving) return
  saving=true; form?.setSaving(true)
  lifecycleScope.launch {
   try {
    val saved=withContext(Dispatchers.IO) { store.save(e) }
    notices.add(saved)
    lastSaved="Last saved: ${saved.unit.format(saved.value)} ${saved.unit.label} · ${java.time.LocalTime.now().format(DateTimeFormatter.ofPattern("h:mm a"))}"
    savedNotice.text=lastSaved; savedNotice.visibility=View.VISIBLE
    preferredUnit=e.unit; editing=null; draft=null
    status.text="Saved on this phone. Sending to Health Connect…"
    (getSystemService(INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager).hideSoftInputFromWindow(form?.windowToken,0)
    currentFocus?.clearFocus()
    render(); sync()
   } catch(ex:CancellationException) { throw ex }
   catch(ex:Exception) { AlertDialog.Builder(this@MainActivity).setTitle("Reading wasn't saved").setMessage(ex.message ?: "Please try again.").setPositiveButton("OK",null).show() }
   finally { saving=false; form?.setSaving(false) }
  }
 }
 private fun history() {
  val readings=store.all(); val unit=preferredUnit
  body.addView(Ui.text(this,"Past 7 days",24f,true))
  body.addView(Ui.text(this,"${unit.label} · individual readings",14f).apply { setTextColor(Ui.muted) })
  body.addView(ChartView(this,readings,unit),LinearLayout.LayoutParams(-1,Ui.dp(this,220)))
  body.addView(Ui.text(this,"All readings",22f,true))
  if(readings.isEmpty()) { body.addView(Ui.text(this,"No readings yet. Open Log to save your first one.")); return }
  val format=DateTimeFormatter.ofPattern("EEE, MMM d, yyyy · h:mm a").withZone(ZoneId.systemDefault())
  readings.forEach { e ->
   val group=Ui.column(this).apply { setPadding(0,Ui.dp(this@MainActivity,12),0,Ui.dp(this@MainActivity,12)) }
   group.addView(Ui.text(this,"${unit.format(unit.fromMg(e.unit.toMg(e.value)))} ${unit.label}",26f,true))
   group.addView(Ui.text(this,format.format(Instant.ofEpochSecond(e.time)),14f))
   group.addView(Ui.text(this,Entry.contexts[e.context]+" · "+if(e.synced) "Sent to Health Connect" else "Waiting to sync",14f).apply { setTextColor(Ui.muted) })
   if(e.note.isNotBlank()) group.addView(Ui.text(this,e.note))
   val row=LinearLayout(this)
   row.addView(Ui.button(this,"Edit") { editing=e; draft=null; tab="log"; render() },LinearLayout.LayoutParams(0,-2,1f).apply { marginEnd=Ui.dp(this@MainActivity,8) })
   row.addView(Ui.button(this,"Delete") { confirmDelete(e) },LinearLayout.LayoutParams(0,-2,1f)); group.addView(row)
   body.addView(group); body.addView(View(this).apply { setBackgroundColor(Ui.pale) },LinearLayout.LayoutParams(-1,Ui.dp(this,2)))
  }
 }
 private fun confirmDelete(e:Entry) {
  AlertDialog.Builder(this).setTitle("Delete this reading?").setMessage("It will be removed from this phone and queued for removal from Health Connect. Cronometer may retain its imported copy.")
   .setNegativeButton("Keep",null).setPositiveButton("Delete") { _,_ ->
    lifecycleScope.launch { try {
     withContext(Dispatchers.IO) { store.delete(e.id) }; if(editing?.id==e.id) { editing=null; draft=null }; render(); sync()
    } catch(ex:CancellationException) { throw ex } catch(ex:Exception) { status.text="Couldn't delete the reading. Try again." } }
   }.show()
 }
 private fun sync() {
  if(syncing) { syncAgain=true; return }
  syncing=true; syncButton.isEnabled=false
  lifecycleScope.launch {
   try {
    do {
     syncAgain=false
     val result=withTimeout(60_000) { HealthSync.run(applicationContext) }
     val confirmations=notices.confirmed(withContext(Dispatchers.IO) { store.all() })
     if(confirmations.isNotEmpty()) Toast.makeText(this@MainActivity,if(confirmations.size==1) confirmations.single() else "${confirmations.size} readings sent to Health Connect",Toast.LENGTH_LONG).show()
     status.text=when {
      result.error!=null -> if(result.remaining>0) "${result.remaining} waiting to sync. ${result.error}" else result.error
      result.remaining>0 -> "${result.remaining} changes waiting to sync"
      else -> "✓  Synced with Health Connect"
     }
     status.setTextColor(if(result.error==null && result.remaining==0) Ui.teal else Ui.muted)
     syncButton.visibility=if(result.error!=null || result.remaining>0) View.VISIBLE else View.GONE
     syncButton.text=if(result.error?.contains("Connect Health Connect")==true) "Connect Health Connect" else "Retry sync"
     if(tab=="history") render()
    } while(syncAgain)
   } catch(ex:TimeoutCancellationException) { status.text="Saved on this phone. Sync will retry."; syncButton.visibility=View.VISIBLE }
   catch(ex:CancellationException) { throw ex }
   catch(ex:Exception) { status.text="Saved on this phone. Sync will retry."; syncButton.visibility=View.VISIBLE }
   finally { syncing=false; syncButton.isEnabled=true }
  }
 }
 private fun connectOrSync() {
  when(HealthConnectClient.getSdkStatus(this)) {
   HealthConnectClient.SDK_AVAILABLE -> permissionRequest.launch(HealthSync.permissions)
   else -> AlertDialog.Builder(this).setTitle("Health Connect unavailable").setMessage("Install or update Health Connect, then return here. You can keep logging locally.").setPositiveButton("OK",null).show()
  }
 }
 private fun settings() {
  AlertDialog.Builder(this).setTitle("Settings").setItems(arrayOf("Display units: ${preferredUnit.label}","Health Connect permissions","Retry pending sync","Privacy")) { _,which ->
   when(which) {
    0 -> AlertDialog.Builder(this).setTitle("Display units").setSingleChoiceItems(arrayOf("mg/dL","mmol/L"),preferredUnit.ordinal) { dialog,i -> preferredUnit=GlucoseUnit.entries[i]; dialog.dismiss(); if(tab=="history") render(); Toast.makeText(this,"Display unit saved. Existing draft keeps its selected unit.",Toast.LENGTH_LONG).show() }.setNegativeButton("Cancel",null).show()
    1 -> try { startActivity(Intent(HealthConnectClient.ACTION_HEALTH_CONNECT_SETTINGS)) } catch(_:Exception) { Toast.makeText(this,"Health Connect isn't available.",Toast.LENGTH_LONG).show() }
    2 -> sync()
    3 -> startActivity(Intent(this,RationaleActivity::class.java))
   }
  }.show()
 }
}
