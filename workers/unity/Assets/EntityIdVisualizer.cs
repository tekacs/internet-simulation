using Improbable.Internet;
using Improbable.Unity.Visualizer;
using UnityEngine;

public class EntityIdVisualizer : MonoBehaviour
{
    [Require] private NetworkBoolsReader network;


    private void OnGUIs()
    {
        Camera camera = Camera.current;

        if (network != null && camera != null)
        {
            Vector3 p = transform.position;
            Vector3 pos = camera.WorldToScreenPoint(p);
            var style = new GUIStyle();
            style.normal.textColor = Color.black;
            int height = Screen.height;
            string id = network.Id.ToString();
            GUI.Label(new Rect(pos.x, height - pos.y, 30, 30), id, style);
        }
    }
}