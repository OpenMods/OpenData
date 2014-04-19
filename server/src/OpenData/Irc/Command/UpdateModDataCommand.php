<?php

namespace OpenData\Irc\Command;

use Symfony\Component\Console\Command\Command;
use Symfony\Component\Console\Input\InputArgument;
use Symfony\Component\Console\Input\InputInterface;
use Symfony\Component\Console\Output\OutputInterface;

class UpdateModDataCommand extends Command
{
    private $app;
    
    public static $validOptions = array(
      'name',
      'credits',
      'description',
      'url',
      'releasesPage',
      'parent',
      'tags',
      'jsonUrl',
      'image',
      'largeImage',
      'authors'
    );
    
    public function __construct($app) {
        $this->app = $app;
        parent::__construct();
    }
    
    protected function configure() {
        $this
            ->setName('mod:update')
            ->setDescription('Get details of a mod')
            ->addArgument(
                'modId',
                InputArgument::REQUIRED,
                'The mod id used on the website'
            )->addArgument(
                'field',
                InputArgument::REQUIRED,
                'What fields would you like to edit? Valid options are: '.implode(', ', self::$validOptions)
            )->addArgument(
                'value',
                InputArgument::REQUIRED | InputArgument::IS_ARRAY,
                'Value of you wish to set'
            );
        
    }

    protected function execute(InputInterface $input, OutputInterface $output) {
        
        $admins = $output->getBot()->getAdmins();
        
        if (!in_array($output->getUser(), $admins)) {
             $output->write($output->getUser().' has insufficient permissions');
             return;
        }
        
        $modId = $input->getArgument('modId');
        
        $mod = $this->app['mods.service']->findById(strtolower($modId));
        
        if ($mod != null) {
            
            $field = $input->getArgument('field');
            if (in_array($field, self::$validOptions)) {
                
                $value = $input->getArgument('value');
                
                
                if ($field != 'authors' && $field == 'tags') {
                    $value = implode(' ', $value);
                } else {
                    if ($field == 'tags') {
                        for ($i = 0; $i < count($value); $i++) {
                            $value[$i] = strtolower($value[$i]);
                        }
                    }
                }
                
                $this->app['mods.service']->updateMod($modId, array(
                    $field => $value
                ));
                $output->write('Update successful'); 
           }
        }
    }
}