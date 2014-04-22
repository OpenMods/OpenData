String.prototype.startsWith = function(str) {
    return this.slice(0, str.length) == str;
};

String.prototype.endsWith = function(suffix) {
    return this.indexOf(suffix, this.length - suffix.length) !== -1;
};

Array.prototype.unique = function() {
    var a = this.concat();
    for (var i = 0; i < a.length; ++i) {
        for (var j = i + 1; j < a.length; ++j) {
            if (a[i] === a[j])
                a.splice(j--, 1);
        }
    }

    return a;
};

Array.prototype.remove = function() {
    var what, a = arguments, L = a.length, ax;
    while (L && this.length) {
        what = a[--L];
        while ((ax = this.indexOf(what)) !== -1) {
            this.splice(ax, 1);
        }
    }
    return this;
};

if (process.argv.length != 4) {
    process.exit();
}

var dbUser = process.argv[2];
var dbPass = process.argv[3];


var myNick = 'OpenEye2';
var mainChannel = '#OpenEye';
var connectionString = 'mongodb://' + dbUser + ':' + dbPass + '@openeye.openmods.info:27017/hopper';

var MongoClient = require('mongodb').MongoClient
var irc = require('irc');
var mods = require('./mods.js');
var files = require('./files.js');

var actions = {
    'mod:find': {
        func: mods.findMods,
        secure: false
    },
    'mod:get': {
        func: mods.getFields,
        secure: false
    },
    'mod:update': {
        func: mods.setField,
        secure: true
    },
    'mod:unset': {
        func: mods.unsetField,
        secure: true
    },
    'mod:admins:add': {
        func: mods.addAdmin,
        secure: true
    },
    'mod:admins:remove': {
        func: mods.removeAdmin,
        secure: true
    },
    'file:notes:add': {
        func: files.addNote,
        secure: true
    },
    'file:notes:list': {
        func: files.listNotes,
        secure: false
    },
    'file:notes:remove': {
        func: files.removeNote,
        secure: true
    },
    'commands': {
        func: function(context) {
            var keys = [];
            for (key in actions) {
                keys.push(key);
            }
            context.bot.say(context.channel, keys.join(', '));
        },
        secure: false
    }
};


MongoClient.connect(connectionString, function(err, db) {

    var bot = new irc.Client('irc.esper.net', myNick, {
        channels: [mainChannel],
        debug: true
    });


    bot.addListener('message', function(from, to, message) {

        if (message.startsWith('!')) {

            var matches = message.match(/^\!([a-z0-9\:]+)\s?(.*)?$/i);

            if (matches.length > 0) {

                var action = actions[matches[1]];

                if (action != null) {

                    var tmp = [];
                    if (matches[2] != null) {
                        tmp = matches[2].match(/('(\\'|[^'])*'|"(\\"|[^"])*"|\/(\\\/|[^\/])*\/|(\\ |[^ ])+|[\w-]+)/g);
                    }

                    if (tmp == null) {
                        tmp = [];
                    }

                    var args = [];

                    for (var i = 0; i < tmp.length; i++) {
                        if ((tmp[i].startsWith("\"") && tmp[i].endsWith("\"")) ||
                                (tmp[i].startsWith("'") && tmp[i].endsWith("'"))) {
                            args.push(tmp[i].substring(1, tmp[i].length - 1));
                        } else {
                            args.push(tmp[i]);
                        }
                    }



                    if (action.secure) {

                        console.log('secure action');

                        console.log('Checking ' + from);

                        bot.whois(from, function(info) {

                            var username = info['account'];

                            if (username == null) {
                                bot.say(from, 'Not authorized');
                                return;
                            }

                            var isOp = info['channels'].indexOf('@#OpenEye') > -1 ||
                                    info['channels'].indexOf('@+#OpenEye') > -1;

                            console.log(info);

                            action['func']({
                                bot: bot,
                                db: db,
                                username: username,
                                isOp: isOp,
                                from: from,
                                channel: to,
                                args: args
                            });

                        });
                    } else {

                        action['func']({
                            bot: bot,
                            db: db,
                            username: null,
                            isOp: false,
                            from: from,
                            channel: to,
                            args: args
                        });
                    }

                }
            }
            console.log(from + ' => ' + to + ': ' + message);

        }
    });

    bot.addListener('error', function(e) {
        console.log(e);
    });
});