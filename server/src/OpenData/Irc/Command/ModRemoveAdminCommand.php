<?php

namespace OpenData\Irc\Command;

use Symfony\Component\Console\Input\InputArgument;
use Symfony\Component\Console\Input\InputInterface;
use Symfony\Component\Console\Output\OutputInterface;

class ModRemoveAdminCommand extends ModCommand {
    
    private $app;
    
    public function __construct($app) {
        $this->app = $app;
        parent::__construct();
    }

    protected function configure() {
        $this
        ->setName('mod:admin:remove')
        ->setDescription('Remove a users mod permissions')
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
            
            if(($key = array_search($username, $admins)) !== false) {
                unset($admins[$key]);
            } else {
                $output->write('Cannot remove user from mod permissions');
                return;
            }
            
            $this->app['mods.service']->updateMod(
                $mod['_id'],
                array('admins' => $admins)
            );
            
            $output->write('Removed user from mod permissions');
            
            
        } else {
            $output->write('Mod not found');
        }
    }
}