/**
 * Created by tdgunes on 12/05/15.
 */


var Player = function (name) {
      this.name = name;
};

var Game = function (io) {
    this.io = io;
    this.players = [];
    console.log("> Game initialized!");

    io.on('connection', function(socket){
        console.log('a user connected');

        socket.on('disconnect', function(){
            console.log('user disconnected');
        });

        socket.on("acknowledge", function(message){
            console.log("message: "+ message);
        });

    });

};

Game.prototype.addPlayer = function (playerName) {
    this.players.push(new Player(playerName));
};

Game.prototype.broadcast = function (message) {
    io.emit('Message', message);
};



module.exports = Game;