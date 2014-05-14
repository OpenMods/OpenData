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
console.log({created_at: {'$gte' : startTime, '$lt' : endTime }});

MongoClient.connect(connectionString, function(err, db) {

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
                'runtime': {},
                'tags': {},
                'count': NumberInt(1)
            };

            ['locale', 'minecraft', 'language', 'javaVersion', 'timezone'].forEach(function (k) {
                result[k] = {};
                var key = self[k];
                if (key != null) {
                        key = key.replace(/\./g, '~');
                        result[k][key] = NumberInt(1);
                }
            });

            this.branding.forEach(function (brand) {
                result['branding'][brand.replace(/\./g, '~')] = NumberInt(1);
            });

            for (key in this.runtime) {
                var version = this.runtime[key];
                if (result['runtime'][key] == null) result['runtime'][key] = {};
                result['runtime'][key][version.replace(/\./g, '~')] = NumberInt(1);
            }

            if (this.tags != null) {
                this.tags.forEach(function (tag) {
                    result['tags'][tag.replace(/\./g, '~')] = NumberInt(1);
                });
            }
            this.signatures.forEach(function (signature) {
                print('emit!');
                this.emit({
                    type: 'signature',
                    key: signature.signature,
                    time: NumberInt(startTime)
                }, result);
            });
            print('finished mapping');
          },
          function (key, docs) {
            var result = {
                'locale': {},
                'minecraft': {},
                'language': {},
                'timezone': {},
                'javaVersion': {},
                'branding': {},
                'tags': {},
                'runtime': {},
                'count': 0
            };
            docs.forEach(function (doc) {
                ['locale', 'minecraft', 'language', 'javaVersion', 'timezone', 'branding', 'tags'].forEach(function (k) {
                    for (x in doc[k]) {
                        if (result[k][x] == null) result[k][x] = 0;
                        result[k][x] += doc[k][x];
                        result[k][x] = NumberInt(result[k][x]);
                    }
                });
                for (ware in doc.runtime) {
                    for (version in doc.runtime[ware]) {
                        if (result['runtime'][ware] == null) result['runtime'][ware] = {};
                        if (result['runtime'][ware][version] == null) {
                            result['runtime'][ware][version] = 0;
                        }
                        result['runtime'][ware][version] += 1;
                        result['runtime'][ware][version] = NumberInt(result['runtime'][ware][version]);
                    }
                }
                result.count += doc.count;
            });
            result.count = NumberInt(result.count);
            return result;
          }, {
            verbose: true,
            query: {
                created_at: {'$gte' : startTime, '$lt' : endTime }
            },
            out: 'analytics_signatures'
          },
          function(err, results) {
              if (err != null) {
                console.log("-----ERROR-------");
                console.log(err);
              } else {
                console.log('ok WE GOT RESULTS');
                console.log(results);
              }

                db.collection('analytics').remove(
                        {
                                created_at: {'$gte' : startTime, '$lt' : endTime }
                        },
                        function(err, result) {
                                db.close();
                        }
                );
           }
        )
});