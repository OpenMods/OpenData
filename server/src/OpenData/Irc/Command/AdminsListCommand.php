<?php

namespace OpenData\Irc\Command;

use Symfony\Component\Console\Command\Command;
use Symfony\Component\Console\Input\InputInterface;
use Symfony\Component\Console\Output\OutputInterface;


class AdminsListCommand extends Command {
    
    private $app;
    
    public function __construct($app) {
        $this->app = $app;
        parent::__construct();
    } 
    
    protected function configure() {
        $this
            ->setName('admins:list')
            ->setDescription('List which admins are online');
        
    }

    protected function execute(InputInterface $input, OutputInterface $output) {
        
        $output->write('Online admins: '.implode(', ', $output->getBot()->getAdmins()));

    }
}
