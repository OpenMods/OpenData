<?php

namespace OpenData\Irc\Command\Mod;

use Symfony\Component\Console\Input\InputArgument;
use Symfony\Component\Console\Input\InputInterface;
use Symfony\Component\Console\Output\OutputInterface;
use OpenData\Irc\Command\ModCommand;

class ModGetWithoutFieldCommand extends ModCommand
{
    private $app;
    
    public function __construct($app) {
        $this->app = $app;
        parent::__construct();
    }
    
    protected function configure() {
        $this
            ->setName('mod:notset')
            ->setDescription('Get details of a mod')
            ->addArgument(
                'field',
                InputArgument::REQUIRED,
                'The field you want'
            )->addArgument(
                'skip',
                InputArgument::OPTIONAL,
                'The how many to skip',
                0
            );
        
    }

    protected function execute(InputInterface $input, OutputInterface $output) {
        
        $field = strtolower($input->getArgument('field'));
        
        if (!in_array($field, array('image', 'largeImage', 'description', 'url', 'donation', 'repository', 'credits'))) {
            return;
        }
        
        $mods = $this->app['mods.service']->findModsWithoutField(strtolower($field), $input->getArgument('skip'));
        
        $modIds = array();
        foreach ($mods as $mod) {
            $modIds[] = $mod['_id'];
        }
        $output->write(implode(', ', $modIds));
        
    }
}