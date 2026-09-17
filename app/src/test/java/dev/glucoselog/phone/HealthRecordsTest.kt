package dev.glucoselog.phone
import androidx.health.connect.client.records.BloodGlucoseRecord
import androidx.health.connect.client.records.metadata.Metadata
import org.junit.Assert.*
import org.junit.Test
class HealthRecordsTest {
 private fun entry()=Entry("identity",110.0,GlucoseUnit.MG,1700000000,-14400,3,"local only",4,false,false)
 @Test fun preservesUnitTimestampOffsetAndRevision() {
  val record=HealthRecords.record(entry())
  assertEquals(110.0,record.level.inMilligramsPerDeciliter,0.000001)
  assertEquals(110.0/18,record.level.inMillimolesPerLiter,0.000001)
  assertEquals(1700000000L,record.time.epochSecond)
  assertEquals(-14400,record.zoneOffset!!.totalSeconds)
  assertEquals("identity",record.metadata.clientRecordId)
  assertEquals(4L,record.metadata.clientRecordVersion)
  assertEquals(Metadata.RECORDING_METHOD_MANUAL_ENTRY,record.metadata.recordingMethod)
  assertEquals(BloodGlucoseRecord.RELATION_TO_MEAL_AFTER_MEAL,record.relationToMeal)
 }
 @Test fun mmolInputIsNotConvertedTwice() {
  val record=HealthRecords.record(entry().copy(value=5.5,unit=GlucoseUnit.MMOL))
  assertEquals(99.0,record.level.inMilligramsPerDeciliter,0.000001)
 }
 @Test fun bedtimeDoesNotInventMealContext() {
  assertEquals(BloodGlucoseRecord.RELATION_TO_MEAL_UNKNOWN,HealthRecords.record(entry().copy(context=4)).relationToMeal)
 }
}
