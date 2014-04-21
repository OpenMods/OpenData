<?php

namespace OpenData\Irc\Command;

use Symfony\Component\Console\Input\InputArgument;
use Symfony\Component\Console\Input\InputInterface;
use Symfony\Component\Console\Output\OutputInterface;

class ModListAdminsCommand extends ModCommand {
    
    private $app;
    
    public function __construct($app) {
        $this->app = $app;
        parent::__construct();
    }

    protected function configure() {
        $this
        ->setName('mod:admin:list')
        ->setDescription('List admins for a mod')
        ->addArgument(
            'modId',
            InputArgument::REQUIRED,
            'The mod id'
        );
    }
    
    protected function execute(InputInterface $input, OutputInterface $output) {
        
        $modId = $input->getArgument('modId');
        
        $mod = $this->app['mods.service']->findById(strtolower($modId));
                
        if ($mod != null) {
                        
            if (isset($mod['admins']) && count($mod['admins']) > 0) {
                
                $output->write('Admins for '.$mod['_id'].': '.implode(', ', $mod['admins']));
                
            } else {
                
                $output->write('No admins found for '.$mod['_id']);
            }
            
            
        } else {
            $output->write('Mod not found');
        }
    }
}