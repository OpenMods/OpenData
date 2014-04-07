<?php

namespace OpenData\Controllers;

class SiteController {

    private $twig;
    private $serviceMods;

    public function __construct($twig, $request, $serviceMods) {
        $this->twig = $twig;
        $this->twig->addFunction(new \Twig_SimpleFunction('relative', function ($string) use ($request) {
            return $request->getBasePath() . '/' . $string;
        }));
        $this->twig->addFilter(new \Twig_SimpleFilter('id', function ($string) {
            return substr(md5($string), 0, 5);
        }));

        $this->serviceMods = $serviceMods;
    }

    public function modinfo($modId) {

        $files = $this->serviceMods->findByModId($modId);

        $numFiles = $files->count();
        
        if ($numFiles == 0) {
            throw new \Exception();
        }

        $lastFile = current(iterator_to_array($files->skip($numFiles - 1)->limit(1)));
        
        $files = $this->serviceMods->findByModId($modId);
        
        
        if ($lastFile == null) {
            throw new \Exception();
        }

        $modData = null;

        foreach ($lastFile['mods'] as $mod) {
            if ($mod['modId'] == $modId) {
                $modData = $mod;
            }
        }

        if ($modData == null) {
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
                    'modData' => $modData
        ));
    }

    public function home() {

        return $this->twig->render('home.twig', array(
                    'mods' => $this->serviceMods->findUniqueMods()
        ));
    }

}
