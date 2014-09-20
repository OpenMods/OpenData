<?php

require_once __DIR__ . '/../../vendor/autoload.php';

$app = new Silex\Application();

require __DIR__ . '/../../resources/config/prod.php';
require __DIR__ . '/../app.php';

$mongo = $app['mongo'];
$conn = $mongo['default'];
$analyticsConn = $mongo['analytics'];
$analyticsConn->connect();

$timer = strtotime(date('Y-m-d H:00:00')) - 3600;

$db = $analyticsConn->selectDB('analytics');

$results = $db->analytics_signatures->aggregate(array(
    array('$match' => array(
        '_id.type' => 'signature',
        '_id.span' => 'hourly',
        '_id.time' => new MongoDate($timer)
    )),
    array('$unwind' => '$value'),
    array('$match' => array(
        'value.type' => 'count'
    )),
    array('$project' => array(
        '_id.key' => 1,
        'value.value' => 1
    ))
));

$signatures = array();

$modCounts = array();


foreach ($results['result'] as $result) {
    $signatures[$result['_id']['key']] = $result['value']['value'];
}

$mainDb = $conn->selectDB('hopper');
$files = $mainDb->files->find(
    array('_id' => array(
        '$in' => array_keys($signatures))
    ),
    array('mods.modId')
);

foreach ($files as $file) {
    $id = $file['_id'];
    if (isset($file['mods'])) {
        foreach ($file['mods'] as $mod) {
            if (!isset($modCounts[$mod['modId']])) {
                $modCounts[$mod['modId']] = 0;
            }
            $modCounts[$mod['modId']] += $signatures[$id];
            break;
        }
    }
}

$mainDb->mods->update(
    array(),
    array('$set' => array('launches' => 0)),
    array('multiple' => true)
);

foreach ($modCounts as $modName => $launches) {
    $mainDb->mods->update(
        array('_id' => $modName),
        array(
            '$set' => array('launches' => $launches)
        )
    );
}

$minecraftVersions = $db->analytics_signatures->aggregate(array(
    array('$match' => array(
         '_id.type' => 'signature',
         '_id.span' => 'hourly',
         '_id.time' => new MongoDate($timer)
    )),
    array('$unwind' => '$value'),
    array('$match'  => array(
        'value.type' => 'minecraft'
    )),
    array('$group' => array(
         '_id'   => '$_id.key',
         'value' => array('$first' => '$value.key'),
         'count' => array('$max'   => '$value.value')
    ))
));

foreach ($minecraftVersions['result'] as $result) {
    $mainDb->files->update(
        array('_id' => $result['_id']),
        array(
            '$set' => array('minecraft' => $result['value'])
        )
    );
}