<?php

namespace OpenData\Irc\Command\File;

use Symfony\Component\Console\Input\InputArgument;
use Symfony\Component\Console\Input\InputInterface;
use Symfony\Component\Console\Output\OutputInterface;
use OpenData\Irc\Command\ModCommand;

class FileRemoveNoteCommand extends ModCommand {
    
    private $app;
    
    public function __construct($app) {
        $this->app = $app;
        parent::__construct();
    }
    

    protected function configure() {
        $this
            ->setName('file:note:remove')
            ->setDescription('Add remove note from a file')
            ->addArgument(
                'fileId',
                InputArgument::REQUIRED,
                'The file signature'
            )->addArgument(
                'index',
                InputArgument::REQUIRED,
                'The 1-based index of the note'
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
            
            $index = $input->getArgument('index');
            
            if (!is_numeric($index)) {
                $output->write('index should be a number'); 
                return;
            }
            $index = ((int) $index) - 1;
            
            $notes = array();
            if (isset($file['notes'])) {
                $notes = $file['notes'];
            }
            
            if (!isset($notes[$index])) {
                $output->write('Note index not found.'); 
                return;
            }
            
            unset($notes[$index]);
            
            $this->app['files.service']->update($fileId, array(
                'notes' => $notes
            ));
            
            $output->write('Note removed'); 
        } else {
            $output->write('File not found'); 
        }
    }
    
}
