<?php
require __DIR__ . '/db.php';
//$app['debug'] = true;

$app['log.level'] = Monolog\Logger::ERROR;
$app['api.version'] = "v1";
$app['api.endpoint'] = "/api";
