using Improbable.Entity.Physical;
using Improbable.Math;
using Improbable.Player.Controls;
using Improbable.Unity;
using Improbable.Unity.Input.Sources;
using Improbable.Unity.Visualizer;
using IoC;
using UnityEngine;
using Vector3 = Improbable.Math.Vector3f;


namespace Assets.Gamelogic.Visualizers
{
    public class PlayerControlsVisualizer : MonoBehaviour
    {
        public Coordinates MouseDownPos;
        [Require] protected PlayerControlsStateWriter PlayerControls;
        [Require] protected PositionWriter Position;

        [Inject]
        public IInputSource InputSource { protected get; set; }

        public void OnEnable()
        {
            transform.position = Position.Value.ToUnityVector();
            GlobalParticles.Particles.Clear();
            GlobalParticles.needsUpdating = true;
        }

        public void Update()
        {
            Camera OurCamera = GlobalCameras.StaticCameras[0];

            if (Input.GetMouseButtonDown(0))
            {
                UnityEngine.Vector3 pos = OurCamera.ScreenToWorldPoint(Input.mousePosition);
                MouseDownPos = new Coordinates(pos.x, 0, pos.z);
            }
            if (Input.GetMouseButtonUp(0))
            {
                UnityEngine.Vector3 pos = OurCamera.ScreenToWorldPoint(Input.mousePosition);
                var MouseUpPos = new Coordinates(pos.x, 0, pos.z);
                PlayerControls.Update.TriggerCreateRoute(MouseDownPos, MouseUpPos).FinishAndSend();
            }

            if (Input.GetMouseButtonDown(1))
            {
                UnityEngine.Vector3 pos = OurCamera.ScreenToWorldPoint(Input.mousePosition);
                MouseDownPos = new Coordinates(pos.x, 0, pos.z);
            }
            if (Input.GetMouseButtonUp(1))
            {
                UnityEngine.Vector3 pos = OurCamera.ScreenToWorldPoint(Input.mousePosition);
                var MouseUpPos = new Coordinates(pos.x, 0, pos.z);
                if (MouseDownPos.IsWithinDistance(MouseUpPos, 1))
                {
                    KillNetwork(pos);
                }
                else
                {
                    PlayerControls.Update.TriggerCreateFlow(MouseDownPos, MouseUpPos).FinishAndSend();
                }
            }

            if (Input.GetKeyDown(KeyCode.Space))
            {
                UnityEngine.Vector3 pos = transform.position;
                KillNetwork(pos);
            }
            if (Input.GetKeyDown(KeyCode.Alpha1))
            {
                PlayerControls.Update.TriggerBuildTheInternet(1).FinishAndSend();
            }


            Move();
        }

        private void KillNetwork(UnityEngine.Vector3 pos)
        {
            var c = new Coordinates(pos.x, 0, pos.z);
            PlayerControls.Update.TriggerKillNetwork(c).FinishAndSend();
        }

        private void Move()
        {
            var direction = new UnityEngine.Vector3(InputSource.GetAxis("Horizontal"), 0,
                InputSource.GetAxis("Vertical"));

            var speed = GlobalCameras.CurrentZoom()/50;
            transform.position += direction*speed;
            Position.Update.Value(new Coordinates(transform.position.x, transform.position.y, transform.position.z))
                .FinishAndSend();
        }
    }
}