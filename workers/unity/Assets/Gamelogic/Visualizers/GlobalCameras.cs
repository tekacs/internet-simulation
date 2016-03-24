using System;
using UnityEngine;

namespace Assets.Gamelogic.Visualizers
{
    class GlobalCameras: MonoBehaviour
    {

        public static Camera[] StaticCameras;

        public Camera[] Cameras;
        public static Transform Transform;

        public void Start()
        {
            StaticCameras = Cameras;
            Transform = transform;
        }

        public static void SetPosition(Vector3 pos)
        {
           Transform.position = pos;
        }

        public static float CurrentZoom()
        {
            return StaticCameras[0].orthographicSize;
        }

        public static void AdjustZoom(float zoomAdjust)
        {
            var zoomDelta = (float)Math.Exp(-zoomAdjust);
            for (int i = 0; i < StaticCameras.Length; i++)
            {
                StaticCameras[i].orthographicSize *= zoomDelta;
            }

        }
    }
}
