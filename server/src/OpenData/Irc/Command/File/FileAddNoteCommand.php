<?php

namespace OpenData\Irc\Command\File;

use Symfony\Component\Console\Input\InputArgument;
use Symfony\Component\Console\Input\InputInterface;
use Symfony\Component\Console\Output\OutputInterface;
use OpenData\Irc\Command\ModCommand;

class FileAddNoteCommand extends ModCommand {
    
    private $app;
    
    public function __construct($app) {
        $this->app = $app;
        parent::__construct();
    }
    

    protected function configure() {
        $this
            ->setName('file:note:add')
            ->setDescription('Add a note to a file')
            ->addArgument(
                'fileId',
                InputArgument::REQUIRED,
                'The file signature'
            )->addArgument(
                'level',
                InputArgument::REQUIRED,
                'The priority level 1-10 (low - high)'
            )->addArgument(
                'description',
                InputArgument::REQUIRED,
                'User readable description of the update'
            )->addArgument(
                'payload',
                InputArgument::OPTIONAL,
                'Unreadable payload if you want to send some data to your mod.'
            );
    }

    protected function execute(InputInterface $input, OutputInterface $output) {
        
        if (!$this->userHasSomePermissions($output)) {
            $output->write('Insufficient permissions'); 
            return;
        }
        
        $fileId = $input->getArgument('fileId');
        
        $file = $this->app['files.service']->findOne(strtolower($fileId));
              
        if ($file != null) {
            
            $hasPerms = false;
            $modIds = array();
            foreach ($file['mods'] as $mod) {
                $modIds[] = $mod['modId'];
            }
            
            if (count($modIds) == 0) {
                $output->write('Insufficient permissions'); 
                return;
            }
            
            foreach ($this->app['mods.service']->findByIds($modIds) as $mod) {
                if ($this->userHasModPermissions($output, $mod)) { 
                    $hasPerms = true;
                }
            }
            if (!$hasPerms) {
                $output->write('Insufficient permissions'); 
                return;
            }
            
            $level = $input->getArgument('level');
            $description = $input->getArgument('description');
            $payload = $input->getArgument('payload');
            
            if (!is_numeric($level)) {
                $output->write('level should be a number'); 
                return;
            }
            $level = (int) $level;
            if ($level < 1 || $level > 10) {
                $output->write('level should be a number from 1 to 10'); 
                return;
            }
            
            $notes = array();
            if (isset($file['notes'])) {
                $notes = $file['notes'];
            }
            
            $newNote = array(
                'level' => $level,
                'description' => $description
            );
            if ($payload != null) {
                $newNote['payload'] = $payload;
            }
            
            $notes[] = $newNote;
            
            $this->app['files.service']->update($fileId, array(
                'notes' => $notes
            ));
            
            $output->write('Update successful'); 
        } else {
            $output->write('File not found'); 
        }
    }
    
}
