var exec = require('cordova/exec');

exports.hello = function (success, error) {
    exec(success, error, 'QingdaPwithePDA', 'hello');
};