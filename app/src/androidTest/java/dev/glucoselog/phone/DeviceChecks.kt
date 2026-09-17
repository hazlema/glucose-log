package dev.glucoselog.phone
import android.app.Instrumentation
import android.content.Intent
import android.os.Bundle
import android.app.Activity
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import java.util.UUID

/** Uses a separate temporary database. Never writes synthetic readings to Health Connect. */
class DeviceChecks:Instrumentation() {
 private fun onUi(action:()->Unit) {
  var failure:Throwable?=null
  runOnMainSync { try { action() } catch(t:Throwable) { failure=t } }
  failure?.let { throw it }
 }
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
   onUi {
    fun texts(v:View):List<String> = (if(v is TextView) listOf(v.text.toString()) else emptyList()) + (if(v is ViewGroup) (0 until v.childCount).flatMap { texts(v.getChildAt(it)) } else emptyList())
    val labels=texts(activity!!.window.decorView)
    check(labels.contains("Glucose Log") && labels.contains("Done or closing the keyboard saves your changes")) { "Default entry form did not render" }
   }
   onUi {
    val decor=activity!!.window.decorView
    val bitmap=android.graphics.Bitmap.createBitmap(decor.width,decor.height,android.graphics.Bitmap.Config.ARGB_8888)
    decor.draw(android.graphics.Canvas(bitmap))
    java.io.File(targetContext.cacheDir,"entry-preview.png").outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it) }
    bitmap.recycle()
    val saved=mutableListOf<Entry>()
    val form=EntryForm(activity!!,null,null,GlucoseUnit.MG) { saved.add(it) }
    fun number(v:View):android.widget.EditText? {
     if(v is android.widget.EditText) return v
     if(v is ViewGroup) for(i in 0 until v.childCount) { val found=number(v.getChildAt(i)); if(found!=null) return found }
     return null
    }
    val field=number(form)!!
    val dismissals=mutableListOf<Entry>()
    val dismissForm=EntryForm(activity!!,null,null,GlucoseUnit.MG) { dismissals.add(it) }
    val dismissField=number(dismissForm)!!
    dismissForm.keyboardVisibilityChanged(true,true); dismissForm.keyboardVisibilityChanged(false,true)
    check(dismissals.isEmpty()) { "Untouched default saved on dismissal" }
    dismissField.setText("0"); dismissForm.keyboardVisibilityChanged(true,true); dismissForm.keyboardVisibilityChanged(false,true)
    check(dismissals.isEmpty() && dismissField.error!=null) { "Invalid dismissal wasn't kept as a draft" }
    dismissField.setText("135"); dismissForm.keyboardVisibilityChanged(true,true); dismissForm.keyboardVisibilityChanged(false,true)
    check(dismissals.size==1 && dismissals.single().value==135.0) { "Changed reading didn't save on dismissal" }
    dismissForm.keyboardVisibilityChanged(false,true)
    check(dismissals.size==1) { "Repeated hidden event duplicated save" }
    val afterDone=EntryForm(activity!!,null,null,GlucoseUnit.MG) { dismissals.add(it) }
    number(afterDone)!!.setText("140"); afterDone.keyboardVisibilityChanged(true,true)
    number(afterDone)!!.onEditorAction(android.view.inputmethod.EditorInfo.IME_ACTION_DONE)
    afterDone.keyboardVisibilityChanged(false,true)
    check(dismissals.size==2) { "Done followed by keyboard dismissal duplicated save" }
    val draftForm=EntryForm(activity!!,null,null,GlucoseUnit.MG) {}
    number(draftForm)!!.setText("145")
    val restoredForm=EntryForm(activity!!,null,draftForm.snapshot(),GlucoseUnit.MG) { dismissals.add(it) }
    restoredForm.keyboardVisibilityChanged(true,true); restoredForm.keyboardVisibilityChanged(false,true)
    check(dismissals.size==3 && dismissals.last().value==145.0) { "Restored draft lost its changed state" }
    field.setText("1"); field.setText("11"); field.setText("110")
    check(saved.isEmpty()) { "Typing partial digits saved a reading" }
    field.onEditorAction(android.view.inputmethod.EditorInfo.IME_ACTION_DONE)
    check(saved.size==1 && saved.single().value==110.0) { "Done did not submit exactly one complete reading" }
    form.setSaving(true)
    field.onEditorAction(android.view.inputmethod.EditorInfo.IME_ACTION_DONE)
    check(saved.size==1) { "Repeated Done submitted during save" }
    form.setSaving(false)
    activity!!.setContentView(form); field.requestFocus(); field.setText("120")
    field.dispatchKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN,android.view.KeyEvent.KEYCODE_ENTER))
    field.dispatchKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_UP,android.view.KeyEvent.KEYCODE_ENTER))
    check(saved.size==2 && saved.last().value==120.0) { "Hardware Enter did not submit exactly once" }
    form.setSaving(false); field.setText("0")
    field.onEditorAction(android.view.inputmethod.EditorInfo.IME_ACTION_DONE)
    check(saved.size==2) { "Invalid value was submitted" }
   }
   finish(Activity.RESULT_OK,Bundle().apply { putString("result","PASS: durable save, versioned edit, stale ack, delete, stale edit rejection, native entry screen, partial typing, Done, hardware Enter, duplicate guard, invalid input, changed/unchanged/invalid keyboard dismissal, Done plus dismissal, restored draft") })
  } catch(t:Throwable) { finish(Activity.RESULT_CANCELED,Bundle().apply { putString("failure",t.stackTraceToString()) }) }
  finally { activity?.let { a -> onUi { a.finish() } }; store.close(); targetContext.deleteDatabase(name) }
 }
}
