var MongoClient = require('mongodb').MongoClient
var time = new Date();
time.setMinutes(0);
time.setSeconds(0);
time.setMilliseconds(0);
var endTime = Math.floor(time.getTime() / 1000);
var startTime = endTime - (60 * 60);

var dbUser = process.argv[2];
var dbPass = process.argv[3];

var connectionString = 'mongodb://' + dbUser + ':' + dbPass + '@localhost:27017/hopper';
console.log({
    created_at: {
        '$gte': startTime,
        '$lt': endTime
    }
});

MongoClient.connect(connectionString, function (err, db) {

    db.collection('analytics').mapReduce(
        function () {
            var time = new Date();
            time.setMinutes(0);
            time.setSeconds(0);
            time.setMilliseconds(0);
            var endTime = Math.floor(time.getTime() / 1000);
            var startTime = endTime - (60 * 60);

            if (this.signatures == null || this.signatures.length == 0) {
                return;
            }
            var self = this;
            var result = {
                'branding': {},
                'packsize' : {},
                'fml': {},
                'forge': {},
                'mcp': {},
                'tags': {},
                'count': NumberInt(1)
            };

            ['locale', 'minecraft', 'language', 'javaVersion', 'timezone'].forEach(function (k) {
                result[k] = {};
                var key = self[k];
                if (key != null) {
                    result[k][key] = NumberInt(1);
                }
            });
            result['packsize'][this.signatures.length] = NumberInt(1);

            this.branding.forEach(function (brand) {
                    if (brand != null) {
                            var matches = brand.match(/^[0-9]+ mods loaded, [0-9]+ mods active$/i);
                            if (matches == null || matches.length == 0) {
                                result['branding'][brand] = NumberInt(1);
                            }
                    }
            });

            for (key in this.runtime) {
                var version = this.runtime[key];
                if (result[key] == null) result[key] = {};
                result[key][version] = NumberInt(1);
            }

            if (this.tags != null) {
                this.tags.forEach(function (tag) {
                    result['tags'][tag] = NumberInt(1);
                });
            }

            this.signatures.forEach(function (signature) {

            	result['added'] = self.addedSignatures.indexOf(signature.signature) > -1 ? 1 : 0;
            	result['removed'] = self.removedSignatures.indexOf(signature.signature) > -1 ? 1 : 0;

                print('emit!');
                this.emit({
                    type: 'signature',
                    key: signature.signature,
                    time: new Date(NumberInt(startTime) * 1000),
                    span: 'hourly'
                }, result);
            });
            print('finished mapping');
        },
        function (key, docs) {
            var result = {
                'locale': {},
                'minecraft': {},
                'packsize' : {},
                'language': {},
                'timezone': {},
                'javaVersion': {},
                'branding': {},
                'tags': {},
                'runtime': {},
                'count': 0,
                'added': 0,
                'removed': 0,
                'fml': {},
                'forge': {},
                'mcp' : {}
            };
            docs.forEach(function (doc) {
                ['locale', 'minecraft', 'language', 'javaVersion', 'timezone', 'branding', 'tags', 'packsize', 'forge', 'fml', 'mcp'].forEach(function (k) {
                    for (x in doc[k]) {
                        if (result[k][x] == null) result[k][x] = 0;
                        result[k][x] += doc[k][x];
                        result[k][x] = NumberInt(result[k][x]);
                    }
                });
                result.count += doc.count;
                result.added += doc.added;
                result.removed += doc.removed;
            });
            result.count = NumberInt(result.count);
            result.added = NumberInt(result.added);
            result.removed = NumberInt(result.removed);
            return result;
        }, {
			finalize: function(key, reducedVal) {
				var reduced = [];
				reduced.push({
					'type' : 'count',
					'key': 'count',
					'value' : NumberInt(reducedVal.count)
				});
				reduced.push({
					'type' : 'added',
					'key': 'added',
					'value' : NumberInt(reducedVal.added)
				});
				reduced.push({
					'type' : 'removed',
					'key': 'removed',
					'value' : NumberInt(reducedVal.removed)
				});
				['locale', 'minecraft', 'language', 'javaVersion', 'timezone', 'branding', 'tags', 'packsize', 'forge', 'fml', 'mcp'].forEach(function(type) {
					if (reducedVal[type] != null) {
						for (key in reducedVal[type]) {
							reduced.push({
								'type': type,
								'key' : key,
								'value' : NumberInt(reducedVal[type][key])
							});
						}
					}
				});
				return reduced;
			},
            verbose: true,
            query: {
                created_at: {
                    '$gte': startTime,
                    '$lt': endTime
                }
            },
            out: 'analytics_signatures'
        },
        function (err, results) {
            if (err != null) {
                console.log("-----ERROR-------");
                console.log(err);
            } else {
                console.log('ok WE GOT RESULTS');
                console.log(results);
            }

            db.collection('analytics').remove({
                    created_at: {
                        '$gte': startTime,
                        '$lt': endTime
                    }
                },
                function (err, result) {
                    db.close();
                }
            );
        }
    )
});