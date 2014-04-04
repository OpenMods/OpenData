<?php

namespace OpenData;

use Silex\Application;

class ServicesLoader {

    public $app;

    public function __construct(Application $app) {

        $this->app = $app;
    }

    public function bindServicesIntoContainer() {

        $loader = $this;
        $this->app['crashes.service'] = $this->app->share(function () use ($loader) {
            return new Services\CrashesService($loader->app["mongo"]);
        });
    }

}
