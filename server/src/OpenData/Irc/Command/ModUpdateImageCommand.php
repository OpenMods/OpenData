<?php

namespace OpenData\Irc\Command;

use Symfony\Component\Console\Command\Command;
use Symfony\Component\Console\Input\InputArgument;
use Symfony\Component\Console\Input\InputInterface;
use Symfony\Component\Console\Output\OutputInterface;

class ModUpdateImageCommand extends ModCommand {
    
    private $field;
    private $app;
    private $imageType;
    private $width;
    private $height;
    private $folder;
    
    public function __construct($app, $field, $imageType, $width, $height, $folder) {
        $this->app = $app;
        $this->field = $field;
        $this->imageType = $imageType;
        $this->width = $width;
        $this->height = $height;
        $this->folder = $folder;
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
                InputArgument::REQUIRED,
                'The url of the image'
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
            
            $content = @file_get_contents($value);
            
            if (empty($content)) {
                $output->write('Unable to find image'); 
                return;
            }
            
            file_put_contents('/tmp/image', $content);
            list($width, $height, $type, $attr) = getimagesize('/tmp/image');
            
            if ($type != $this->imageType) {
                $output->write('Unexpected image format. Small images should be png, large banners should be jpg ('.$type.')'); 
                return;
            }
            if ($width != $this->width || $height != $this->height) {
                $output->write('Invalid image size. Expected '.$this->width.'x'.$this->height); 
                return;
            }
            
            $extension = $this->imageType == IMAGETYPE_PNG ? '.png' : '.jpg';
                        
            file_put_contents(ROOT_PATH.'/web/'.$this->folder.'/'.$mod['_id'].$extension, $content);
            
            $this->app['mods.service']->updateMod($modId, array(
                $this->field => $this->folder.'/'.$mod['_id'].$extension
            ));
            
            $output->write('Update successful'); 
        } else {
            $output->write('Mod not found'); 
        }
    }
    
}
