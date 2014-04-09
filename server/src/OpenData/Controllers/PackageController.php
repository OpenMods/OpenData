<?php

namespace OpenData\Controllers;

class PackageController {
    
    private $twig;
    private $serviceFiles;
    private $serviceMods;
    
    public function __construct($twig, $files, $mods) {
        $this->twig = $twig;
        $this->serviceFiles = $files;
        $this->serviceMods = $mods;
    }
    
    public function package($package) {
        
        $files = $this->serviceFiles->findByPackage($package);
        
        if ($files->count() == 0) {
            throw new \Exception('Unknown package');
        }
        
        $modIds = $this->serviceFiles->findUniqueModIdsForPackage($package);
        
        $mods = $this->serviceMods->findByIds($modIds);
        
        return $this->twig->render('package.twig', array(
            'packageName' => $package,
            'files' => $files,
            'mods' => $mods
        ));
    }
    
}
