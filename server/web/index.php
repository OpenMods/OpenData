<?php

$time_start = microtime(true);

require_once __DIR__ . '/../vendor/autoload.php';

$app = new Silex\Application();

require __DIR__ . '/../resources/config/prod.php';

require __DIR__ . '/../src/app.php';


$app['http_cache']->run();
/*
$time_end = microtime(true);
$timeTaken = $time_end - $time_start;

if ($timeTaken > 30) {
    $mongo->hopper->slow_requests->insert(array(
        'request' => json_decode($request->get('api_request', '[]'), true)
    ));
}
*/