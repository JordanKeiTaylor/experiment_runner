const sdk = require("spatialos_worker_sdk");

let position = require('./generated/sandbox/Position.js').Position;

console.log("position");
console.log(position);

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

      console.log("======> registering position callback");
      dispatcher.onAddComponent(position.COMPONENT, (id, data) => {
        console.log("attempting add");
        
        window.entities[id] = {
            id,
            data,
        };
      });

      connection.attachDispatcher(dispatcher);
    });
});

document.addEventListener("DOMContentLoaded", function (event) {
  // Code which depends on the HTML DOM content.
  console.log("Hello World!");
});