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
            return new Controllers\ApiController(
                    $loader->app['crashes.service'],
                    $loader->app['analytics.service'],
                    $loader->app['mods.service'],
                    class_exists('\Memcache') ? $loader->app['memcache'] : null
            );
        });
    }

    public function bindRoutesToControllers() {

        $api = $this->app["controllers_factory"];

        $api->post('/data', "api.controller:main");

        $this->app->mount($this->app["api.endpoint"] . '/' . $this->app["api.version"], $api);
    }

}
