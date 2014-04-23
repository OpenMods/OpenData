<?php

namespace OpenData\Controllers;

class CrashesController {

    private $twig;
    
    private $serviceMods;
    private $serviceFiles;
    private $serviceCrashes;
    
    public function __construct($twig, $mods, $files, $crashes) {
        
        $this->twig = $twig;
        $this->serviceMods = $mods;
        $this->serviceFiles = $files;
        $this->serviceCrashes = $crashes;
        
    }
    
    public function search() {
        return $this->twig->render('crashes.twig', array(
            'crashes' => $this->serviceCrashes->findLatestUnique()
        ));
    }
    
}
