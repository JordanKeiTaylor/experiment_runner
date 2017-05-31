const sdk = require("spatialos_worker_sdk");

let position = require('./generated/sandbox/Position.js').Position;
let visualise = require('./generated/sandbox/Visualise.js').Visualise;
let connection = require('./generated/subscriber/Connection.js').Connection;
let entityacl = require('./generated/improbable/EntityAcl.js').EntityAcl;

let components = [
    visualise,
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
        });
      });

      connection.attachDispatcher(dispatcher);
    });
});

document.addEventListener("DOMContentLoaded", function (event) {
    setTimeout(() => {
        var canvas = document.getElementById("canvas");
        var ctx = canvas.getContext("2d");
        ctx.beginPath();
        ctx.arc(95,50,40,0,2*Math.PI);
        ctx.stroke();
    }, 5000);
});