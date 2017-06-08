var app = require('express')();
var http = require('http').Server(app);
var io = require('socket.io')(http);
var child = require('child_process');
var process = require('process');

let spatial = null;

app.get('/', function(req, res){
    res.sendFile(__dirname + '/index.html');
});

io.on('connection', function(socket){
    console.log('Client connected');

    socket.on('request launch', () => requestLaunchSpatial(socket));

    socket.on('request kill', () => requestKillSpatial(socket));
});

http.listen(3000, function(){
    console.log('listening on *:3000');
});

function requestLaunchSpatial(socket) {
    socket.emit('console dump', "Initiating spatial launch...");

    if (!spatial) {
        launchSpatial(socket);
    } else {
        socket.emit('console dump', "[ERROR] Spatial already running");
    }
}

function launchSpatial(socket) {
    spatial = child.spawn('bash', ['startspatial'], {detached: true});

    spatial.stdout.on('data', data => {
        console.log(data.toString());
        socket.emit('console dump', data.toString());
    });

    spatial.stderr.on('data', err => {
        socket.emit('console dump', err.toString());
    })

    spatial.on('close', (code, signal) => {
        socket.emit('console dump', "Spatial died/killed (rip)");
        console.log(`Spatial terminated due to receipt of signal ${signal}`);
    });
}

function requestKillSpatial(socket) {
    socket.emit('console dump', "Attempting spatial kill...");

    if (spatial) {
        killSpatial();
    } else {
        socket.emit('console dump', " [ERROR] Cannot kill spatial. Is it running?");
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