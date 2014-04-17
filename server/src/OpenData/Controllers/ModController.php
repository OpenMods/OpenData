<?php

namespace OpenData\Controllers;

use Symfony\Component\HttpFoundation\JsonResponse;

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
        
        $info = null;
        if ($fileId == null) {
            $info = $this->serviceMods->findById($modId);
        } else {
            $info = $this->serviceFiles->findOne($fileId);
        }
        
        $hourly = array();
        if (isset($info['hours'])) {
            foreach ($info['hours'] as $hour) {
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
        
        return new JsonResponse(array(
            'hourly' => array(
                array('label' => '&nbsp;&nbsp;Past 24 hours', 'data' => $hourlyStats['today']),
                array('color' => '#cccccc', 'label' => '&nbsp;&nbsp;Previous 24 hours', 'data' => $hourlyStats['yesterday'])
            )
        ));
        
    }
    
    public function crashes($modId) {
        
        return $this->twig->render('mod_crashes.twig', array(
        ));
    }
}
