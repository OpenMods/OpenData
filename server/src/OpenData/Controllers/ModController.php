<?php

namespace OpenData\Controllers;

class ModController {

    private $serviceFiles;
    private $serviceMods;
    private $twig;

    public function __construct($twig, $files, $mods) {
        $this->twig = $twig;
        $this->serviceFiles = $files;
        $this->serviceMods = $mods;
    }

    public function modinfo($modId) {

        $modInfo = $this->serviceMods->findById($modId);
        
        if ($modInfo == null) {
            throw new \Exception();
        }
        
        $files = $this->serviceFiles->findByModId($modId);

        $numFiles = $files->count();
        
        if ($numFiles == 0) {
            throw new \Exception();
        }
        
        $versions = array();

        foreach ($files as $file) {
            foreach ($file['mods'] as $mod) {
                if ($mod['modId'] == $modId) {
                    $version = $mod['version'];
                    if (!isset($versions[$version])) {
                        $versions[$version] = array();
                    }
                    $versions[$version][] = $file;
                }
            }
        }

        return $this->twig->render('mod.twig', array(
            'versions' => $versions,
            'modInfo' => $modInfo
        ));
    }

}
