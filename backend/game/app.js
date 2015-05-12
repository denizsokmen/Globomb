/**
 * Created by tdgunes on 12/05/15.
 */

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
};

var Game = function (io) {
    this.io = io;
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
        });

        socket.on("acknowledge", function(message){
            self.addPlayer(socket.id, message["name"]);
        });

    });

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