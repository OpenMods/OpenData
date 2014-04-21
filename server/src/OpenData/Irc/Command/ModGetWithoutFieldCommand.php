<?php

namespace OpenData\Irc\Command;

use Symfony\Component\Console\Input\InputArgument;
use Symfony\Component\Console\Input\InputInterface;
use Symfony\Component\Console\Output\OutputInterface;
use OpenData\Irc\IrcOutputStream;

class ModGetWithoutFieldCommand extends ModCommand
{
    private $app;
    private $field;
    
    public function __construct($app, $field) {
        $this->app = $app;
        $this->field = $field;
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
            );
        
    }

    protected function execute(InputInterface $input, OutputInterface $output) {
        
        $field = strtolower($input->getArgument('field'));
        
        if (!in_array($field, array('image', 'largeImage', 'description', 'url', 'donation', 'repository', 'credits'))) {
            return;
        }
        
        $mods = $this->app['mods.service']->findModsWithoutField(strtolower($field));
        
        $output->setTarget(IrcOutputStream::USER);
        foreach ($mods as $mod) {
            $output->write($mod['_id']);
        }
    }
}