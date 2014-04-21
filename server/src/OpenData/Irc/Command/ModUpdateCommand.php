<?php

namespace OpenData\Irc\Command;

use Symfony\Component\Console\Command\Command;
use Symfony\Component\Console\Input\InputArgument;
use Symfony\Component\Console\Input\InputInterface;
use Symfony\Component\Console\Output\OutputInterface;

class ModUpdateCommand extends ModCommand {
    
    private $field;
    private $app;
    private $requirement;
    
    public function __construct($app, $field, $isArray = false) {
        $this->app = $app;
        $this->field = $field;
        $this->requirement = InputArgument::REQUIRED;
        if ($isArray) {
            $this->requirement |= InputArgument::IS_ARRAY;
        }
        parent::__construct();
    }
    

    protected function configure() {
        $this
            ->setName('mod:update:'.$this->field)
            ->setDescription('Update the '.$this->field.' field')
            ->addArgument(
                'modId',
                InputArgument::REQUIRED,
                'The mod id used on the website'
            )->addArgument(
                'value',
                $this->requirement,
                'Value of you wish to set'
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
            
            $value = $input->getArgument('value');
            $this->app['mods.service']->updateMod($modId, array(
                $this->field => $value
            ));
            $output->write('Update successful'); 
        } else {
            $output->write('Mod not found'); 
        }
    }
    
}
