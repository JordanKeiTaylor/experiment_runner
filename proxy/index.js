var app = require('express')();
var http = require('http').Server(app);
var io = require('socket.io')(http);
var child = require('child_process');
var process = require('process');
const uuidV4 = require('uuid/v4');

let spatial = null;
const uuid = uuidV4();

console.log(`Starting Hermes instance: ${uuid}`);

app.get('/start', function(req, res, next) {
    res.send();
    console.log("Starting simulation...");
    console.log(req.query);
    requestLaunchSpatial();
});

app.get('/*', function(req, res){
    res.sendFile(__dirname + '/index.html');
});

io.on('connection', function(socket){
    console.log('Client connected');

    socket.emit('console dump', `Hermes instance: ${uuid}`);

    socket.on('request launch', requestLaunchSpatial);

    socket.on('request kill', requestKillSpatial);
});

http.listen(3000, function(){
    console.log('listening on *:3000');
});


function requestLaunchSpatial() {
    io.sockets.emit('console dump', "Initiating spatial launch...");

    if (!spatial) {
        if (launchSpatial()) {
            spatial.stdout.on('data', data => {
                console.log(data.toString());
                io.sockets.emit('console dump', data.toString());
            });

            spatial.stderr.on('data', err => {
                io.sockets.emit('console dump', err.toString());
            })

            spatial.on('close', (code, signal) => {
                io.sockets.emit('console dump', "Spatial died/killed (rip)");
                console.log(`Spatial terminated due to receipt of signal ${signal}`);
            });
        }
    } else {
        io.sockets.emit('console dump', "[ERROR] Spatial already running");
    }
}

function launchSpatial() {
    if (!spatial) {
        spatial = child.spawn('bash', ['startspatial'], {detached: true});
    }

    return spatial;
}

function requestKillSpatial() {
    io.sockets.emit('console dump', "Attempting spatial kill...");

    if (spatial) {
        killSpatial();
    } else {
        io.sockets.emit('console dump', " [ERROR] Cannot kill spatial. Is it running?");
    }
}

function killSpatial() {
    if (spatial) {
        process.kill(-spatial.pid, 'SIGINT');
        spatial = null;
    }
}

function exit() {
    killSpatial();
    process.exit();
}

process.on('exit', exit);
process.on('SIGINT',exit);
process.on('uncaughtException', exit);