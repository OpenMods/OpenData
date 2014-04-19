<?php

namespace OpenData\Irc\Command;

use Symfony\Component\Console\Command\Command;
use Symfony\Component\Console\Input\InputArgument;
use Symfony\Component\Console\Input\InputInterface;
use Symfony\Component\Console\Output\OutputInterface;
use OpenData\Irc\IrcOutputStream;

class FindModCommand extends Command {
    
    private $app;
    
    public function __construct($app) {
        $this->app = $app;
        parent::__construct();
    } 
    
    protected function configure() {
        $this
            ->setName('mod:find')
            ->setDescription('Find a mod by name')
            ->addArgument(
                'regex',
                InputArgument::REQUIRED,
                'A regular expression for the mod id or name'
            );
        
    }

    protected function execute(InputInterface $input, OutputInterface $output) {
        
        $regex = $input->getArgument('regex');
        
        $mods = $this->app['mods.service']->findByRegex($regex);
        
        $response = array();
        
        foreach ($mods as $mod) {
            $response[] = $mod['name'].' ('.$mod['_id'].')';
        }
        
        $output->write(count($response).' matches');
        
        $i = 0;
        $chunks = array_chunk($response, 10);
        foreach ($chunks as $chunk) {
            
            if (count($chunks) > 1 && $i == 1 && $output->getTarget() == IrcOutputStream::CHANNEL) {
                $output->write('Sending the rest via PM');
                $output->setTarget(IrcOutputStream::USER);
            }
            $output->write(implode(', ', $chunk));
           
            $i++;
        }
             
    }
}
