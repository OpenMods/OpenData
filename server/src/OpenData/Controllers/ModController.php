<?php

namespace OpenData\Controllers;

class ModController {

    private $serviceFiles;
    private $serviceMods;
    private $serviceAnalytics;
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
        
        $hourly = array();
        if (isset($modInfo['hours'])) {
            foreach ($modInfo['hours'] as $hour) {
                $hourly[$hour['time']->sec * 1000] = $hour['launches'];
            }
        }
 
        $lastHour = strtotime(date("Y-m-d H:00:00"));
        
        $hourlyStats = array(
            'today' => array(),
            'yesterday' => array()
        );
        
        foreach ($hourlyStats as $day => $v) {
            for ($i = 0; $i < 24; $i++) {
                $displayTime = ($lastHour - ($i * 3600)) * 1000;
                $statTime = $displayTime;
                if ($day == 'yesterday') {
                    $statTime -= 86400000;
                }
                $hourlyStats[$day][] = array(
                    $displayTime,
                    isset($hourly[$statTime]) ? $hourly[$statTime] : 0
                ); 
            }
        }
        
        return $this->twig->render('mod.twig', array(
            'versions' => $versions,
            'modInfo' => $modInfo,
            'hourly' => array(
                array('label' => '&nbsp;&nbsp;Past 24 hours', 'data' => $hourlyStats['today']),
                array('color' => '#cccccc', 'label' => '&nbsp;&nbsp;Previous 24 hours', 'data' => $hourlyStats['yesterday'])
            ),
            'downloads' => $this->serviceFiles->findDownloadsForSignatures($signatures)
        ));
    }

}
