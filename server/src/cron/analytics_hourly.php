<?php

$rustart = getrusage();
$time_start = microtime(true);

require_once __DIR__ . '/../../vendor/autoload.php';

$app = new Silex\Application();

require __DIR__ . '/../../resources/config/prod.php';
require __DIR__ . '/../app.php';

$mongo = $app['mongo'];
$conn = $mongo['default'];
$db = $conn->hopper;

/* * ***********************************************
 * Hourly stats
 * ********************************************** */

$currentHour = strtotime(date("Y-m-d H:00:00"));
$previousHour = $currentHour - 3600;

$reportTypes = array();

$fileStatMap = array();

foreach ($db->reports->find() as $report) {
    $reportTypes[] = $report['type'];
    $docs = array();
    $aggregate = $report['aggregate'];
    array_unshift(
        $aggregate, array('$match' => array('created_at' => array('$gte' => $previousHour, '$lt' => $currentHour)))
    );
    $results = $db->analytics->aggregate($aggregate);

    foreach ($results['result'] as $result) {
        if ($report['type'] == 'signatures') {
            $fileStatMap[$result['_id']] = $result['launches'];
        }
        $docs[] = array(
            '_id' => array(
                'key' => $result['_id'],
                'type' => $report['type'],
                'span' => 'hourly',
                'time' => new \MongoDate($previousHour)
            ),
            'launches' => $result['launches']
        );
    }
    
    if (count($docs) > 0) {
        $db->analytics_aggregated->batchInsert($docs);
    }
}

/***********************************************
 * Update mod stats for homepage listing
 **********************************************/
$mods = array();
$files = $db->files->find(array(
    '_id' => array('$in' => array_keys($fileStatMap))
));
foreach ($files as $file) {
    if (isset($fileStatMap[$file['_id']])) {
        foreach ($file['mods'] as $mod) {
            if (!isset($mods[$mod['modId']])) {
                $mods[$mod['modId']] = 0;
            }
            $mods[$mod['modId']] += $fileStatMap[$file['_id']];
        }
    }
}
foreach ($mods as $mod => $launches) {
    $db->mods->update(
            array('_id' => $mod),
            array('$set' => array(
                'launches' => $launches
            )
        )
    );
}

/**************************************************
 * Daily stats
 **************************************************/


if ((int) date('G', $currentHour) == 0) {

    $today = strtotime(date("Y-m-d 00:00:00"));
    $yesterday = $today - 86400;
    $oYesterday = new \MongoDate($yesterday);

    foreach ($reportTypes as $type) {
        $docs = array();
        $results = $db->analytics_aggregated->aggregate(
            array(
                array(
                    '$where' => array(
                        '_id.time' => array('$gte' => $oYesterday),
                        '_id.type' => $type,
                        '_id.span' => 'hourly'
                    )
                ),
                array('$group' => array('_id' => '$_id.key', 'launches' => array($sum => '$launches')))
            )
        );
        foreach ($results['result'] as $result) {
            
            $docs[] = array(
                '_id' => array(
                    'key' => $result['_id'],
                    'type' => $type,
                    'span' => 'daily',
                    'time' => $oYesterday
                ),
                'launches' => $result['launches']
            );
        }
        if (count($docs) > 0) {
            $db->analytics_aggregated->batchInsert($docs);
        }
    }
    
}

function rutime($ru, $rus, $index) {
    return ($ru["ru_$index.tv_sec"] * 1000 + intval($ru["ru_$index.tv_usec"] / 1000)) - ($rus["ru_$index.tv_sec"] * 1000 + intval($rus["ru_$index.tv_usec"] / 1000));
}

$ru = getrusage();
$time_end = microtime(true);
$timeTaken = $time_end - $time_start;

$date = date("Y-m-d H:i:s");
echo "[" . $date . "] Computations: " . rutime($ru, $rustart, "utime") . "\n";
echo "[" . $date . "] System calls: " . rutime($ru, $rustart, "stime") . "\n";
echo "[" . $date . "] Memory: " . memory_get_peak_usage() . "\n";
echo "[" . $date . "] Clock time: " . $timeTaken . "\n";
