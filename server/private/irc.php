<?php

use Symfony\Component\Console\Application;
use OpenData\Irc\Command;

require_once __DIR__ . '/../vendor/autoload.php';

$app = new Silex\Application();

require __DIR__ . '/../resources/config/prod.php';

require __DIR__ . '/../src/app.php';


$console = new Application();

$bot = new Application();

$bot->add(new Command\Admin\AdminsListCommand($app));

$bot->add(new Command\Mod\FindModCommand($app));
$bot->add(new Command\Mod\ModGetCommand($app, 'name'));
$bot->add(new Command\Mod\ModGetCommand($app, 'description'));
$bot->add(new Command\Mod\ModGetCommand($app, 'authors'));
$bot->add(new Command\Mod\ModGetCommand($app, 'parent'));
$bot->add(new Command\Mod\ModGetCommand($app, 'url'));
$bot->add(new Command\Mod\ModGetCommand($app, 'releasesPage'));
$bot->add(new Command\Mod\ModGetCommand($app, 'credits'));
$bot->add(new Command\Mod\ModGetCommand($app, 'tags'));
$bot->add(new Command\Mod\ModGetCommand($app, 'jsonUrl'));
$bot->add(new Command\Mod\ModGetCommand($app, 'repository'));
$bot->add(new Command\Mod\ModGetCommand($app, 'irc'));
$bot->add(new Command\Mod\ModGetCommand($app, 'donation'));
$bot->add(new Command\Mod\ModUpdateCommand($app, 'name'));
$bot->add(new Command\Mod\ModUpdateCommand($app, 'description'));
$bot->add(new Command\Mod\ModUpdateCommand($app, 'authors', true));
$bot->add(new Command\Mod\ModUpdateCommand($app, 'parent'));
$bot->add(new Command\Mod\ModUpdateCommand($app, 'url'));
$bot->add(new Command\Mod\ModUpdateCommand($app, 'repository'));
$bot->add(new Command\Mod\ModUpdateCommand($app, 'releasesPage'));
$bot->add(new Command\Mod\ModUpdateCommand($app, 'credits'));
$bot->add(new Command\Mod\ModUpdateCommand($app, 'tags', true));
$bot->add(new Command\Mod\ModUpdateCommand($app, 'jsonUrl'));
$bot->add(new Command\Mod\ModUpdateCommand($app, 'donation'));
$bot->add(new Command\Mod\ModUpdateIrcCommand($app));
$bot->add(new Command\Mod\ModGetWithoutFieldCommand($app));
$bot->add(new Command\Mod\ModListAdminsCommand($app));
$bot->add(new Command\Mod\ModRemoveAdminCommand($app));
$bot->add(new Command\Mod\ModAddAdminCommand($app));
$bot->add(new Command\Mod\ModUpdateImageCommand($app, 'image', IMAGETYPE_PNG, 64, 64, 'modimages'));
$bot->add(new Command\Mod\ModUpdateImageCommand($app, 'largeImage', IMAGETYPE_JPEG, 1920, 500, 'largemodimages'));

$bot->add(new Command\File\FileAddNoteCommand($app));
$bot->add(new Command\File\FileListNotesCommand($app));
$bot->add(new Command\File\FileRemoveNoteCommand($app));


$console->add(new OpenData\Irc\RunConsoleCommand($bot));
$console->run();
