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
        $this->app['crashes.controller'] = $this->app->share(function () use ($loader) {
            return new Controllers\CrashesController($loader->app['crashes.service']);
        });
    }

    public function bindRoutesToControllers() {

        $api = $this->app["controllers_factory"];

        $api->get('/crashes', "crashes.controller:listAll");
        $api->post('/crashes', "crashes.controller:save");

        $this->app->mount($this->app["api.endpoint"] . '/' . $this->app["api.version"], $api);
    }

}
