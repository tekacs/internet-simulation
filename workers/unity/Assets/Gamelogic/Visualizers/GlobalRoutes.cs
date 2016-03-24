using System.Collections.Generic;
using System.Linq;
using Improbable;
using Improbable.Internet;
using Improbable.Player.Controls;
using UnityEngine;

namespace Assets.Gamelogic.Visualizers
{
    internal class GlobalRoutes : MonoBehaviour
    {
        private static readonly Dictionary<EntityId, RouteReader> Routes = new Dictionary<EntityId, RouteReader>(5000);
        private static RouteReader[] RouteArray;
        private static bool NeedsUpdating = true;

        public static void Add(EntityId Id, RouteReader Route)
        {
            Routes.Add(Id, Route);
            NeedsUpdating = true;
        }

        public static void Remove(EntityId id)
        {
            Routes.Remove(id);
            NeedsUpdating = true;
        }

        public Material active;

        private Color LIGHT_GREY = new Color(0.8f, 0.8f, 0.8f, 0.002f);

        public void OnRenderObject()
        {
            if (NeedsUpdating)
            {
                NeedsUpdating = false;
                RouteArray = Routes.Values.ToArray();
            }

            // Apply the line material
            active.SetPass(0);
            GL.PushMatrix();

            if ((Camera.current.cullingMask & (1 << LayerMask.NameToLayer("Route"))) != 0)
            {
                // Draw lines
                GL.Begin(GL.LINES);

                DrawInactive();
                GL.End();
            }

            GL.Begin(GL.LINES);
            DrawActive();
            GL.End();

            GL.PopMatrix();
        }

        private void DrawActive()
        {
            for (int i = 0; i < RouteArray.Length; i ++)
            {
                var Route = RouteArray[i];

                var utilisation = Route.UtilisationDown + Route.UtilisationUp;
                var color = LIGHT_GREY;

                if (utilisation > 0)
                {
                    var fractionUsed = utilisation/(1*Route.Capacity);
                    if (fractionUsed < 1.0)
                    {
                        color.g = 1 - fractionUsed;
                        color.b = fractionUsed;
                        color.r = 0;
                        color.a = (fractionUsed*0.3f + 0.4f)/4;
                    }
                    else
                    {
                        color.g = 0;
                        color.b = 2 - fractionUsed;
                        color.r = fractionUsed - 1;
                        color.a = (fractionUsed*0.3f + 0.4f)/4;
                    }
                    GL.Color(color);

                    // One vertex at transform position
                    GL.Vertex3((float) Route.SrcPosition.X, (float) Route.SrcPosition.Y, (float) Route.SrcPosition.Z);
                    // Another vertex at edge of circle
                    GL.Vertex3((float) Route.DestPosition.X, (float) Route.DestPosition.Y, (float) Route.DestPosition.Z);
                }
            }
        }


        private void DrawInactive()
        {
            var color = LIGHT_GREY;
            color.r = 0.95f;
            color.g = 0.95f;
            color.b = 1f;
            color.a = 0.03f;
            GL.Color(color);

            for (int i = 0; i < RouteArray.Length; i += 1)
            {
                var Route = RouteArray[i];
                var utilisation = Route.UtilisationDown + Route.UtilisationUp;

                if (utilisation <= 0)
                {
                    // One vertex at transform position
                    GL.Vertex3((float) Route.SrcPosition.X, (float) Route.SrcPosition.Y, (float) Route.SrcPosition.Z);
                    // Another vertex at edge of circle
                    GL.Vertex3((float) Route.DestPosition.X, (float) Route.DestPosition.Y,
                        (float) Route.DestPosition.Z);
                }
            }
        }
    }
}