using System;
using System.Collections.Generic;
using Improbable.Core;
using Improbable.Core.Network;
using Improbable.Fapi.Receptionist;
using Improbable.Unity;
using Improbable.Unity.Core;
using UnityEngine;

namespace Improbable
{
    public class Bootstrap : MonoBehaviour, IBootstrapHandler
    {
        public string ReceptionistIp = "localhost";
        public int ReceptionistPort = 7777;
        public EnginePlatform EngineType = EnginePlatform.Client;
        public int FixedUpdateRate = 20;
        public int TargetFps = 120;
        public bool UsePrefabPooling = true;
        public LinkProtocol LinkProtocol = LinkProtocol.Tcp;

        public void Start()
        {
            var engineConfiguration = EngineConfiguration.Instance;
            {
                engineConfiguration.Ip = ReceptionistIp;
                engineConfiguration.Port = ReceptionistPort;
                engineConfiguration.TargetFps = TargetFps;
                engineConfiguration.FixedUpdateRate = FixedUpdateRate;
                engineConfiguration.UsePrefabPooling = UsePrefabPooling;
                engineConfiguration.PrefabToPool = Prepool();
                engineConfiguration.EngineType = EngineTypeUtils.ToEngineName(EngineType);
                engineConfiguration.UseInstrumentation = true;
                engineConfiguration.IsDebugMode = true;
                engineConfiguration.LinkProtocol = LinkProtocol;
                engineConfiguration.AppName = "demo";
                engineConfiguration.MsgProcessLimitPerFrame = 0;
                engineConfiguration.Log4netConfigXml = "log4net-local.xml";
                engineConfiguration.ShouldReconnect = true;
                engineConfiguration.AssemblyName = "";
            };
            EngineLifecycleManager.StartGame(this, gameObject, engineConfiguration);
        }

        private static Dictionary<string, int> Prepool()
        {
            return new Dictionary<string, int>();
        }
        
        public void OnDeploymentListRetrieved(IList<IDeployment> deployments, Action<IDeployment> handleChosenDeployment)
        {
            handleChosenDeployment(deployments[0]);
        }

        public void OnQueuingStarted()
        {
            Debug.Log("Queueing started");
        }

        public void OnQueuingUpdate(IQueueStatus status)
        {
            Debug.Log(status);
        }

        public void OnQueuingCompleted(IQueueStatus status)
        {
            Debug.Log("Queueing complete");
        }

        public void OnBootstrapError(Exception exception)
        {
            Debug.LogError("Exception: " + exception.Message);
        }

        public void BeginPreconnectionTasks(IDeployment deployment, IContainer container, Action onCompletedPreconnectionTasks)
        {
            onCompletedPreconnectionTasks();
        }
    }
}
