using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using Improbable;
using UnityEngine;

namespace Assets.Gamelogic.Visualizers
{
    class GlobalParticles: MonoBehaviour
    {

        public ParticleSystem ParticleSystem;

        public static Dictionary<EntityId, UnityEngine.ParticleSystem.Particle> Particles = new Dictionary<EntityId, ParticleSystem.Particle>(2000);

        public ParticleSystem.Particle[] parts = Particles.Values.ToArray();
        public static bool needsUpdating = true;

        public static void Add(EntityId id, UnityEngine.ParticleSystem.Particle particle)
        {
            if (Particles.ContainsKey(id))
            {
                Particles.Remove(id);
            }
            Particles.Add(id, particle);
            needsUpdating = true;
        }

        public static void Remove(EntityId id)
        {
            needsUpdating = true;
            Particles.Remove(id);
        }

        public void OnEnable()
        {
//            GetComponent<Renderer>().bounds = new Bounds(Vector3.zero, new Vector3(1000,1000,1000));
            ParticleSystem.Play();
            ParticleSystem.gravityModifier = 0;
            needsUpdating = true;

        }

        public void Update()
        {

            if (needsUpdating && Time.frameCount % 30 == 0)
            {
                parts = Particles.Values.ToArray();
                ParticleSystem.SetParticles(parts, Particles.Count);
                needsUpdating = false;
            }

            
        }

    }
}
