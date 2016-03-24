using Assets.Gamelogic.Visualizers;
using Improbable;
using Improbable.Entity.Physical;
using Improbable.Internet;
using Improbable.Math;
using Improbable.Player.Controls;
using Improbable.Unity;
using Improbable.Unity.Visualizer;
using UnityEngine;

public class NetworkVisualizer : MonoBehaviour
{
    [Require] public NetworkBoolsReader Bools;
    public ParticleSystem.Particle Particle = new ParticleSystem.Particle();

    [Require] public PositionReader Position;

    public Renderer[] Renderers;
    [Require] public NetworkScaleReader Scale;

    private EntityId id;

    public void OnEnable()
    {
//        Scale.ScaleUpdated += SetSize;
        Bools.PropertyUpdated += UpdateColor;
        transform.localEulerAngles = new Vector3(90f, 0, 0);
        Position.ValueUpdated += SetParticlePosition;
        Particle.lifetime = 10000;
        Particle.position = Position.Value.ToUnityVector();
        id = gameObject.EntityId();
        GlobalParticles.Add(id, Particle);
    }


    private void SetParticlePosition(Coordinates Coordinates)
    {
        Particle.position = Coordinates.ToUnityVector();
        Particle.size = 3;
        Particle.lifetime = 10000;
    }

    public void OnDisable()
    {
        Particle.lifetime = -1;
        GlobalParticles.Remove(id);
    }

    public void SetSize(float size)
    {
        transform.localScale = new Vector3(size, size, size);
    }

    public void UpdateColor()
    {
        var unityColor = new Color(0.8f, 0.8f, 0.8f);
        if (!Bools.Sinking)
        {
            float red = Bools.Broken ? 0.9f : 0.5f;
            float blue = Bools.Routing ? 0.9f : Bools.Broken ? 0.0f : 0.5f;
            float green = Bools.Generating ? 0.9f : Bools.Broken ? 0.0f : 0.5f;

            Particle.color = new Color(red, green, blue);
        }
        else
        {
            Particle.color = unityColor;
        }
        GlobalParticles.Remove(id);
        GlobalParticles.Add(id, Particle);
    }
}