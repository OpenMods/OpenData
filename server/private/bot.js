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

if (process.argv.length != 5) {
    process.exit();
}

var dbUser = process.argv[2];
var dbPass = process.argv[3];
var password = process.argv[4];


var myNick = 'MissOpenEye';
var mainChannel = '#OpenEye';
var connectionString = 'mongodb://' + dbUser + ':' + dbPass + '@openeye.openmods.info:27017/hopper';

var MongoClient = require('mongodb').MongoClient
var irc = require('irc');
var mods = require('./mods.js');
var files = require('./files.js');
var crashes = require('./crashes.js');
var redis = require("redis"), redisClient = redis.createClient();
var Levenshtein = require('levenshtein');


var actions = {
    'mod:find': {
        func: mods.findMods,
        secure: false,
        usage: '#mod:find <regex>'
    },
    'mod:find:author': {
        func: mods.findModsByAuthor,
        secure: false,
        usage: '#mod:find:authors <regex>'
    },
    'mod:get': {
        func: mods.getFields,
        secure: false,
        usage: '#mod:get <modid> <field1> <field2>... (' + [    'name', 'description', 'url',
    		'donation', 'authors', 'releasesPage',
    		'tags', 'repository', 'irc', 'credits', 'admins', ].join(', ') + ')'
    },
    'mod:stats': {
        func: mods.getStats,
        secure: false,
        usage: '#mod:stats <modid> <time>'
    },
    'mod:update': {
        func: mods.setField,
        secure: true,
        usage: '#mod:update <modid> <field> <value>'
    },
    'mod:unset': {
        func: mods.unsetField,
        secure: true,
        usage: '#mod:unset <modid> <field>'
    },
    'mod:admins:add': {
        func: mods.addAdmin,
        secure: true,
        usage: '#mod:admins:add <modid> <username>'
    },
    'mod:admins:remove': {
        func: mods.removeAdmin,
        secure: true,
        usage: '#mod:admins:remove <modid> <username>'
    },
    'file:notes:add': {
        func: files.addNote,
        secure: true,
        usage: '#file:notes:add <signature> <level> <description> <payload>'
    },
    'file:notes:list': {
        func: files.listNotes,
        secure: false,
        usage: '#file:notes:list <signature>'
    },
    'file:notes:remove': {
        func: files.removeNote,
        secure: true,
        usage: '#file:notes:remove <signature> <index>'
    },
    'crash:note:set': {
        func: crashes.setNote,
        secure: true,
        usage: '#crash:note:set <hash> <note>'
    },
    'crash:note:remove': {
        func: crashes.removeNote,
        secure: true,
        usage: '#crash:note:set <hash>'
    },
    'commands': {
        func: function(context) {
            var keys = [];
            for (key in actions) {
                keys.push(key);
            }
            context.bot.say(context.channel, keys.join(', '));
            return true;
        },
        secure: false,
        usage: '#commands'
    }
};

MongoClient.connect(connectionString, function(err, db) {

    var bot = new irc.Client('irc.esper.net', myNick, {
        channels: [mainChannel],
        autoConnect: false,
        debug: true
    });
    
    bot.connect(5, function() {
       bot.say('nickserv', 'identify OpenEye ' + password);
    });
    
    redisClient.on('message', function (channel, message) {
        if (channel == 'crash') {
        
            var packet = JSON.parse(message);
            var modIds = packet['modIds'];
            var content = packet['content'];
            
            if (modIds.length > 0) {
                db.collection('mods').find({
                    _id: {'$in': modIds}
                }).toArray(function(err, results) {
                    results.forEach(function(mod) {
                       if (mod['irc'] != null) {
                            var irc = mod['irc'];
                            if (irc['host'] == 'irc.esper.net' && irc['report'] != null && irc['report'] == true) {
                                bot.join(irc['channel'], function() {
                                    bot.say(irc['channel'], content);
                                    setTimeout(function() {
                                        bot.part(irc['channel'], 'Bye!', function() {})
                                    }, 2000);
                                });
                            }
                       } 
                    });
                });
            }
            
            bot.say('#OpenEye', content);
            
        } else {
            bot.say('#OpenEye', message);
        }
    });
    
    redisClient.on('subscribe', function(channel, count) {
       console.log('subscribed to ' + channel); 
    });



    redisClient.subscribe('file');
    redisClient.subscribe('crash');


    bot.addListener('message', function(from, to, message) {

        if (message.startsWith('#')) {

            var matches = message.match(/^#([a-z0-9\:]+)\s?(.*)?$/i);

            if (matches.length > 0) {

                var action = actions[matches[1]];
                
                if (action == null) {
                    var sorted = [];
                    for (cmd in actions) {
                        var l = new Levenshtein(matches[1], cmd);
                        sorted.push({
                            'distance' : l.distance,
                            'action' : actions[cmd]                  
                        });
                    }
                    sorted.sort(function(a, b) {
                        return (a.distance - b.distance);
                    });
                    action = sorted[0].action
                }

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
                            
                            var isVoiced = info['channels'].indexOf('+#OpenEye') > -1 ||
                                    info['channels'].indexOf('@+#OpenEye') > -1;

                            if (!action['func']({
                                bot: bot,
                                db: db,
                                username: username,
                                isOp: isOp,
                                isVoiced: isVoiced,
                                from: from,
                                channel: to,
                                args: args
                            })) {
            			bot.say(to, 'Usage: ' + action.usage);
                            }

                        });
                    } else {

                        if (!action['func']({
                            bot: bot,
                            db: db,
                            username: null,
                            isOp: false,
                            isVoiced: false,
                            from: from,
                            channel: to,
                            args: args
                        })) {
            			bot.say(to, 'Usage: ' + action.usage);
                        }
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