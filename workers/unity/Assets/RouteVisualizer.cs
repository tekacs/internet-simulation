using Assets.Gamelogic.Visualizers;
using Improbable.Internet;
using Improbable.Unity.Visualizer;
using UnityEngine;

public class RouteVisualizer : MonoBehaviour
{
    private readonly Color LIGHT_GREY = new Color(0.1f, 0.1f, 0.1f);
    [Require] public RouteReader Route;



    public void OnEnable()
    {
        float capacity = Route.Capacity;

        GlobalRoutes.Add(gameObject.EntityId(), Route);
    }

    public void OnDisable()
    {
        GlobalRoutes.Remove(gameObject.EntityId());
    }

}