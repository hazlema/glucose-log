package dev.glucoselog.phone
import android.app.Instrumentation
import android.content.Intent
import android.os.Bundle
import android.app.Activity
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import java.util.UUID

/** Uses a separate temporary database. Never calls Health Connect or saves through the UI. */
class DeviceChecks:Instrumentation() {
 override fun onCreate(arguments:Bundle?) { super.onCreate(arguments); start() }
 override fun onStart() {
  val name="checks-${UUID.randomUUID()}.db"
  var store=EntryStore(targetContext,name)
  var activity:Activity?=null
  try {
   val e=Entry("device-test",100.0,GlucoseUnit.MG,1700000000,-14400,0,"test-local-only",0,false,false)
   store.save(e); store.close(); store=EntryStore(targetContext,name)
   check(store.all().single().value==100.0) { "Local save didn't survive reopening" }
   check(store.pending().single().revision==1L)
   store.save(store.all().single().copy(value=110.0))
   store.acknowledge(e.id,1)
   check(!store.all().single().synced) { "Stale ack lost edit" }
   val oldEdit=store.all().single()
   store.delete(e.id); check(store.all().isEmpty()); check(store.pending().single().deleted)
   store.acknowledge(e.id,3); check(store.pending().isEmpty())
   var rejected=false
   try { store.save(oldEdit) } catch(_:IllegalArgumentException) { rejected=true }
   check(rejected) { "An old edit resurrected a deleted reading" }
   activity=startActivitySync(Intent(targetContext,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
   waitForIdleSync()
   runOnMainSync {
    fun texts(v:View):List<String> = (if(v is TextView) listOf(v.text.toString()) else emptyList()) + (if(v is ViewGroup) (0 until v.childCount).flatMap { texts(v.getChildAt(it)) } else emptyList())
    val labels=texts(activity!!.window.decorView)
    check(labels.contains("100") && labels.contains("Save reading") && labels.contains("mg/dL")) { "Default entry form did not render" }
   }
   finish(Activity.RESULT_OK,Bundle().apply { putString("result","PASS: durable save, versioned edit, stale ack, delete, stale edit rejection, native entry screen") })
  } catch(t:Throwable) { finish(Activity.RESULT_CANCELED,Bundle().apply { putString("failure",t.stackTraceToString()) }) }
  finally { activity?.let { a -> runOnMainSync { a.finish() } }; store.close(); targetContext.deleteDatabase(name) }
 }
}
