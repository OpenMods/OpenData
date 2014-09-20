<?php

use Silex\Application;
use Silex\Provider\HttpCacheServiceProvider;
use Silex\Provider\MonologServiceProvider;
use Silex\Provider\ServiceControllerServiceProvider;
use Mongo\Silex\Provider\MongoServiceProvider;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\HttpFoundation\Request;
use Silex\Provider\FormServiceProvider;

date_default_timezone_set('Etc/UTC');

define("ROOT_PATH", __DIR__ . "/..");

$time_start = microtime(true);

$app->before(function (Request $request) {
    if ($request->getMethod() === "OPTIONS") {
        $response = new Response();
        $response->headers->set("Access-Control-Allow-Origin", "*");
        $response->headers->set("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,OPTIONS");
        $response->headers->set("Access-Control-Allow-Headers", "Content-Type");
        $response->setStatusCode(200);
        $response->send();
    }
}, Application::EARLY_EVENT);

//handling CORS respons with right headers
$app->after(function (Request $request, Response $response) use ($app, $time_start) {
    $response->headers->set("Access-Control-Allow-Origin", "*");
    $response->headers->set("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,OPTIONS");

    $time_end = microtime(true);
    $timeTaken = $time_end - $time_start;
    if ($timeTaken > 20) {
        $app['mongo']['default']->hopper->slow_requests->insert(array(
            'time' => $timeTaken,
            'request' => json_decode($request->get('api_request', '[]'), true)
        ));
    }

});

//accepting JSON
$app->before(function (Request $request) use ($app) {
    if (0 === strpos($request->headers->get('Content-Type'), 'application/json')) {
        $content = $request->getContent();
        if (0 === strpos($request->headers->get('Content-Encoding'), 'gzip')) {
               $content = @gzinflate(substr($content, 10, -8));
        }
        // no idea what it was doing here? JSON was parsed in controller too
        //$data = json_decode($content, true);
        //$request->request->replace(is_array($data) ? $data : array());
        $request->request->set('api_request', $content);
    }
});

$app->register(new ServiceControllerServiceProvider());

$app->register(new MongoServiceProvider(), array(
    'mongo.connections' => array(
        'default' => array(
            'server' => $app['mongo_connection'],
            'options' => array("connect" => true)
        ),
        'analytics' => array(
            'server' => $app['mongo_analytics_connection'],
            'options' => array("connect" => false)
        )
    ),
));

$app->register(new FormServiceProvider());
$app->register(new Silex\Provider\TranslationServiceProvider(), array(
    'translator.messages' => array(),
));
if (class_exists('\Memcache')) {
    $app->register(new SilexMemcache\MemcacheExtension(), array(
            'memcache.library'    => 'memcache',
            'memcache.server' => array(
                array('127.0.0.1', 11211)
            )
    ));
}

$app->register(new Silex\Provider\TwigServiceProvider(), array(
    'twig.path' => __DIR__.'/views',
));
$app['twig'] = $app->share($app->extend('twig', function($twig, $app) {
    $twig->addFunction(new \Twig_SimpleFunction('relative', function ($string) use ($app) {
        if (preg_match("@https?:\/\/@i", $string)) {
            return $string;
        }
        return $app['request']->getBasePath() . '/' . $string;
    }));
    $twig->addFilter(new \Twig_SimpleFilter('id', function ($string) {
        return substr(md5($string), 0, 5);
    }));
    $twig->addFunction(new \Twig_SimpleFunction('in_array', function ($needle, $haystack) {
        return in_array($needle, $haystack);
    }));
    $twig->addFilter(new \Twig_SimpleFilter('fullurl', function ($string) {
        if (!empty($string)) {
            if(!preg_match("/^https?:\/\//", $string)) {
                $string = 'http://'.$string;
            }
        }
        return $string;
    }));
    return $twig;
}));


$app->register(new HttpCacheServiceProvider(), array("http_cache.cache_dir" => ROOT_PATH . "/storage/cache",));

$app->register(new MonologServiceProvider(), array(
    "monolog.logfile" => ROOT_PATH . "/storage/logs/" . date('Y-m-d') . ".log",
    "monolog.level" => $app["log.level"],
    "monolog.name" => "application"
));

//load services
$servicesLoader = new OpenData\ServicesLoader($app);
$servicesLoader->bindServicesIntoContainer();

//load routes
$routesLoader = new OpenData\RoutesLoader($app);
$routesLoader->bindRoutesToControllers();

/*
$app->error(function (\Exception $e, $code) use ($app) {
    $app['monolog']->addError($e->getMessage());
    $app['monolog']->addError($e->getTraceAsString());
    //if (0 === strpos($app['request']->headers->get('Content-Type'), 'application/json')) {
        return new JsonResponse(array("statusCode" => $code, "message" => $e->getMessage(), "stacktrace" => $e->getTraceAsString()));
    //}
});

 */

return $app;
