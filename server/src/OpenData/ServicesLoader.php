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
        $this->app['files.service'] = $this->app->share(function () use ($loader) {
            return new Services\FilesService($loader->app["mongo"]);
        });
        $this->app['analytics.service'] = $this->app->share(function () use ($loader) {
            return new Services\AnalyticsService($loader->app["mongo"]);
        });
        $this->app['mods.service'] = $this->app->share(function () use ($loader) {
            return new Services\ModsService($loader->app["mongo"]);
        });
        
        $this->app['handler.analytics'] = $this->app->share(function () use ($loader) {
            return new PacketHandlers\Analytics(
                $loader->app["analytics.service"],
                $loader->app["files.service"]
            );
        });
        
        $this->app['handler.ping'] = $this->app->share(function () {
            return new PacketHandlers\Ping();
        });
        
        $this->app['handler.fileinfo'] = $this->app->share(function () use ($loader) {
            return new PacketHandlers\FileInfo(
                $loader->app["files.service"],
                $loader->app["mods.service"]
            );
        });
        
        $this->app['handler.crashlog'] = $this->app->share(function () use ($loader) {
            return new PacketHandlers\CrashLog(
                $loader->app["crashes.service"]
            );
        });
        
        $this->app['handler.filelist'] = $this->app->share(function () use ($loader) {
            return new PacketHandlers\FileList(
                $loader->app["files.service"]
            );
        });
    }

}
