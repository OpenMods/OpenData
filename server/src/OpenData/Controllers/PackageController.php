<?php

namespace OpenData\Controllers;

class PackageController {

    private $twig;
    private $serviceFiles;
    private $serviceMods;

    public function __construct($twig, $files, $mods, $crashes) {
        $this->twig = $twig;
        $this->serviceFiles = $files;
        $this->serviceMods = $mods;
        $this->serviceCrashes = $crashes;
    }

    public function listAll() {
        return $this->twig->render('package_list.twig', array(
            'packages' => array()
        ));
    }

    public function package($package) {
        return $this->twig->render('package_list.twig', array(
            'packages' => array()
        ));
    }

}
