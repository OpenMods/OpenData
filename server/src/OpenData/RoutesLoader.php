<?php

namespace OpenData;

use Silex\Application;

class RoutesLoader {

    public $app;

    public function __construct(Application $app) {

        $this->app = $app;
        $this->instantiateControllers();
    }

    private function instantiateControllers() {

        $loader = $this;
        $this->app['api.controller'] = $this->app->share(function () use ($loader) {
            
            $apiController = new Controllers\ApiController(
                class_exists('\Memcache') ? $loader->app['memcache'] : null
            );
            
            $apiController->registerPacketHandler($loader->app['handler.analytics']);
            $apiController->registerPacketHandler($loader->app['handler.ping']);
            $apiController->registerPacketHandler($loader->app['handler.fileinfo']);
            $apiController->registerPacketHandler($loader->app['handler.crashlog']);
            $apiController->registerPacketHandler($loader->app['handler.filelist']);
            
            return $apiController;
        });

        $this->app['site.controller'] = $this->app->share(function () use ($loader) {
            return new Controllers\SiteController(
                $loader->app['twig'],
                $loader->app['request'],
                $loader->app['files.service'],
                $loader->app['mods.service']
            );
        });
    }

    public function bindRoutesToControllers() {

        $api = $this->app["controllers_factory"];
        $api->post('/data', "api.controller:main");
        
        $site = $this->app["controllers_factory"];
        $site->get('/', "site.controller:home");
        $site->get('/mod/{modId}', "site.controller:modinfo");

        $this->app->mount($this->app["api.endpoint"] . '/' . $this->app["api.version"], $api);
        $this->app->mount('/', $site);
    }

}
