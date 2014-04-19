<?php

namespace OpenData\Irc\Command;

use Symfony\Component\Console\Command\Command;
use Symfony\Component\Console\Input\InputArgument;
use Symfony\Component\Console\Input\InputInterface;
use Symfony\Component\Console\Output\OutputInterface;

class GetModDataCommand extends Command
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
            ->setName('mod:get')
            ->setDescription('Get details of a mod')
            ->addArgument(
                'modId',
                InputArgument::REQUIRED,
                'The mod id used on the website'
            )->addArgument(
                'fields',
                InputArgument::IS_ARRAY | InputArgument::REQUIRED,
                'What fields would you like information about? Valid options are: '.implode(', ', self::$validOptions)
            );
        
    }

    protected function execute(InputInterface $input, OutputInterface $output) {
        
        $modId = $input->getArgument('modId');
        
        $mod = $this->app['mods.service']->findById(strtolower($modId));
        
        if ($mod != null) {
            foreach ($input->getArgument('fields') as $field) {
                if (isset($mod[$field]) && in_array($field, self::$validOptions)) {
                    
                    $info = $mod[$field];
                    
                    if (is_array($info)) {
                        $info = implode(', ', $info);
                    }
                    
                    $output->write($field .': '. $info);
                }
            }
        }
    }
}