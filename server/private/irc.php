<?php

use Symfony\Component\Console\Application;
use OpenData\Irc\Command;

require_once __DIR__ . '/../vendor/autoload.php';

$app = new Silex\Application();

require __DIR__ . '/../resources/config/prod.php';

require __DIR__ . '/../src/app.php';


$console = new Application();

$bot = new Application();

$bot->addCommands(array(
    new Command\FindModCommand($app),
    new Command\AdminsListCommand($app)
));

$bot->add(new Command\ModGetCommand($app, 'name'));
$bot->add(new Command\ModGetCommand($app, 'description'));
$bot->add(new Command\ModGetCommand($app, 'authors'));
$bot->add(new Command\ModGetCommand($app, 'parent'));
$bot->add(new Command\ModGetCommand($app, 'url'));
$bot->add(new Command\ModGetCommand($app, 'releasesPage'));
$bot->add(new Command\ModGetCommand($app, 'credits'));
$bot->add(new Command\ModGetCommand($app, 'tags'));
$bot->add(new Command\ModGetCommand($app, 'jsonUrl'));
$bot->add(new Command\ModGetCommand($app, 'repository'));
$bot->add(new Command\ModGetCommand($app, 'irc'));
$bot->add(new Command\ModGetCommand($app, 'donation'));

$bot->add(new Command\ModUpdateCommand($app, 'name'));
$bot->add(new Command\ModUpdateCommand($app, 'description'));
$bot->add(new Command\ModUpdateCommand($app, 'authors', true));
$bot->add(new Command\ModUpdateCommand($app, 'parent'));
$bot->add(new Command\ModUpdateCommand($app, 'url'));
$bot->add(new Command\ModUpdateCommand($app, 'repository'));
$bot->add(new Command\ModUpdateCommand($app, 'releasesPage'));
$bot->add(new Command\ModUpdateCommand($app, 'credits'));
$bot->add(new Command\ModUpdateCommand($app, 'tags', true));
$bot->add(new Command\ModUpdateCommand($app, 'jsonUrl'));
$bot->add(new Command\ModUpdateCommand($app, 'donation'));
$bot->add(new Command\ModUpdateIrcCommand($app));

$bot->add(new Command\ModGetWithoutFieldCommand($app));

$bot->add(new Command\ModListAdminsCommand($app));
$bot->add(new Command\ModRemoveAdminCommand($app));
$bot->add(new Command\ModAddAdminCommand($app));

$bot->add(new Command\ModUpdateImageCommand($app, 'image', IMAGETYPE_PNG, 64, 64, 'modimages'));
$bot->add(new Command\ModUpdateImageCommand($app, 'largeImage', IMAGETYPE_JPEG, 1920, 500, 'largemodimages'));

$console->add(new OpenData\Irc\RunConsoleCommand($bot));
$console->run();
