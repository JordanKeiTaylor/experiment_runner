const sdk = require("spatialos_worker_sdk");

let position = require('./generated/sandbox/Position.js').Position;
let visualise = require('./generated/sandbox/Visualise.js').Visualise;
let entityacl = require('./generated/improbable/EntityAcl.js').EntityAcl;

let links = require('./generated/sandbox/Links.js').Links;
let firefly = require('./generated/sandbox/Firefly.js').Firefly;

// specify what you want to observe
let listenItems = [
    {
        key: 'links',
        component: links
    },
    {
        key: 'firefly',
        component: firefly
    }
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

const FIX_PERSPECTIVE = true;
let canvasScale = {
    x: 1,
    y: 1
}
let canvasOffset = {
    x: 500,
    y: 500
}

let SCALE_REFRESH = 1000;
let MAX_FPS = 10;
let renderTimeout = null;
let scaleRefreshTimeout = null;

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

      dispatcher.onAddComponent(position.COMPONENT, op => {
        window.entities[op.entityId] = window.entities[op.entityId] || { id: op.entityId };

        window.entities[op.entityId].position = {
            x: op.data.position.x,
            y: -op.data.position.z,
        }

        setScale();
      });

      listenItems.forEach((listenItem) => {
        console.log("======> registering position callback" + listenItem.component.COMPONENT.getComponentId());

        dispatcher.onAddComponent(listenItem.component.COMPONENT, addComponentState.bind(null, listenItem.key));
        dispatcher.onComponentUpdate(listenItem.component.COMPONENT, updateComponentState.bind(null, listenItem.key));
      });

      connection.attachDispatcher(dispatcher);
    });
});

addComponentState = (componentKey, op) => {
    window.entities[op.entityId] = window.entities[op.entityId] || { id: op.entityId };
    window.entities[op.entityId][componentKey] = op.data;

    render();
}

updateComponentState = (componentKey, op) => {
    window.entities[op.entityId] = window.entities[op.entityId] || { id: op.entityId };
    
    window.entities[op.entityId][componentKey].illuminated = op.update.illuminated.value;

    render();
}

render = () => {
    // console.log("Requesting render");
    if (!renderTimeout) {
        // console.log("Setting render callback");
        renderTimeout = setTimeout(() => {
            renderTimeout = null;
            // console.log("Actually rerendering");
            renderBackground();
            renderAllEntities(window.entities);
        }, 1000/MAX_FPS)
    } else {
        // console.log("Render not needed");
    }
};

renderAllEntities = (entities) => {
    for (let id in entities) {
        if (entities[id]) {
            renderSingleEntity(entities[id])
        }
    }
}

renderSingleEntity = (entity) => {
    let position = entity.position;

    if (position) {
        // ctx.globalAlpha = 1;
        // ctx.fillStyle = "#FFF";
        // ctx.font = 'lighter 10px sans-serif';
        // ctx.fillText(entity.id, getWorldX(position.x) + 5, getWorldY(position.y));

        ctx.globalAlpha = 0.5;

        ctx.beginPath();
        ctx.arc(getWorldX(position.x), getWorldY(position.y), 8, 0, 2 * Math.PI);
        ctx.stroke();

        if (entity.firefly && entity.firefly.illuminated) {
            ctx.fillStyle = "#EDD300";
            ctx.fill();
        }

        entity.links && entity.links.ids.filter(id => id).forEach(targetId => 
            drawEntityReference(entity, entities[targetId])
        );
    }
}

getWorldX = x => (x * canvasScale.x) + (canvasOffset.x * canvasScale.x);

getWorldY = y => (y * canvasScale.y) + (canvasOffset.y * canvasScale.y);

drawEntityReference = (entity1, entity2) => {
    if (entity1 && entity1.position && entity2 && entity2.position) {
        ctx.strokeStyle = "#FFF";
        ctx.lineWidth = 3;
        ctx.beginPath();
        ctx.moveTo(getWorldX(entity1.position.x), getWorldY(entity1.position.y));
        ctx.lineTo(getWorldX(entity2.position.x), getWorldY(entity2.position.y));
        ctx.stroke();
    }
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
    if (!scaleRefreshTimeout) {
        scaleRefreshTimeout = null;
        console.log("Resetting scale");
        scaleRefreshTimeout = setTimeout(() => {
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

                if (position) {
                    min.x = (position.x < min.x) ? position.x : min.x;
                    min.y = (position.y < min.y) ? position.y : min.y;
                    max.x = (position.x > max.x) ? position.x : max.x;
                    max.y = (position.y > max.y) ? position.y : max.y;    
                }
            }

            const bezel = 0; //px

            canvasOffset.x = -min.x + bezel;
            canvasOffset.y = -min.y + bezel;

            canvasScale.x = (ctx.canvas.width - (bezel*2)) / (max.x - min.x);
            canvasScale.y = (ctx.canvas.height - (bezel*2)) / (max.y - min.y);

            if (FIX_PERSPECTIVE) {
                let minScaleFactor = Math.min(canvasScale.x, canvasScale.y);    
                canvasScale.x = minScaleFactor;
                canvasScale.y = minScaleFactor;
            }
        }, SCALE_REFRESH);
    }
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