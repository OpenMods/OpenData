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
    new Command\GetModDataCommand($app),
    new Command\FindModCommand($app),
    new Command\AdminsListCommand($app),
    new Command\UpdateModDataCommand($app)
));

$console->add(new OpenData\Irc\RunConsoleCommand($bot));
$console->run();
