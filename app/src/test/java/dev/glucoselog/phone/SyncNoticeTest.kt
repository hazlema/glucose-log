package dev.glucoselog.phone
import org.junit.Assert.*
import org.junit.Test
class SyncNoticeTest {
 private fun row()=Entry("one",110.0,GlucoseUnit.MG,1700000000,0,0,"",1,false,false)
 @Test fun doesNotClaimSentUntilExactSavedRevisionIsAcknowledged() {
  val notices=SyncNotices(); notices.add(row())
  assertTrue(notices.confirmed(listOf(row())).isEmpty())
  assertTrue(notices.confirmed(listOf(row().copy(synced=true,revision=0))).isEmpty())
  assertEquals(listOf("110 mg/dL sent to Health Connect"),notices.confirmed(listOf(row().copy(synced=true))))
  assertTrue(notices.confirmed(listOf(row().copy(synced=true))).isEmpty())
 }
 @Test fun pendingConfirmationSurvivesRecreation() {
  val first=SyncNotices(); first.add(row())
  val restored=SyncNotices(); first.pending().forEach { restored.add(it) }
  assertEquals(listOf("110 mg/dL sent to Health Connect"),restored.confirmed(listOf(row().copy(synced=true))))
 }
 @Test fun newEditReplacesOlderConfirmationAndDeletedRowsCannotConfirm() {
  val notices=SyncNotices(); notices.add(row()); notices.add(row().copy(value=120.0,revision=2))
  assertTrue(notices.confirmed(listOf(row().copy(synced=true))).isEmpty())
  assertTrue(notices.confirmed(listOf(row().copy(value=120.0,revision=2,synced=true,deleted=true))).isEmpty())
  assertEquals(listOf("120 mg/dL sent to Health Connect"),notices.confirmed(listOf(row().copy(value=120.0,revision=2,synced=true))))
 }
}
