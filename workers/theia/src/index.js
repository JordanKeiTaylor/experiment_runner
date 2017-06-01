const sdk = require("spatialos_worker_sdk");

let position = require('./generated/sandbox/Position.js').Position;
let visualise = require('./generated/sandbox/Visualise.js').Visualise;
let connection = require('./generated/subscriber/Connection.js').Connection;
let entityacl = require('./generated/improbable/EntityAcl.js').EntityAcl;

let components = [
    position
];

let locatorParameters = new sdk.LocatorParameters();
locatorParameters.projectName = "sandbox";
locatorParameters.credentialsType = sdk.LocatorCredentialsType.LOGIN_TOKEN;
locatorParameters.loginToken = {
  token: sdk.DefaultConfiguration.LOCAL_DEVELOPMENT_LOGIN_TOKEN
};


let workerType = "theia";
const connectionParameters = new sdk.ConnectionParameters();
connectionParameters.workerType = workerType;

window.entities = {};
window.positions = [];

let canvas;
let ctx;

const locator = sdk.Locator.create(sdk.DefaultConfiguration.LOCAL_DEVELOPMENT_LOCATOR_URL, locatorParameters);
locator.getDeploymentList((err, deploymentList) => {
  locator.connect("sandbox", connectionParameters, (err, queueStatus) => {
      return true;
    },
    (err, connection) => {
      if (err) {
        console.log("Error when connecting", err);
        return;
      }
      connection.sendLogMessage(sdk.LogLevel.WARN, workerType, "Hello from JavaScript!");


      let dispatcher = sdk.Dispatcher.create();
      dispatcher.onDisconnect(op => {
        console.log("---> Disconnected", op);
      });

      dispatcher.onAddComponent(entityacl.COMPONENT, op => {});

      components.forEach((component) => {
        console.log("======> registering position callback" + component.COMPONENT.getComponentId());

        dispatcher.onAddComponent(component.COMPONENT, op => {
          window.entities[op.entityId] = {
            op,
          }
          window.positions.push(op)
        });
      });

      connection.attachDispatcher(dispatcher);

      setTimeout(() => {
        render();
      }, 5000);
    });
});

render = () => {
    renderBackground();

    ctx.strokeStyle = "#FFF";
    ctx.globalAlpha = 0.5;
    for (let id in window.entities) {
        let position = window.entities[id].op.data.position;
        ctx.beginPath();
        ctx.arc(position.x + 500, position.y + 500, 3, 0, 2 * Math.PI);
        ctx.stroke();
    }
};

initialisePage = () => {
    canvas = document.getElementById("canvas");
    ctx = canvas.getContext("2d");

    var dimension = [document.documentElement.clientWidth, document.documentElement.clientHeight];
    
    canvas.width = dimension[0];
    canvas.height = dimension[1];

    renderBackground();
    renderLoading();
};

renderBackground = () => {
    ctx.globalAlpha = 1;
    ctx.fillStyle = "#1C1F22";
    ctx.fillRect(0, 0, ctx.canvas.width, ctx.canvas.height);
}

renderLoading = () => {
    ctx.globalAlpha = 1;
    ctx.fillStyle = "#FFF";
    ctx.font = 'lighter 20px sans-serif';
    ctx.fillText("Connecting...", 50, 50);

    ctx.globalAlpha = 0.5;
    ctx.strokeStyle = "#FFF";
    ctx.strokeRect(40, 60, 160, -35);
}

document.addEventListener("DOMContentLoaded", function(event) {
    initialisePage();
});