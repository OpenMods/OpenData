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
        
        $files = $this->serviceFiles->findByPackage($package)->sort(array('filename' => 1));
        
        if ($files->count() == 0) {
            throw new \Exception('Unknown package');
        }
        
        $modIds = $this->serviceFiles->findUniqueModIdsForPackage($package);
                
        $modList = array();
        
        foreach ($this->serviceMods->findByIds($modIds) as $mod) {
            $mod['files'] = array();
            $modList[$mod['_id']] = $mod;
        }
        
        foreach ($files as $file) {
            foreach ($file['mods'] as $mod) {
                if (isset($modList[$mod['modId']])) {
                    $modList[$mod['modId']]['files'][] = $file;
                }
            }
        }
        
        return $this->twig->render('package.twig', array(
            'packageName' => $package,
            'mods' => $modList,
            'subpackages' => $this->serviceFiles->findSubPackages($package)
        ));
    }
    
}
