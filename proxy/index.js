var app = require('express')();
var http = require('http').Server(app);
var io = require('socket.io')(http);
var child = require('child_process');

let spatialProcess = null;

app.get('/', function(req, res){
    res.sendFile(__dirname + '/index.html');
});

io.on('connection', function(socket){
    console.log('a user connected');

    socket.on('request launch', () => {
        if (startSpatial(socket)) {
            socket.emit('console dump', "Initiating spatial launch...");
        } else {
            socket.emit('console dump', "[ERROR] Spatial already running!");
        }
    })


});

function startSpatial(socket) {
    if (!spatialProcess) {
        spatial = child.spawn('bash', ['startspatial'], []);

        spatial.stdout.on('data', data => {
            console.log(data.toString());
            socket.emit('console dump', data.toString());
        });

        spatial.stderr.on('data', err => {
            socket.emit('console dump', err.toString());
        })

        return true;
    }

    return false;
}

http.listen(3000, function(){
    console.log('listening on *:3000');
});