var args = process.argv.slice(2);
console.log("RG",args[0])
var flags = JSON.parse(args[0]);

var fs = require('fs');




fs.readFile('default_launch.json', function(err, data) {
	var obj = JSON.parse(data)

	//var edited = {"sim_name": "sim3"} 

	for(var i = 0;i<obj.workers.length;i++){
		var worker = obj.workers[i];
		console.log(worker);
		for(var i = 0;i < worker.flags.length; i++){

			var flagObj = worker.flags[i];

			console.log(flagObj)
			if(flags[flagObj.name] !== undefined){
				console.log('CHANGED')
				flagObj.value = flags[flagObj.name];
			}
		}
	}

	fs.writeFileSync(__dirname + "/default_launch.json",JSON.stringify(obj))
});
