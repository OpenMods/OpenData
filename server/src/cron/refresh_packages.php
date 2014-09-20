<?php

require_once __DIR__ . '/../../vendor/autoload.php';

$app = new Silex\Application();

require __DIR__ . '/../../resources/config/prod.php';
require __DIR__ . '/../app.php';

$mongo = $app['mongo'];
$conn = $mongo['default'];

$db = $conn->selectDB('hopper');

$db->packages->drop();
$db->createCollection('packages');
$db->packages->ensureIndex(array('parent' => 1));

$packages = $db->files->distinct('packages');

$foundPackages = array();
$docs = array();
foreach ($packages as $package) {
    $l = 0;
    $previousPackage = null;
    while ($l = strpos($package, '.', $l)) {
        $subpackage = substr($package, 0, $l++);
        if (!isset($foundPackages[$subpackage])) {
            $docs[] = array(
                '_id' => $subpackage,
                'parent' => $previousPackage
            );
        }
        $previousPackage = $subpackage;
        $foundPackages[$subpackage] = 1;
    }
}

$db->packages->batchInsert($docs);
