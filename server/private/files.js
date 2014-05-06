var colors = require('irc-colors');

function addNote(context) {

	var args = context.args;

	if (args.length < 3 || args.length > 4) {
		return false;
	}
	
	var signature = args[0];
	var level = parseInt(args[1]);
	var description = args[2];
	var payload = null;
	if (args.length > 3) {
		payload = args[3];
	}
	context.db.collection('files').findOne(
		{_id : signature},
		function(err, file){ 
			if (file == null) {
				context.bot.say(
					context.channel,
					'File not found'
				);
				return;
			}
			
			var modIds = [];
			
			file.mods.forEach(function(mod) {
				modIds.push(mod.modId);
			});
			
			if (modIds.length == 0) {
			
				context.bot.say(
					context.channel,
					'Some kind of issue. Mod not found? weeird!'
				);
				return;
			}
			
			context.db.collection('mods').find(
				{_id : {'$in': modIds}}
			).toArray(function(err, results) {
				
				var allowed = context.isOp;
				
				results.forEach(function(mod) {
					if (mod['admins'] != null) {
						if (mod['admins'].indexOf(context.username) > -1) {
							allowed = true;
						}
					}
				});
				
				if (allowed) {
					
					var notes = [];
					if (file['notes'] != null) {
						notes = file['notes'];
					}
					var note = {
						'level' : level,
						'description' : description
					};
					
					if (payload != null) {
						note['payload'] = payload;
					}
					
					notes.push(note);
					
					context.db.collection('files').update(
						{_id: signature },
						{
							'$set' : {
								'notes' : notes
							}
						},
						{},
						function(err) {
							context.bot.say(
								context.channel,
								'Added note'
							);
						}
					);
					
				} else {
					context.bot.say(
						context.channel,
						'Insufficient permissions'
					);
				}
				
			});
			
		}
	);
	return true;
	
}

function listNotes(context) {

	if (context.args.length != 1) {
		return false;
	}
		
	var signature = context.args[0];
	
	context.db.collection('files').findOne(
		{_id : signature},
		function(err, file){ 
			if (file == null) {
				context.bot.say(
					context.channel,
					'File not found'
				);
				return;
			}
			
			if (file['notes'] === undefined || file['notes'] === null || file['notes'].length == 0) {
				context.bot.say(
					context.channel,
					'No notes found'
				);
				return;
			}
			
			var notes = file['notes'];
			
			var index = 1;
			notes.forEach(function(note) {
				
				var msg = '[' + index + '] ';
				msg += 'Level: ' + note['level'];
				msg += '    Description: ' + note['description'];
				if (note['payload'] != null) {
					msg += '    Payload: '+ note['payload'] 
				}
				
				context.bot.say(
					context.channel,
					msg
				);
				
				index++;
			
			});
			
		}
	);
	return true;

}

function getLatest(context) {
    context.db.collection('files').find().sort({
       '$natural' : -1 
    }).limit(5).toArray(function(err, results) {
        results.forEach(function(result) {
            var mods = [];
            if (result['mods'] != null) {
                result['mods'].forEach(function(mod) {
                   mods.push(mod.name + ' (' +mod.modId + ')');
                });
            }
            var txt = colors.navy(result.filenames[0]);
            if (mods.length > 0) {
                txt += colors.green(' contains: ' + mods.join(', '));
            }
            context.bot.say(
                context.channel,
                txt
            );
        });
    });    
}

function removeNote(context) {

	var args = context.args;

	if (args.length != 2) {
		return false;
	}
	
	var signature = args[0];
	var index = parseInt(args[1]);

	context.db.collection('files').findOne(
		{_id : signature},
		function(err, file){ 
			if (file == null) {
				context.bot.say(
					context.channel,
					'File not found'
				);
				return;
			}
			
			var modIds = [];
			
			file.mods.forEach(function(mod) {
				modIds.push(mod.modId);
			});
			
			if (modIds.length == 0) {
			
				context.bot.say(
					context.channel,
					'Some kind of issue. Mod not found? weeird!'
				);
				return;
			}
			
			context.db.collection('mods').find(
				{_id : {'$in': modIds}}
			).toArray(function(err, results) {
				
				var allowed = context.isOp;
				
				results.forEach(function(mod) {
					if (mod['admins'] != null) {
						if (mod['admins'].indexOf(context.username) > -1) {
							allowed = true;
						}
					}
				});
				
				if (allowed) {
					
					var notes = [];
					if (file['notes'] != null) {
						notes = file['notes'];
					}
					
					index--;
					
					if (notes[index] == null) {
						context.bot.say(
							context.channel,
							'No note at index ' + index
						);
						return;
					}
					notes.splice(index, 1);
					
					context.db.collection('files').update(
						{_id: signature },
						{
							'$set' : {
								'notes' : notes
							}
						},
						{},
						function(err) {
							context.bot.say(
								context.channel,
								'Removed note'
							);
						}
					);
					
				} else {
					context.bot.say(
						context.channel,
						'Insufficient permissions'
					);
				}
				
			});
			
		}
	);
	return true;
}

module.exports.addNote = addNote;
module.exports.listNotes = listNotes;
module.exports.removeNote = removeNote;
module.exports.getLatest = getLatest;