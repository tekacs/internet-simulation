package improbable.launch.bridgesettings

import improbable.fapi.bridge.{BridgeSettings, BridgeSettingsResolver, ConstantEngineLoadPolicy, PerEntityOrderedStateUpdateQos}
import improbable.fapi.network.TcpLinkSettings
import improbable.unity.fabric.engine.EnginePlatform
import improbable.unity.fabric.engine.EnginePlatform._
import improbable.unity.fabric.satisfiers.{AggregateSatisfiers, SatisfySingleConstraint, SatisfySpecificEngine}
import improbable.unity.fabric.{AuthoritativeEntityOnly, VisualEngineConstraint}

object DemonstrationUnityClientBridgeSettings extends BridgeSettingsResolver {

  private val CLIENT_ENGINE_BRIDGE_SETTINGS = BridgeSettings(
    DemonstrationClientAssetContextDiscriminator(),
    TcpLinkSettings,
    EnginePlatform.UNITY_CLIENT_ENGINE,
    AggregateSatisfiers(
      SatisfySpecificEngine,
      SatisfySingleConstraint(VisualEngineConstraint)
    ),
    AuthoritativeEntityOnly(30),
    ConstantEngineLoadPolicy(0.5),
    PerEntityOrderedStateUpdateQos
  )

  private val ANDROID_CLIENT_ENGINE_BRIDGE_SETTINGS = CLIENT_ENGINE_BRIDGE_SETTINGS.copy(enginePlatform = UNITY_ANDROID_CLIENT_ENGINE)

  private val bridgeSettings = Map[String, BridgeSettings](
    UNITY_CLIENT_ENGINE -> CLIENT_ENGINE_BRIDGE_SETTINGS,
    UNITY_ANDROID_CLIENT_ENGINE -> ANDROID_CLIENT_ENGINE_BRIDGE_SETTINGS
  )

  override def engineTypeToBridgeSettings(engineType: String, metadata: String): Option[BridgeSettings] = {
    bridgeSettings.get(engineType)
  }

}
