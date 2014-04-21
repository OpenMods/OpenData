<?php

namespace OpenData\Irc\Command;

use Symfony\Component\Console\Command\Command;
use Symfony\Component\Console\Input\InputArgument;
use Symfony\Component\Console\Input\InputInterface;
use Symfony\Component\Console\Output\OutputInterface;

class ModUpdateIrcCommand extends ModCommand {
    
    private $app;
    
    public function __construct($app) {
        $this->app = $app;
        parent::__construct();
    }

    protected function configure() {
        $this
            ->setName('mod:update:irc')
            ->setDescription('Update the irc field')
            ->addArgument(
                'modId',
                InputArgument::REQUIRED,
                'The mod id used on the website'
            )->addArgument(
                'host',
                InputArgument::REQUIRED,
                'The IRC host'
            )->addArgument(
                'channel',
                InputArgument::REQUIRED,
                'The IRC channel'
            );
    }

    protected function execute(InputInterface $input, OutputInterface $output) {
        
        if (!$this->userHasSomePermissions($output)) {
            $output->write('Insufficient permissions'); 
            return;
        }
        
        $modId = $input->getArgument('modId');
        
        $mod = $this->app['mods.service']->findById(strtolower($modId));
        
        if ($mod != null) {
            
            if (!$this->userHasModPermissions($output, $mod)) { 
                $output->write('Insufficient permissions'); 
                return;
            }
            
            $host = $input->getArgument('host');
            $channel = $input->getArgument('channel');
            $this->app['mods.service']->updateMod($modId, array(
                'irc' => array(
                    'host' => $host,
                    'channel' => $channel
                )
            ));
            $output->write('Update successful'); 
        } else {
            $output->write('Mod not found'); 
        }
    }
    
}
