/**
 * Created by tdgunes on 12/05/15.
 */


var Timer = require("./timer");


Object.count = function(obj) {
    var count = 0, key;
    for (key in obj) {
        if (obj.hasOwnProperty(key)) count++;
    }
    return count;
};


var Player = function (identifier, name) {
        this.name = name;
        this.identifier = identifier;
        this.longitude = 0;
        this.latitude = 0;
        this.bomb = false;
};

var Game = function (io) {
    this.io = io;
    this.roundTime = 60 * 1000; //one minute


    this.players = {};
    console.log("> Game initialized!");
    var myself = this;
    io.on('connection', function(socket){
        console.log('a user connected');
        var self = myself;
        socket.emit("message",{
            "username":"master chief",
            "message":"hello world"
            }
        );
        socket.on('disconnect', function(){
            console.log('user disconnected');

            self.deletePlayer(socket.id);

            
	    socket.broadcast.emit("kick", {"identifier": socket.id });

        });

        socket.on("acknowledge", function(message){
            self.addPlayer(socket.id, message["name"]);
            socket.emit("initialize",{"player": self.players[socket.id]});
        });

        socket.on("bomb", function(message){
            var sender = self.players[socket.id];
            var receiver = self.players[message.identifier];
            console.log(sender.name + " is sending bomb to " + receiver.name);
            sender.bomb = false;
            receiver.bomb = true;
        });

        socket.on("location", function(message){

            var player = self.players[socket.id];
            console.log(player.name + " on new connection (" + message["longitude"] + "," + message["latitude"] + ")" );
            player.longitude = message["longitude"];
            player.latitude = message["latitude"];
            self.players[socket.id] = player;
        });

    });

    function update() {
        myself.update();
    }

    function restart() {
        myself.restart();
    }

    this.timer = new Timer(this.roundTime, restart, update);
    this.timer.start();
};

//restarts the game
Game.prototype.restart = function () {
    //console.log("Restart second: " + (this.timer.elapsedTime / 1000));
    console.log("New round!");
    var myself = this;
    for (var identifier in this.players) {
        if (this.players.hasOwnProperty(identifier)) {
            this.players[identifier].bomb = false;
            this.io.to(identifier).emit("message",{
                "username":"master chief",
                "message":"new round!"
            });

        }
    }

    var allPlayers = Object.keys(this.players).map(function(key){
        return myself.players[key];
    });
    if (allPlayers.length > 0) {
        var randomPlayer = allPlayers[Math.floor(Math.random()*allPlayers.length)];

        randomPlayer.bomb = true;
        console.log("Bomb is given to "+randomPlayer.name);
    }





};

//updates game for every second
Game.prototype.update = function () {
    //console.log("Update second: " + (this.timer.elapsedTime / 1000));
    var pack = {"time": (this.timer.elapsedTime / 1000)  };
    var myself = this;
    pack["players"] = Object.keys(this.players).map(function(key){
        return myself.players[key];
    });
    for (var identifier in this.players) {
        if (this.players.hasOwnProperty(identifier)) {
            this.io.to(identifier).emit('gamestate', pack );
        }
    }
};

Game.prototype.deletePlayer = function (identifier) {
    delete this.players[identifier];
    this.logPlayerCount();
};

Game.prototype.logPlayerCount = function () {
    console.log("Total players: " + Object.count(this.players));
};

Game.prototype.addPlayer = function (identifier, playerName) {
    console.log("Player added: "+playerName);
    this.players[identifier] = new Player(identifier, playerName);
    this.logPlayerCount();
};

Game.prototype.broadcast = function (message) {
    io.emit('Message', message);
};



module.exports = Game;
