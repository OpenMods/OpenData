<?php

namespace OpenData\Controllers;

class ModController {

    private $serviceFiles;
    private $serviceMods;
    private $serviceCrashes;
    private $twig;

    public function __construct($twig, $files, $mods, $crashes) {
        $this->twig = $twig;
        $this->serviceFiles = $files;
        $this->serviceMods = $mods;
        $this->serviceCrashes = $crashes;
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
        
        return $this->twig->render('mod.twig', array(
            'versions' => $versions,
            'modInfo' => $modInfo,
            'downloads' => $this->serviceFiles->findDownloadsForSignatures($signatures)
        ));
    }

    public function fileinfo($fileId) {
        
        $file = $this->serviceFiles->findOne($fileId);
        
        if ($file == null) {
            throw new \Exception();
        }
        
        return $this->twig->render('file.twig', array(
            'file' => $file
        ));
    }
    
    public function analytics($modId, $fileId = null) {

        $document = $fileId == null ?
                    $this->serviceMods->findById($modId) :
                    $this->serviceFiles->findOne($fileId);
        
        if ($document == null) {
            throw new \Exception();
        }
        
        $hourly = array();
        
        if (isset($document['hours'])) {
            foreach ($document['hours'] as $hour) {
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
        
        return $this->twig->render('mod_analytics.twig', array(
            'hourly' => array(
                array('label' => '&nbsp;&nbsp;Past 24 hours', 'data' => $hourlyStats['today']),
                array('color' => '#cccccc', 'label' => '&nbsp;&nbsp;Previous 24 hours', 'data' => $hourlyStats['yesterday'])
            )
        ));
    }
    
    public function crashes($modId, $fileId = null) {
        $signatures = array();
        if ($fileId == null) {
            foreach($this->serviceFiles->findByModId($modId) as $file) {
                $signatures[] = $file['_id'];
            }
        } else {
            $signatures[] = $fileId;
        }
        
        $crashes = $this->serviceCrashes->findUniqueBySignatures($signatures);
        
        return $this->twig->render('mod_crashes.twig', array(
            'crashes' => $crashes
        ));
    }
}
