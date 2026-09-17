package dev.glucoselog.phone
import org.junit.Assert.*
import org.junit.Test
class KeyboardCloseTest {
 @Test fun savesOnlyAfterVisibleKeyboardClosesOnChangedForm() {
  val state=KeyboardClose()
  assertFalse(state.update(false,true,true,false))
  assertFalse(state.update(true,true,true,false))
  assertTrue(state.update(false,true,true,false))
  assertFalse(state.update(false,true,true,false))
 }
 @Test fun unchangedOrSubmittingFormsDoNotSave() {
  val state=KeyboardClose()
  state.update(true,true,false,false)
  assertFalse(state.update(false,true,false,false))
  state.update(true,true,true,true)
  assertFalse(state.update(false,true,true,true))
 }
 @Test fun backgroundingOrOpeningDialogDoesNotCommit() {
  val state=KeyboardClose(); state.update(true,true,true,false)
  assertFalse(state.update(false,false,true,false))
  assertFalse(state.update(false,true,true,false))
 }
}
