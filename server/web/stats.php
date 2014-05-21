<?php

require_once __DIR__ . '/../vendor/autoload.php';

$app = new Silex\Application();

require __DIR__ . '/../resources/config/prod.php';

require __DIR__ . '/../src/app.php';

header("Content-Type: application/json");

$mongo = $app['mongo'];
$analytics = $mongo['analytics'];
$analytics->connect();
$db = $analytics->analytics;

$defaultConn = $mongo['default'];
$default = $defaultConn->hopper;

$master = array();


$defaultClient = $mongo['default'];
$defaultDb = $defaultClient->hopper;

$mod = isset($_REQUEST['mod']) ? $_REQUEST['mod'] : 'openeye';


$signatures = array();
foreach ($defaultDb->files->find(
    array('mods.modId' => $mod),
    array('_id' => 1)
) as $file) {
    $signatures[] = $file['_id'];
}

if (count($signatures) == 0) {
	exit;
}

$timer = strtotime(date('Y-m-d H:00:00')) - (3600 * 2);

$signatureCounts = array();
$timesCount = array();

foreach ($db->analytics_signatures->find(array(
     '_id.type'     => 'signature',
     '_id.key'      => array('$in' => $signatures),
     '_id.span'     => 'hourly',
     '_id.time'     => array(
         '$gt' => new MongoDate($timer - 86400),
         '$lte' => new MongoDate($timer),
     )
)) as $hour) {
    
    if (!isset($signatureCounts[$hour['_id']['key']])) {
        $signatureCounts[$hour['_id']['key']] = 0;
    }
    if (!isset($timesCount[$hour['_id']['time']->sec])) {
        $timesCount[$hour['_id']['time']->sec] = 0;
    }    
    foreach ($hour['value'] as $set) {
        if (!isset($master[$set['type']])) {
            $master[$set['type']] = array();
        }
        if (!isset($master[$set['type']][$set['key']])) {
            $master[$set['type']][$set['key']] = 0;
        }
        $master[$set['type']][$set['key']] += $set['value'];
        
        if ($set['type'] == 'count') {
            $signatureCounts[$hour['_id']['key']] += $set['value'];
            $timesCount[$hour['_id']['time']->sec] += $set['value'];
        }
    }
}

$filenameCounts = array();

foreach ($default->files->find(
    array('_id' => array('$in' => array_keys($signatureCounts))),
    array('filenames' => 1)
) as $dbFile) {
    $filenameCounts[current($dbFile['filenames'])] = $signatureCounts[$dbFile['_id']];
}
ksort($timesCount);
$master['files'] = $filenameCounts;
$master['times'] = $timesCount;

foreach ($master as $k => $v) {
    arsort($master[$k]);
}
echo json_encode($master, JSON_PRETTY_PRINT);