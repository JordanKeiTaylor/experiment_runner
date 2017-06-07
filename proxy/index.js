var app = require('express')();
var http = require('http').Server(app);
var io = require('socket.io')(http);
var child = require('child_process');

app.get('/', function(req, res){
    res.sendFile(__dirname + '/index.html');
});

io.on('connection', function(socket){
    console.log('a user connected');

    let spatial = child.spawn('bash', ['startspatial'], []);

    spatial.stdout.on('data', data => {
        console.log(data);
        socket.emit('console dump', data.toString());
        // res.write(data);
    });

    spatial.stderr.on('data', data => {
        // res.send("[PROXY] Something went wrong: " + data);
    })
});

http.listen(3000, function(){
    console.log('listening on *:3000');
});