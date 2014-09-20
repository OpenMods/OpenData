<?php

$rustart = getrusage();
$time_start = microtime(true);

require_once __DIR__ . '/../../vendor/autoload.php';

$app = new Silex\Application();

require __DIR__ . '/../../resources/config/prod.php';
require __DIR__ . '/../app.php';

$mongo = $app['mongo'];
$conn = $mongo['analytics'];
$conn->connect();
$db = $conn->analytics;

$client = new Predis\Client();

/* * ***********************************************
 * Hourly stats
 * ********************************************** */

$currentHour = strtotime(date("Y-m-d H:00:00"));
$previousHour = $currentHour - 3600;

$keys = array('language', 'locale', 'timezone', 'minecraft', 'javaVersion', 'fml', 'mcp', 'forge', 'tags');

foreach ($client->smembers('signatures') as $signature) {
    $items = array();
    $items[] = array(
        'type' => 'count',
        'key' => 'count',
        'value' => (int)$client->get($signature)
    );
    foreach ($keys as $key) {
        foreach ($client->hgetall($signature.':'.$key) as $item => $count) {
            $items[] = array(
                'type' => $key,
                'key' => $item,
                'value' => (int)$count
            );
        }
    }
    $allEntries[] = array(
        '_id' => array(
            'type' => 'signature',
            'key' => $signature,
            'time' => new \MongoDate($previousHour),
            'span' => 'hourly'
        ),
        'value' => $items
    );
}


foreach (array_chunk($allEntries, 20) as $batch) {
  try {
    $db->analytics_signatures->batchInsert($batch, array('continueOnError' => true));
  } catch (Exception $e) { }
}
$client->flushall();


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
