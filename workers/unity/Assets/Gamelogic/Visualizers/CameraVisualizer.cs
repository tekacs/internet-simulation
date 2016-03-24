using System;
using Improbable.Player;
using Improbable.Unity.Input.Sources;
using Improbable.Unity.Visualizer;
using IoC;
using Newtonsoft.Json.Schema;
using UnityEngine;

namespace Assets.Gamelogic.Visualizers
{
    public class CameraVisualizer : MonoBehaviour
    {
        [Inject] public IInputSource InputSource { protected get; set; }
        
        [Require] protected LocalPlayerCheckStateWriter LocalPlayerCheck;

        public float RotationSpeed;
        public float zoomSpeed = 0.3f;
        public Vector3 CameraOffset;


        public void Update()
        {
 
            GlobalCameras.SetPosition(transform.position + CameraOffset);

            var scrollWheel = Input.mouseScrollDelta.y/10;
            if (scrollWheel != 0)
            {
                GlobalCameras.AdjustZoom(scrollWheel*zoomSpeed);
            }

            if (Input.GetKey(KeyCode.LeftControl)|| Input.GetKey(KeyCode.Q))
            {
                GlobalCameras.AdjustZoom(zoomSpeed*Time.deltaTime);
            }

            if (Input.GetKey(KeyCode.LeftShift) || Input.GetKey(KeyCode.E))
            {
                GlobalCameras.AdjustZoom(zoomSpeed * Time.deltaTime*-1);
            }
        }
    }
}
