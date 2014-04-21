<?php

namespace OpenData\Irc\Command\File;

use Symfony\Component\Console\Input\InputArgument;
use Symfony\Component\Console\Input\InputInterface;
use Symfony\Component\Console\Output\OutputInterface;
use OpenData\Irc\Command\ModCommand;

class FileListNotesCommand extends ModCommand {
    
    private $app;
    
    public function __construct($app) {
        $this->app = $app;
        parent::__construct();
    }
    

    protected function configure() {
        $this
            ->setName('file:note:list')
            ->setDescription('List the notes against a file')
            ->addArgument(
                'fileId',
                InputArgument::REQUIRED,
                'The file signature'
            );
    }

    protected function execute(InputInterface $input, OutputInterface $output) {
        
        $fileId = $input->getArgument('fileId');
        
        $file = $this->app['files.service']->findOne(strtolower($fileId));
              
        if ($file != null) {
            
            
            
            if (isset($file['notes']) && count($file['notes']) > 0) {
                $count = count($file['notes']);
                $output->write($count.' note'.($count > 1 ? 's' : '').' found'); 
                $index = 1;
                foreach ($file['notes'] as $note) {
                    $msg = '['.$index.'] ';
                    $msg .= 'Level: '.$note['level'].' ';
                    $msg .= 'Description: '.$note['description'].' ';
                    if (isset($note['payload'])) {
                        $msg .= 'Payload: '.$note['payload'].' ';
                    }
                    $output->write($msg);
                    $index++;
                }
            } else {
                $output->write('No notes found'); 
            }            
        } else {
            $output->write('File not found'); 
        }
    }
    
}
