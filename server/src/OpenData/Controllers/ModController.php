<?php

namespace OpenData\Controllers;

class ModController {

    private $serviceFiles;
    private $serviceMods;
    private $serviceAnalytics;
    private $twig;

    public function __construct($twig, $files, $mods, $analytics) {
        $this->twig = $twig;
        $this->serviceFiles = $files;
        $this->serviceMods = $mods;
        $this->serviceAnalytics = $analytics;
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
        $signatures = array();

        foreach ($files as $file) {
            $signatures[] = $file['_id'];
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
        
        $hourlyStats = $this->serviceAnalytics->hourlyForFiles($signatures);
        
        $tmp = array();
        foreach ($hourlyStats as $stat) {
            $tmp[$stat['time']->sec * 1000] += $stat['launches'];
        }
        $hourlyFormatted = array();
        foreach ($tmp as $k => $v) {
            $hourlyFormatted[] = array($k, $v);
        }
        
        return $this->twig->render('mod.twig', array(
            'versions' => $versions,
            'modInfo' => $modInfo,
            'hourly' => $hourlyFormatted
        ));
    }

}
