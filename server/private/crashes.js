
function getCrash(context, crashId, callback) {
    context.db.collection('crashes').findOne(
            {_id: crashId},
    function(err, result) {
        if (result == null) {
            context.bot.say(
                    context.channel,
                    'Crash not found'
                    );
            return;
        }
        callback(result);
    }
    );
}

function setNote(context) {
    
    var args = context.args;

    if (args.length != 2) {
        return false;
    }
        
        var crashId = args[0];
        var note = args[1];
        
        if (!context.isOp && !context.isVoiced) {
        context.bot.say(
            context.bot.channel,
            'Insufficient permissions'        
        );
        return true;
        }
        getCrash(context, crashId, function(crash) {
           context.db.collection('crashes').update(
                   {_id: crashId},
                   {'$set': {'note' : {'message': note, user: context.username}}},
                   function(err) {
                        context.bot.say(
                                context.channel,
                                'Note added!'        
                        );
                   }
            );
        });
        return true;
        
}
function removeNote(context) {
    var args = context.args;

    if (args.length != 1) {
        return false;
    }
        
        var crashId = args[0];
        
        if (!context.isOp && !context.isVoiced) {
        context.bot.say(
            context.channel,
            'Insufficient permissions'        
        );
        return;
        }
        getCrash(context, crashId, function(crash) {
           context.db.collection('crashes').update(
                   {_id: crashId},
                   {'$unset': {'note' : 1}},
                   function(err) {
                        context.bot.say(
                                context.channel,
                                'Note removed!'        
                        );
                   }
            );
        });
        return true;
}


module.exports.setNote = setNote;
module.exports.removeNote = removeNote;