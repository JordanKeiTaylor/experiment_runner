var express = require("express");
var app = express();
var http = require('http').Server(app);
var io = require('socket.io')(http);
var sims = [];
var exec = require('child_process').exec;
var fs = require('fs');
var bodyParser = require("body-parser");
var flagNames = getFlagNames();
var existing_package = doesPackageExist();
var events = require('events');
var eventEmitter = new events.EventEmitter();

var num_sims;
var num_vm_folders = 0;
var sims_data;

eventEmitter.on("packageCreated", function(){
  console.log("Finished creating VM folder");
  num_vm_folders++;
  console.log("vmfolders", num_vm_folders)
  console.log("numsims",num_sims)
  if(num_vm_folders === num_sims){
    startVMs();
  }
});

app.use(bodyParser.urlencoded({
    extended: true
}));

app.use(bodyParser.json());

var x  = 1;
var y = 1;
var data;
var x2 = 100;
var y2 = 100;

app.get('/', function (req, res) {
  res.send("Head to data map to use the simulation experiment runner");
})

app.get('/datamap', function (req, res) {
  res.sendFile(__dirname + '/grapher.html');
})

app.post("/createSim",function(req,res){
  res.send("test");
  var worker_flags = req.body;
  var sims = {};
  var arr;
  var sim;
  var flagName;
  for(var flag in worker_flags){
    arr = flag.split(":")
    sim = arr[0]
    flagName = arr[1]
    if(sims[sim] === undefined){
      sims[sim] = {};
    }
    sims[sim][flagName] = worker_flags[flag]
  }

  num_sims = Object.keys(sims).length
  sims_data = sims;
  console.log(JSON.stringify(sims_data))

  console.log("Starting Virtual Machine Simulations...");
  if(existing_package){
    console.log("pPckage.box file already exists. If you made changes to your simulation by running spatial build since the last time you ran experiment runner, delete the package.box file");
    createVagrantDirectorys(sims);
  } else {
    var childProcess = createVagrantPackage();
    childProcess.stdout.pipe(process.stdout)
    childProcess.on('exit', function() {
      console.log("Done creating package; creating VMs to host simulations")
      createVagrantDirectorys(sims);
    })
  }
  
})

app.post('/senddata', function (req, res) {
  console.log("Received data")
  console.log(JSON.stringify(req.body))
  io.emit("message",JSON.stringify(req.body));
  res.send("Sent data to mapping client");
})


io.on('connection', function(socket){

  socket.on("requestFlags",function(data){
    console.log("Sending flags");
    socket.emit("sendFlags",JSON.stringify(flagNames))
  })

});



http.listen(3000, function(){
  console.log('listening on *:3000');
});

function createVagrantDirectorys(sims){
  var childProcess = exec("mkdir experiment_vms");

  childProcess.on('exit', function() {
    for(var simName in sims){
      createSingleDir(simName,sims[simName])
    }
  })
}

function createSingleDir(simName, simFlagData){
  console.log("Creating file for " + simName + " with flags " + JSON.stringify(simFlagData))
  var vagrant_file_path = __dirname + "/package.box"
  var sim_path = __dirname + "/experiment_vms/" + simName
  child = exec("mkdir " + simName,{cwd: __dirname + "/experiment_vms"})
  var child = exec("cp " + vagrant_file_path + " " + sim_path)
  child.stdout.pipe(process.stdout)
  child.on('exit', function() {
    eventEmitter.emit("packageCreated")
  })
}


function getFlagNames()
{
  var obj = JSON.parse(fs.readFileSync(__dirname + '/default_launch.json'));
  var name;
  var names = []


  for(var i = 0; i<obj.workers.length;i++){
    var worker = obj.workers[i];
    for(var i = 0;i < worker.flags.length; i++){

      var flagObj = worker.flags[i];
      name = flagObj.name;
      names.push(name);
    }
  }

  return names;
}

function createVagrantPackage(){
  console.log("Creating new vagrant package since one doesn't already exist; this may take a while");
  var childProcess = exec("vagrant up; vagrant package; spatial build");
  return childProcess;
}


function doesPackageExist(){
  if(fs.readdirSync(__dirname).indexOf("package.box")!== -1){
    return true
  }
  return false;
}

function startVMs(){
  console.log("Folders complete: starting VMs")
  for(var sim in sims_data){
    var path_to_vm = __dirname + "/experiment_vms/" + sim
    var c_process = exec('vagrant up;vagrant ssh --command="cd Desktop; cd */; node edit_defaults.js ' + JSON.stringify(sim_data[sim]) + ' ;spatial local start"');
    console.log("Starting vm with data " +  JSON.stringify(sim_data[sim]));
    c_process.stdout.pipe(process.stdout)
  }
}

function destroyVms(){

}