<?php

$mongo = new MongoClient();

$db = $mongo->hopper;

$currentHour = strtotime(date("Y-m-d H:00:00"));
$previousHour = $currentHour - 3600;

$results = $db->analytics->aggregate(array(
    array('$match' => array('created_at' => array('$gte' => $previousHour, '$lt' => $currentHour))),
    array('$unwind' => '$signatures'),
    array('$group' => array('_id' => '$signatures.signature', 'launches' => array('$sum' => 1)))
));


$entries = array();
foreach ($results['result'] as $result) {
    $entries[] = array(
        'file' => $result['_id'],
        'launches' => $result['launches'],
        'time' => new MongoDate($currentHour)
    );
}

if (count($entries) > 0) {
    $db->files_hourly->batchInsert($entries);
}

