<?php

namespace OpenData\Irc\Command;

use Symfony\Component\Console\Input\InputArgument;
use Symfony\Component\Console\Input\InputInterface;
use Symfony\Component\Console\Output\OutputInterface;

class ModAddAdminCommand extends ModCommand {
    
    private $app;
    
    public function __construct($app) {
        $this->app = $app;
        parent::__construct();
    }

    protected function configure() {
        $this
        ->setName('mod:admin:add')
        ->setDescription('Add a user to mod permissions')
        ->addArgument(
            'modId',
            InputArgument::REQUIRED,
            'The mod id'
        )->addArgument(
            'username',
            InputArgument::REQUIRED,
            'The IRC name'
        );
    }
    
    protected function execute(InputInterface $input, OutputInterface $output) {

        if ($this->isUserAdmin($output)) {
            $output->write('Insufficient permissions'); 
            return;
        }
        
        $modId = $input->getArgument('modId');
        
        $mod = $this->app['mods.service']->findById(strtolower($modId));
        
        $username = $input->getArgument('username');
        
        if ($mod != null) {
            
            $admins = array();
            if ($mod['admins'] != null) {
                $admins = $mod['admins'];
            }
            
            if (in_array($username, $admins)) {
                $output->write('User is already an admin for this mod');
                return;
            }
            
            $admins[] = $username;
            
            $this->app['mods.service']->updateMod(
                $mod['_id'],
                array('admins' => $admins)
            );
            
            $output->write('Added user to permissions list.');
            
        } else {
            $output->write('Mod not found');
        }
    }
}