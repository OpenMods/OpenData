<?php

namespace OpenData\Irc\Command;

use Symfony\Component\Console\Input\InputArgument;
use Symfony\Component\Console\Input\InputInterface;
use Symfony\Component\Console\Output\OutputInterface;

class ModGetCommand extends ModCommand
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
            ->setName('mod:get:'.$this->field)
            ->setDescription('Get details of a mod')
            ->addArgument(
                'modId',
                InputArgument::REQUIRED,
                'The mod id used on the website'
            );
        
    }

    protected function execute(InputInterface $input, OutputInterface $output) {
        
        $modId = $input->getArgument('modId');
        
        $mod = $this->app['mods.service']->findById(strtolower($modId));
        
        if ($mod != null) {
            if (isset($mod[$this->field])) {
                $data = $mod[$this->field];
                if (is_array($data)) {
                    $data = implode(', ', $data);
                }
                $output->write($modId.'.'.$this->field .': '. $data);
            } else {
                $output->write('Field not found');
            }
        } else {
            $output->write('Mod not found');
        }
    }
}