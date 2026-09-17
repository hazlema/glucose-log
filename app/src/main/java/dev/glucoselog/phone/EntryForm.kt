package dev.glucoselog.phone
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.os.Bundle
import android.text.InputFilter
import android.text.InputType
import android.view.Gravity
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.widget.*
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.UUID

class EntryForm(c:Context,private val original:Entry?,saved:Bundle?,initialUnit:GlucoseUnit,private val onSave:(Entry)->Unit):LinearLayout(c) {
 private var unit=GlucoseUnit.valueOf(saved?.getString("unit") ?: original?.unit?.name ?: initialUnit.name)
 private val id=saved?.getString("id") ?: original?.id ?: UUID.randomUUID().toString()
 private val expectedRevision=saved?.getLong("revision") ?: original?.revision ?: 0L
 private var customTime=saved?.getBoolean("customTime") ?: (original!=null)
 private var instant=saved?.getLong("time") ?: original?.time ?: Instant.now().epochSecond
 private var offset=saved?.getInt("offset") ?: original?.offset ?: ZoneId.systemDefault().rules.getOffset(Instant.ofEpochSecond(instant)).totalSeconds
 private val number=EditText(c)
 private val notes=EditText(c)
 private val meal=Spinner(c)
 private val timeButton:Button
 private val unitButton:Button
 private val keyboardClose=KeyboardClose()
 private val baselineMg=saved?.getDouble("baselineMg") ?: original?.let { it.unit.toMg(it.value) } ?: initialUnit.toMg(initialUnit.initial.toDouble())
 private fun changed():Boolean {
  val changedValue=try { kotlin.math.abs(unit.toMg(value())-baselineMg)>0.000001 } catch(_:IllegalArgumentException) { true }
  return changedValue || notes.text.toString().trim()!=(original?.note ?: "") || meal.selectedItemPosition!=(original?.context ?: 0) || (customTime && instant!=original?.time)
 }
 fun keyboardVisibilityChanged(visible:Boolean,eligible:Boolean) {
  if(keyboardClose.update(visible,eligible,changed(),submitting)) submit()
 }
 private var editSaveButton:Button?=null
 private var submitting=false
 fun setSaving(value:Boolean) { submitting=value; editSaveButton?.isEnabled=!value; number.isEnabled=!value; notes.isEnabled=!value; meal.isEnabled=!value; unitButton.isEnabled=!value; timeButton.isEnabled=!value }
 private var exactValue=saved?.getDouble("exact") ?: original?.value ?: unit.initial.toDouble()
 private var rendered=saved?.getString("rendered") ?: unit.format(exactValue)
 init {
  orientation=VERTICAL
  setPadding(0,Ui.dp(c,20),0,Ui.dp(c,24))
  addView(Ui.text(c,if(original==null) "New reading" else "Edit reading",28f,true))
  addView(Ui.text(c,"Enter your blood glucose",16f).apply { setTextColor(Ui.muted) })
  val numberRow=LinearLayout(c).apply { gravity=Gravity.CENTER_VERTICAL }
  number.apply { setText(saved?.getString("number") ?: rendered); textSize=64f; setTextColor(Ui.ink); inputType=InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL; setSingleLine(); setSelectAllOnFocus(true); contentDescription="Blood glucose value"; filters=arrayOf(InputFilter.LengthFilter(12)); setPadding(0,Ui.dp(c,8),Ui.dp(c,8),Ui.dp(c,8)) }
  number.imeOptions=EditorInfo.IME_ACTION_DONE
  number.setOnEditorActionListener { _,action,event ->
   if(action==EditorInfo.IME_ACTION_DONE || (event?.keyCode==KeyEvent.KEYCODE_ENTER && event.action==KeyEvent.ACTION_DOWN)) { submit(); true } else false
  }
  number.setOnKeyListener { _,key,event ->
   if(key==KeyEvent.KEYCODE_ENTER || key==KeyEvent.KEYCODE_NUMPAD_ENTER) {
    if(event.action==KeyEvent.ACTION_DOWN && event.repeatCount==0) submit()
    true
   } else false
  }
  numberRow.setPadding(Ui.dp(c,16),Ui.dp(c,16),Ui.dp(c,16),Ui.dp(c,16))
  numberRow.background=Ui.background(Ui.pale,Ui.dp(c,20).toFloat())
  numberRow.layoutParams=LayoutParams(-1,-2).apply { topMargin=Ui.dp(c,20); bottomMargin=Ui.dp(c,8) }
  numberRow.addView(number,LinearLayout.LayoutParams(0,-2,1f))
  unitButton=Ui.button(c,unit.label) { switchUnit() }
  numberRow.addView(unitButton,LinearLayout.LayoutParams(-2,Ui.dp(c,56)))
  addView(numberRow)
  addView(Ui.text(c,"Close the keyboard to save",14f).apply { setTextColor(Ui.muted) })
  val details=Ui.column(c).apply { visibility=if(saved?.getBoolean("details")==true || original!=null) VISIBLE else GONE }
  val detailsToggle=Ui.button(c,if(details.visibility==VISIBLE) "Hide details" else "+  Time, meal & notes") {}
  detailsToggle.setOnClickListener { details.visibility=if(details.visibility==VISIBLE) GONE else VISIBLE; detailsToggle.text=if(details.visibility==VISIBLE) "Hide details" else "+  Time, meal & notes" }
  addView(detailsToggle); addView(details)
  details.tag="details"
  details.addView(Ui.text(c,"Measured at",14f,true))
  timeButton=Ui.button(c,"") { chooseTime() }; details.addView(timeButton); updateTime()
  if(original==null) details.addView(Ui.button(c,"Use current time") { customTime=false; updateTime() })
  details.addView(Ui.text(c,"Meal label (optional)",14f,true))
  meal.adapter=ArrayAdapter(c,android.R.layout.simple_spinner_dropdown_item,Entry.contexts)
  meal.setSelection(saved?.getInt("meal") ?: original?.context ?: 0); meal.minimumHeight=Ui.dp(c,52); meal.contentDescription="Meal label"; details.addView(meal)
  details.addView(Ui.text(c,"Notes (optional)",14f,true))
  notes.apply { hint="Food, exercise, or anything to remember"; textSize=16f; inputType=InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES; minLines=2; maxLines=5; filters=arrayOf(InputFilter.LengthFilter(2000)); setText(saved?.getString("note") ?: original?.note ?: ""); setTextColor(Ui.ink); setHintTextColor(Ui.muted); contentDescription="Notes" }
  details.addView(notes)
  notes.imeOptions=EditorInfo.IME_ACTION_DONE
  notes.setOnEditorActionListener { _,action,_ -> if(action==EditorInfo.IME_ACTION_DONE) { submit(); true } else false }
  details.addView(Ui.text(c,"Notes stay on your phone. Close the keyboard to save.",13f).apply { setTextColor(Ui.muted) })
  if(original!=null) {
   editSaveButton=Ui.button(c,"Save changes",true) { submit() }
   addView(editSaveButton)
  }
 }
 private fun value():Double {
  val parsed=unit.parse(number.text.toString())
  return if(number.text.toString()==rendered) exactValue else parsed
 }
 private fun switchUnit() {
  try {
   val mg=unit.toMg(value()); unit=if(unit==GlucoseUnit.MG) GlucoseUnit.MMOL else GlucoseUnit.MG
   exactValue=unit.fromMg(mg); rendered=unit.format(exactValue); number.setText(rendered); unitButton.text=unit.label
  } catch(ex:IllegalArgumentException) { number.error=ex.message }
 }
 private fun updateTime() { timeButton.text=if(!customTime) "Now · time recorded when saved" else Instant.ofEpochSecond(instant).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("EEE, MMM d, yyyy  h:mm a")) }
 private fun chooseTime() {
  val z=Instant.ofEpochSecond(if(customTime) instant else Instant.now().epochSecond).atZone(ZoneId.systemDefault())
  DatePickerDialog(context,{_,year,month,day ->
   TimePickerDialog(context,{_,hour,minute ->
    val chosen=LocalDateTime.of(year,month+1,day,hour,minute).atZone(ZoneId.systemDefault())
    instant=chosen.toEpochSecond(); offset=chosen.offset.totalSeconds; customTime=true; updateTime()
   },z.hour,z.minute,android.text.format.DateFormat.is24HourFormat(context)).show()
  },z.year,z.monthValue-1,z.dayOfMonth).apply { datePicker.maxDate=System.currentTimeMillis() }.show()
 }
 private fun submit() {
  if(submitting) return
  try {
   val time=if(customTime) instant else Instant.now().epochSecond
   val zone=if(customTime) offset else ZoneId.systemDefault().rules.getOffset(Instant.ofEpochSecond(time)).totalSeconds
   val e=Entry(id,value(),unit,time,zone,meal.selectedItemPosition,notes.text.toString().trim(),expectedRevision,false,false)
   e.validate(Instant.now().epochSecond); setSaving(true); onSave(e)
  } catch(ex:IllegalArgumentException) { setSaving(false); number.error=ex.message; Toast.makeText(context,ex.message,Toast.LENGTH_LONG).show() }
 }
 fun snapshot()=Bundle().apply {
  putDouble("baselineMg",baselineMg); putBoolean("details",findViewWithTag<android.view.View>("details")?.visibility==VISIBLE); putString("id",id); putLong("revision",expectedRevision); putString("unit",unit.name); putString("number",number.text.toString()); putString("note",notes.text.toString()); putInt("meal",meal.selectedItemPosition); putBoolean("customTime",customTime); putLong("time",instant); putInt("offset",offset); putDouble("exact",exactValue); putString("rendered",rendered)
 }
}
