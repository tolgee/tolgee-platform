package io.tolgee.formats.unity

/**
 * Values extracted from real Unity output of com.unity.localization 1.5.x; they must match the
 * MonoScript GUIDs that ship with that package version or Unity will not bind the imported assets.
 */
object UnityFormatConstants {
  const val STRING_TABLE_SCRIPT_GUID = "9058120eef828264daf3083b0010d769"
  const val SHARED_TABLE_DATA_SCRIPT_GUID = "9b94f33f036cd2c4391bef2b2c4400c4"
  const val STRING_TABLE_COLLECTION_SCRIPT_GUID = "2a5edd11fb04c1d4c8e64f5deea05c14"

  const val CUSTOM_KEY_ID = "_unityKeyId"
  const val CUSTOM_SHARED_TABLE_DATA_GUID = "_unitySharedTableDataGuid"
  const val CUSTOM_IS_SMART = "_unityIsSmart"
}
