const sdk = require("spatialos_worker_sdk");

let position = require('./generated/sandbox/Position.js').Position;
let visualise = require('./generated/sandbox/Visualise.js').Visualise;
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
let canvasScale = {
    x: 1,
    y: 1
}
let canvasOffset = {
    x: 500,
    y: 500
}

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
            position: {
                x: op.data.position.x,
                y: -op.data.position.z,
            }
          }
          window.positions.push(op)
        });
      });

      connection.attachDispatcher(dispatcher);

      setTimeout(() => {
        setScale();
        render();
      }, 5000);
    });
});

render = () => {
    renderBackground();
    renderAllEntities(window.entities);
};

renderAllEntities = (entities) => {
    for (let id in entities) {
        renderSingleEntity(entities[id])
    }
}

renderSingleEntity = (entity) => {
    ctx.strokeStyle = "#FFF";
    ctx.globalAlpha = 0.5;

    let position = {
        x: entity.position.x,
        y: entity.position.y,
    }

    console.log(position);

    ctx.beginPath();
    ctx.arc((position.x + canvasOffset.x) * canvasScale.x, (position.y + canvasOffset.y) * canvasScale.y, 3, 0, 2 * Math.PI);
    ctx.stroke();
}

initialisePage = () => {
    canvas = document.getElementById("canvas");
    ctx = canvas.getContext("2d");

    var dimension = [document.documentElement.clientWidth, document.documentElement.clientHeight];
    
    canvas.width = dimension[0];
    canvas.height = dimension[1];

    renderBackground();
    renderLoading();
};

setScale = () => {
    let min = {
        x: 99999,
        y: 99999,
    }

    let max = {
        x: -99999,
        y: -99999,
    }

    for (let id in window.entities) {
        let position = window.entities[id].position;
        min.x = (position.x < min.x) ? position.x : min.x;
        min.y = (position.y < min.y) ? position.y : min.y;
        max.x = (position.x > max.x) ? position.x : max.x;
        max.y = (position.y > max.y) ? position.y : max.y; 
    }

    const bezel = 50; //px

    canvasOffset.x = -min.x + bezel;
    canvasOffset.y = -min.y + bezel;

    canvasScale.x = (ctx.canvas.width - (bezel*2)) / (max.x - min.x);
    canvasScale.y = (ctx.canvas.height - (bezel*2)) / (max.y - min.y);

    console.log(canvasScale.x, canvasScale.y)
    console.log(canvasOffset.x, canvasOffset.y)
}

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