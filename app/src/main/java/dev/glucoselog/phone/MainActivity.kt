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
 private lateinit var syncButton:Button
 private var form:EntryForm?=null
 private var draft:Bundle?=null
 private var editing:Entry?=null
 private var tab="log"
 private var syncing=false
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
  tab=savedInstanceState?.getString("tab") ?: "log"; draft=savedInstanceState?.getBundle("draft")
  val editId=savedInstanceState?.getString("editing")
  if(editId!=null) { editing=store.all().find { it.id==editId }; if(editing==null) draft=null }
  val root=Ui.column(this).apply { setBackgroundColor(Color.WHITE); setPadding(Ui.dp(this@MainActivity,20),0,Ui.dp(this@MainActivity,20),0) }
  if(android.os.Build.VERSION.SDK_INT>=30) root.setOnApplyWindowInsetsListener { v,insets ->
   val system=insets.getInsets(android.view.WindowInsets.Type.systemBars() or android.view.WindowInsets.Type.ime())
   v.setPadding(Ui.dp(this,20)+system.left,system.top,Ui.dp(this,20)+system.right,system.bottom); insets
  }
  // API 28/29 use the legacy inset fields.
  if(android.os.Build.VERSION.SDK_INT<30) root.setOnApplyWindowInsetsListener { v,insets ->
   @Suppress("DEPRECATION")
   v.setPadding(Ui.dp(this,20)+insets.systemWindowInsetLeft,insets.systemWindowInsetTop,Ui.dp(this,20)+insets.systemWindowInsetRight,insets.systemWindowInsetBottom); insets
  }
  root.addView(Ui.text(this,"Glucose Log",30f,true))
  val navigation=LinearLayout(this)
  fun nav(label:String,which:String) { navigation.addView(Ui.button(this,label) { if(!saving) { if(tab=="log") draft=form?.snapshot(); tab=which; render() } },LinearLayout.LayoutParams(0,Ui.dp(this,52),1f).apply { marginEnd=Ui.dp(this@MainActivity,6) }) }
  nav("Log","log"); nav("History","history")
  navigation.addView(Ui.button(this,"Settings") { settings() },LinearLayout.LayoutParams(0,Ui.dp(this,52),1f)); root.addView(navigation)
  status=Ui.text(this,"Readings save on this phone first.",14f).apply { setTextColor(Ui.muted) }; root.addView(status)
  syncButton=Ui.button(this,"Sync to Health Connect") { connectOrSync() }; root.addView(syncButton)
  body=Ui.column(this)
  root.addView(ScrollView(this).apply { isFillViewport=true; addView(body) },LinearLayout.LayoutParams(-1,0,1f))
  setContentView(root); root.requestApplyInsets(); render()
  if(!SyncJob.schedule(this)) status.text="Automatic retry is unavailable. Use Sync to Health Connect."
 }
 override fun onResume() { super.onResume(); if(::status.isInitialized) sync() }
 override fun onSaveInstanceState(out:Bundle) {
  out.putString("tab",tab); out.putBundle("draft",if(tab=="log") form?.snapshot() else draft); out.putString("editing",editing?.id); super.onSaveInstanceState(out)
 }
 private fun render() {
  body.removeAllViews()
  if(tab=="log") {
   form=EntryForm(this,editing,draft,preferredUnit) { save(it) }; body.addView(form)
   if(editing!=null) body.addView(Ui.button(this,"Cancel edit") { editing=null; draft=null; render() })
  } else { form=null; history() }
 }
 private fun save(e:Entry) {
  if(saving) return
  saving=true; form?.saveButton?.isEnabled=false
  lifecycleScope.launch {
   try {
    withContext(Dispatchers.IO) { store.save(e) }
    preferredUnit=e.unit; editing=null; draft=null
    status.text="Saved on this phone. Sending to Health Connect…"
    Toast.makeText(this@MainActivity,"Reading saved",Toast.LENGTH_SHORT).show()
    render(); sync()
   } catch(ex:CancellationException) { throw ex }
   catch(ex:Exception) { AlertDialog.Builder(this@MainActivity).setTitle("Reading wasn't saved").setMessage(ex.message ?: "Please try again.").setPositiveButton("OK",null).show() }
   finally { saving=false; form?.saveButton?.isEnabled=true }
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
  if(syncing) return
  syncing=true; syncButton.isEnabled=false
  lifecycleScope.launch {
   try {
    val result=withTimeout(60_000) { HealthSync.run(applicationContext) }
    status.text=when {
     result.error!=null -> if(result.remaining>0) "${result.remaining} changes waiting. ${result.error}" else result.error
     result.remaining>0 -> "${result.remaining} changes waiting. Tap Sync to retry."
     else -> "Health Connect is up to date. Cronometer imports on its own schedule."
    }
    if(tab=="history") render()
   } catch(ex:TimeoutCancellationException) { status.text="Sync timed out. Your readings are saved here; retry when ready." }
   catch(ex:CancellationException) { throw ex }
   catch(ex:Exception) { status.text="Sync could not finish. Please retry." }
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
  AlertDialog.Builder(this).setTitle("Settings").setItems(arrayOf("Display units: ${preferredUnit.label}","Health Connect permissions","Privacy")) { _,which ->
   when(which) {
    0 -> AlertDialog.Builder(this).setTitle("Display units").setSingleChoiceItems(arrayOf("mg/dL","mmol/L"),preferredUnit.ordinal) { dialog,i -> preferredUnit=GlucoseUnit.entries[i]; dialog.dismiss(); if(tab=="history") render(); Toast.makeText(this,"Display unit saved. Existing draft keeps its selected unit.",Toast.LENGTH_LONG).show() }.setNegativeButton("Cancel",null).show()
    1 -> try { startActivity(Intent(HealthConnectClient.ACTION_HEALTH_CONNECT_SETTINGS)) } catch(_:Exception) { Toast.makeText(this,"Health Connect isn't available.",Toast.LENGTH_LONG).show() }
    2 -> startActivity(Intent(this,RationaleActivity::class.java))
   }
  }.show()
 }
}
