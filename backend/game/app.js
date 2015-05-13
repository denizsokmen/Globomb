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

            for (var identifier in this.players) {
                if (this.players.hasOwnProperty(identifier)) {
                    this.io.to(identifier).emit('kick', {"identifier": socket.id } );
                }
            }

        });

        socket.on("acknowledge", function(message){
            self.addPlayer(socket.id, message["name"]);
            socket.emit("initialize",{"player": self.players[socket.id]});
        });

        socket.on("bomb", function(message){
            var player = self.players[socket.id];

        });

        socket.on("location", function(message){
            var player = self.players[socket.id];
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
    console.log("Restart second: " + (this.timer.elapsedTime / 1000));
    for (var identifier in this.players) {
        if (this.players.hasOwnProperty(identifier)) {
            this.io.to(identifier).emit("message",{
                "username":"master chief",
                "message":"New game is started!"
            });
        }
    }
};

//updates game for every second
Game.prototype.update = function () {
    console.log("Update second: " + (this.timer.elapsedTime / 1000));
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