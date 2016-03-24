package improbable.launch

import improbable.dapi.{LaunchConfig, Launcher}
import org.flagz.{ScalaFlagz, FlagInfo, FlagContainer}

class DemonstrationLauncher(launchConfig: LaunchConfig) {

  val options = Seq(
    "--engine_startup_retries=3",
    "--game_world_edge_length=8000",
    "--entity_activator=improbable.corelib.entity.CoreLibraryEntityActivator",
    "--use_spatial_build_workflow=true",
    "--resource_based_config_name=one-gsim-one-jvm",
    "--running_locally=true"
  )
  Launcher.startGame(launchConfig, options: _*)

}

object LocalFlag extends FlagContainer {

  @FlagInfo(help="Flag for running locally, or in deployment")
  val running_locally = ScalaFlagz.valueOf(false)

}

object ManualEngineSpoolUpDemonstrationLauncher extends DemonstrationLauncher(ManualEngineStartupLaunchConfig) with App

object AutoEngineSpoolUpDemonstrationLauncher extends DemonstrationLauncher(AutomaticEngineStartupLaunchConfig) with App
