/**
 * Created by tdgunes on 13/05/15.
 */


var Timer = function (interval, operation, tickOperation) {
    this.interval = interval;
    this.operation = operation;
    this.tickOperation = tickOperation;
    this.ticker = null;
    this.elapsedTime = 0;
    console.log("Timer initialized");

};


Timer.prototype.tick = function () {
    // tick operation is called on every tick and uses timeLeft
    this.tickOperation(this.interval-this.elapsedTime);

    // does to operation if elapsedTime is greater or equal to interval
    if (this.elapsedTime >= this.interval) {
        this.operation();
        this.elapsedTime = 0;
    }
    else {
        this.elapsedTime = this.elapsedTime + 1000;
    }


};


Timer.prototype.start = function () {

    //Javascript tricks :-(
    //For details about this: http://bytes.com/topic/javascript/answers/88860-using-setinterval-inside-object
    if (this.ticker === null) {
        var myself = this;
        function callMethod() {
            myself.tick();
        }
        console.log("Timer started!");
        this.ticker = setInterval(callMethod, 1000); //ticks for every one second
    }
};

Timer.prototype.stop = function () {
    if (this.ticker !== null){
        clearInterval(this.ticker);
        this.ticker = null;
    }
    this.elapsedTime = 0;
};


module.exports = Timer;