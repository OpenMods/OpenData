
require('datejs');
var colors = require('irc-colors');


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



var validFields = [
    'name', 'description', 'url',
    'donation', 'authors', 'releasesPage',
    'tags', 'repository', 'irc', 'credits', 'admins'
];

function findModsBy(context, query) {

    context.db.collection('mods').find(
            query
            ).toArray(function(err, results) {
        var response = [];
        var i = 0;
        results.forEach(function(result) {
            if (i < 10) {
                response.push(result.name + ' (' + result._id + ')');
            }
            i++;
        });
        if (response.length > 0) {
            var msg = i + ' results found.';
            if (i > 10) {
                msg = msg + ' Limiting to 10!';
            }
            context.bot.say(context.channel, msg);
            context.bot.say(context.channel, response.join(', '));
        } else {
            context.bot.say(context.channel, 'No results found');
        }
    });
}

function findModsByAuthor(context) {
    var args = context.args;

    if (args.length != 1) {
        return false;
    }

    var regex = null;

    try {
        regex = new RegExp(args[0], 'i');
    } catch (e) {
        return false;
    }

    findModsBy(context, {'authors': regex});
    return true;
}

function findMods(context) {

    var args = context.args;

    if (args.length != 1) {
        return false;
    }

    var regex = null;

    try {
        regex = new RegExp(args[0], 'i');
    } catch (e) {
        return false;
    }

    findModsBy(context, {'$or': [{_id: regex}, {name: regex}]});
    return true;
}

function removeAdmin(context) {

    var args = context.args;

    if (!context.isOp) {
        context.bot.say(context.channel, 'Sorry, but no.');
        return true;
    }

    if (args.length == 2) {

        var modId = args[0];
        var username = args[1];

        getMod(context, modId, false, function(mod) {

            var admins = [];
            if (mod['admins'] != null) {
                admins = mod['admins'];
            }
            admins.remove(username);

            context.db.collection('mods').update(
                    {'_id': modId},
            {'$set': {
                    'admins': admins
                }},
            {},
                    function(err) {
                        if (err) {
                            context.bot.say(
                                    context.channel,
                                    err.message
                                    );
                            return;
                        }
                        context.bot.say(
                                context.channel,
                                'Update successful'
                                );
                    }

            );

        });
        return true;
    } else {
        return false;
    }
}

function addAdmin(context) {

    var args = context.args;

    if (!context.isOp) {
        context.bot.say(context.channel, 'Sorry, but no.');
        return true;
    }

    if (args.length == 2) {

        var modId = args[0];
        var username = args[1];

        getMod(context, modId, false, function(mod) {

            var admins = [];
            if (mod['admins'] != null) {
                admins = mod['admins'];
            }
            if (admins.indexOf(username) == -1) {
                admins.push(username);
            }

            context.db.collection('mods').update(
                    {'_id': modId},
            {'$set': {
                    'admins': admins
                }},
            {},
                    function(err) {
                        if (err) {
                            context.bot.say(
                                    context.channel,
                                    err.message
                                    );
                            return;
                        }
                        context.bot.say(
                                context.channel,
                                'Update successful'
                                );
                    }

            );

        });
        return true;
    }
    return false;

}

function getMod(context, modId, requiresPermissions, callback) {
    context.db.collection('mods').findOne(
            {_id: modId},
    function(err, result) {
        if (result == null) {
            context.bot.say(
                    context.channel,
                    'Mod not found'
                    );
            return true;
        }
        if (requiresPermissions && !context.isOp) {
            if (result['admins'] == null || result['admins'].indexOf(context.username) == -1) {
                context.bot.say(
                        context.channel,
                        'Not authorized'
                        );
                return true;
            }
        }
        if (result['unlisted'] != null && result['unlisted'] === true) {
            context.bot.say(
                        context.channel,
                        'No information is available for this mod'
                        );
                return true;
        }
        callback(result);
    }
    );
}

function setField(context) {

    if (context.args.length >= 3) {

        var modId = context.args[0];
        var field = context.args[1];
        var value = context.args[2];

        if (field == 'admins') {
            context.bot.say(context.channel, 'Use mod:admins:add or mod:admins:remove');
            return;
        }

        context.args.shift();
        context.args.shift();

        if (field == 'irc' && context.args.length != 2) {
            context.bot.say(context.channel, 'Not enough arguments');
            return true;
        }

        if (validFields.indexOf(field) == -1) {
            context.bot.say(
                    context.channel,
                    'Invalid field'
                    );
            return true;
        }

        getMod(context, modId, true, function(mod) {

            if (field == 'irc') {
                value = {'host': context.args[0], 'channel': context.args[1]};
            } else if (field == 'authors' || field == 'tags') {
                value = context.args;
            }

            var set = {};
            set[field] = value;

            context.db.collection('mods').update(
                    {'_id': modId},
            {'$set': set},
            {},
                    function(err) {
                        if (err) {
                            context.bot.say(
                                    context.channel,
                                    err.message
                                    );
                            return true;
                        }
                        context.bot.say(
                                context.channel,
                                'Update successful'
                                );
                    }

            );

        });
        return true;
    }
    return false;

}

