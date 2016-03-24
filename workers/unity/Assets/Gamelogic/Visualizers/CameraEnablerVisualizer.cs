using Improbable.Player;
using Improbable.Unity.Visualizer;
using UnityEngine;

namespace Assets.Gamelogic.Visualizers
{
    public class CameraEnablerVisualizer : MonoBehaviour
    {
        [Require] protected LocalPlayerCheckStateWriter LocalPlayerCheck;

        public Camera OurCamera;

        public void OnEnable()
        {
            OurCamera.enabled = true;
        }

        public void OnDisable()
        {
            OurCamera.enabled = false;
        }
    }
}