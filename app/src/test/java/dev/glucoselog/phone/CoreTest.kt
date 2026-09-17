package dev.glucoselog.phone
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
class CoreTest {
 private fun entry() = Entry("stable-id",100.0,GlucoseUnit.MG,1700000000,-14400,0,"",1,false,false)
 @Test fun acceptsCommaDecimalAndCorrectConversions() {
  assertEquals(5.6,GlucoseUnit.MMOL.parse("5,6"),0.00001)
  assertEquals(100.0,GlucoseUnit.MG.toMg(100.0),0.00001)
  assertEquals(100.0,GlucoseUnit.MMOL.toMg(100.0/18),0.00001)
 }
 @Test fun rejectsInvalidValuesAndFutureTime() {
  listOf("", "NaN", "Infinity", "0", "-1", "901").forEach { raw -> assertThrows(IllegalArgumentException::class.java) { GlucoseUnit.MG.parse(raw) } }
  assertThrows(IllegalArgumentException::class.java) { GlucoseUnit.MMOL.parse("50.1") }
  assertThrows(IllegalArgumentException::class.java) { entry().copy(time=1700000100).validate(1700000000) }
 }
 @Test fun acceptsBoundariesAndBackdatedReading() {
  assertEquals(900.0,GlucoseUnit.MG.parse("900"),0.0)
  assertEquals(50.0,GlucoseUnit.MMOL.parse("50"),0.0)
  entry().validate(1700001000)
 }
 @Test fun staleOrDeletedEditIsRejected() {
  assertThrows(IllegalArgumentException::class.java) { entry().requireRevision(0) }
  assertThrows(IllegalArgumentException::class.java) { entry().requireRevision(2) }
  entry().requireRevision(1)
  entry().copy(revision=0).requireRevision(0)
 }
 private class Memory(var row:Entry):SyncStore {
  override fun pending()=if(row.synced) emptyList() else listOf(row)
  override fun acknowledge(id:String,revision:Long) { if(row.id==id && row.revision==revision) row=row.copy(synced=true) }
 }
 @Test fun failedWriteStaysPendingAndRetryKeepsIdentity() = runBlocking {
  val store=Memory(entry()); val sent=mutableListOf<Entry>(); var fail=true
  val sink=object:HealthSink { override suspend fun write(e:Entry) { sent.add(e); if(fail) error("unavailable") }; override suspend fun delete(id:String) { error("not a deletion") } }
  assertEquals(1,SyncEngine(store,sink).run().remaining)
  assertFalse(store.row.synced); fail=false
  assertEquals(0,SyncEngine(store,sink).run().remaining)
  assertEquals(sent[0],sent[1]); assertTrue(store.row.synced)
 }
 @Test fun acknowledgmentCannotLoseConcurrentEdit() = runBlocking {
  val store=Memory(entry())
  val sink=object:HealthSink { override suspend fun write(e:Entry) { store.row=e.copy(value=110.0,revision=2) }; override suspend fun delete(id:String) {} }
  assertEquals(1,SyncEngine(store,sink).run().remaining)
  assertFalse(store.row.synced); assertEquals(110.0,store.row.value,0.0)
 }
 @Test fun deletionRetriesWithoutWritingReading() = runBlocking {
  val store=Memory(entry().copy(deleted=true,revision=2)); val ids=mutableListOf<String>()
  val sink=object:HealthSink { override suspend fun write(e:Entry) { error("must delete") }; override suspend fun delete(id:String) { ids.add(id) } }
  assertEquals(0,SyncEngine(store,sink).run().remaining)
  assertEquals(listOf("stable-id"),ids)
 }
}