function unsetField(context) {

    var args = context.args;

    if (args.length == 2) {

        var modId = args[0];
        var field = args[1];


        if (validFields.indexOf(field) == -1) {
            context.bot.say(
                    context.channel,
                    'Invalid field'
                    );
            return;
        }

        getMod(context, modId, true, function(mod) {

            var action = {};
            var tmp = {};

            if (field == 'tags' || field == 'authors') {
                tmp[field] = [];
                action['$set'] = tmp;
            } else if (field == 'irc') {
                tmp[field] = 1;
                action['$unset'] = tmp;
            } else {
                tmp[field] = '';
                action['$set'] = tmp;
            }


            context.db.collection('mods').update(
                    {'_id': modId},
            action,
                    {},
                    function(err) {
                        if (err) {
                            context.bot.say(
                                    context.channel,
                                    err.message
                                    );
                            return;
                        }
                        context.bot.say(
                                context.channel,
                                'Update successful'
                                );
                    }

            );
        });
        return true;
    }
    return false;

}

//<type> <span> <key> <from> <to>
function getStats(context) {

    var args = context.args;

    if (args.length == 5) {

        var span = args[0];
        var type = args[1];
        var key = args[2];
        var from = args[3];
        var to = args[4];

        var fromDate = Date.parse(from);
        var toDate = Date.parse(to);

        if (fromDate == null) {
            context.bot.say(
                    context.channel,
                    'Invalid <from> date string'
                    );
            return;
        }

        if (toDate == null) {
            context.bot.say(
                    context.channel,
                    'Invalid <to> date string'
                    );
            return;
        }
        
        fromDate.setMinutes(0);
        fromDate.setSeconds(0);
        fromDate.setMilliseconds(0);
        
        toDate.setMinutes(0);
        toDate.setSeconds(0);
        toDate.setMilliseconds(0);
        
        if (type == 'mod') {
            context.db.collection('files').find(
               {'mods.modId' : key}
            ).toArray(function(err, results) {
                var fileIds = [];
                results.forEach(function(result) {
                    fileIds.push(result._id);
                });
                if (fileIds.length == 0) {
                    return true;
                }
                compileStats(context, {
                    '_id.span': span,
                    '_id.time': {
                        '$gte': fromDate,
                        '$lt': toDate
                    },
                    '_id.type': type,
                    '_id.key': {'$in': fileIds}
                }, fromDate, toDate);
            });
        } else {
            compileStats(context, {
                '_id.span': span,
                '_id.time': {
                    '$gte': fromDate,
                    '$lt': toDate
                },
                '_id.type': type,
                '_id.key': key
            }, fromDate, toDate);
        }
        return true;
    }
    return false;
}

function compileStats(context, query, fromDate, toDate) {
    context.db.collection('analytics_aggregated').find(
        query
    ).toArray(function(err, results) {
        var launches = 0;
        results.forEach(function(result) {
            launches += result['launches'];
        });
        context.bot.say(
            context.channel,
            colors.navy(fromDate.toString("dddd, MMMM dd, yyyy HH:00:00") + ' - ' +
            toDate.toString("dddd, MMMM dd, yyyy HH:00:00") + ': ') +
            colors.green(launches + ' launches')
        );
    });
}

function getFields(context) {

    if (context.args.length > 0) {

        var modId = context.args[0];
        context.args.shift();
        var fields = context.args;

        getMod(context, modId, false, function(mod) {

            fields.forEach(function(field) {

                if (validFields.indexOf(field) == -1) {

                    context.bot.say(
                            context.channel,
                            colors.red('Invalid field: ' + field)
                            );

                } else {

                    if (mod[field] === undefined || mod[field] === null) {
                        context.bot.say(
                                context.channel,
                                colors.navy('Field not found: ' + field)
                                );
                    } else {
                        var fieldData = mod[field];

                        if (field == 'irc') {
                            context.bot.say(
                                    context.channel,
                                    colors.navy('irc: ' + fieldData.host + ' ') + colors.green(fieldData.channel)
                                    );
                        } else if (Array.isArray(fieldData)) {
                            context.bot.say(
                                    context.channel,
                                    colors.navy(field + ': ') + colors.green(fieldData.join(' '))
                             );
                        } else {
                            context.bot.say(
                                    context.channel,
                                    colors.navy(field + ': ') + colors.green(fieldData)
                           );
                        }
                    }
                }
            });
        });

        return true;
    }
    return false;

}

module.exports.findMods = findMods;
module.exports.getFields = getFields;
module.exports.setField = setField;
module.exports.unsetField = unsetField;
module.exports.addAdmin = addAdmin;
module.exports.removeAdmin = removeAdmin;
module.exports.findModsByAuthor = findModsByAuthor;
module.exports.getStats = getStats;