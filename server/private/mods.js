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
    'name', 'description', 'url', 'parent',
    'donation', 'authors', 'releasesPage', 'credits',
    'tags', 'repository', 'irc', 'credits', 'admins'
];


function findMods(context) {

    var args = context.args;

    if (args.length != 1) {
        context.bot.say(context.channel, 'Invalid number of arguments. Expected 1');
        return;
    }

    var regex = null;

    try {
        regex = new RegExp(args[0], 'i');
    }catch (e) {
        context.bot.say(context.channel, 'Bad regular expression.. noob.');
        return;
    }

    context.db.collection('mods').find(
            {_id: regex}
    ).toArray(function(err, results) {
        var response = [];
        var i = 0;
        results.forEach(function(result) {
            if (i < 50) {
                response.push(result.name + ' (' + result._id + ')');
            }
            i++;
        });
        if (response.length > 0) {
            var msg = i + ' results found.';
            if (i > 50) {
                msg = msg + ' Limiting to 50!';
            }
            context.bot.say(context.channel, msg);
        	context.bot.say(context.channel, response.join(', '));
        } else {
            context.bot.say(context.channel, 'No results found');
		}
    });
}

function removeAdmin(context) {

    var args = context.args;

    if (!context.isOp) {
        context.bot.say(context.channel, 'Sorry, but no.');
        return;
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

    } else {
        context.bot.say(context.channel, 'Bad usage');
    }
}

function addAdmin(context) {

    var args = context.args;

    if (!context.isOp) {
        context.bot.say(context.channel, 'Sorry, but no.');
        return;
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

    } else {
        context.bot.say(context.channel, 'Bad usage');
    }
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
            return;
        }
        if (requiresPermissions && !context.isOp) {
            if (result['admins'] == null || result['admins'].indexOf(context.username) == -1) {
                context.bot.say(
                        context.channel,
                        'Not authorized'
                        );
                return;
            }
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
            return;
        }

        if (validFields.indexOf(field) == -1) {
            context.bot.say(
                    context.channel,
                    'Invalid field'
                    );
            return;
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
                            return;
                        }
                        context.bot.say(
                                context.channel,
                                'Update successful'
                                );
                    }

            );

        });
    } else {
        context.bot.say(context.channel, 'Bad usage');
    }

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
    } else {
        context.bot.say(context.channel, 'Bad usage');
    }

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
                            'Invalid field: ' + field
                            );

                } else {

                    if (mod[field] === undefined || mod[field] === null) {
                        context.bot.say(
                                context.channel,
                                'Field not found: ' + field
                                );
                    } else {
                        var fieldData = mod[field];

                        if (field == 'irc') {
                            context.bot.say(
                                    context.channel,
                                    'irc: ' + fieldData.host + ' ' + fieldData.channel
                                    );
                        } else if (Array.isArray(fieldData)) {
                            context.bot.say(
                                    context.channel,
                                    field + ': ' + fieldData.join(' ')
                                    );
                        } else {
                            context.bot.say(
                                    context.channel,
                                    field + ': ' + fieldData
                                    );
                        }
                    }
                }
            });
        });


    } else {
        context.bot.say(context.channel, 'Bad usage');
    }

}

module.exports.findMods = findMods;
module.exports.getFields = getFields;
module.exports.setField = setField;
module.exports.unsetField = unsetField;
module.exports.addAdmin = addAdmin;
module.exports.removeAdmin = removeAdmin;