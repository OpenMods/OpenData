<?php

namespace OpenData\Controllers;

class SiteController {

    private $twig;
    private $serviceFiles;
    private $serviceMods;

    public function __construct($twig, $request, $files, $mods) {
        $this->twig = $twig;
        $this->twig->addFunction(new \Twig_SimpleFunction('relative', function ($string) use ($request) {
            return $request->getBasePath() . '/' . $string;
        }));
        $this->twig->addFilter(new \Twig_SimpleFilter('id', function ($string) {
            return substr(md5($string), 0, 5);
        }));

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

    public function home() {
        
        return $this->twig->render('home.twig', array(
            'mods' => $this->serviceMods->findAll()->sort(array('name' => 1))
        ));
    }

}
