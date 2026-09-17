package dev.glucoselog.phone

/** Confirm only the exact value/revision the user just saved, never a transport attempt. */
class SyncNotices {
 private val awaiting=linkedMapOf<String,Entry>()
 fun pending():List<Entry> = awaiting.values.toList()
 fun add(entry:Entry) { awaiting[entry.id]=entry }
 fun confirmed(rows:List<Entry>):List<String> {
  val messages=mutableListOf<String>()
  for(row in rows) {
   val expected=awaiting[row.id] ?: continue
   if(!row.deleted && row.synced && row.revision==expected.revision && row.value==expected.value && row.unit==expected.unit) {
    messages.add("${row.unit.format(row.value)} ${row.unit.label} sent to Health Connect")
    awaiting.remove(row.id)
   }
  }
  return messages
 }
}
