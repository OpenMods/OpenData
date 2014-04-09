<?php

namespace OpenData\Controllers;

class HomeController {

    private $serviceMods;
    private $twig;
    
    public function __construct($twig, $mods) {
        $this->twig = $twig;
        $this->serviceMods = $mods;
    }
    
    public function home() {
        return $this->twig->render('home.twig', array(
            'mods' => $this->serviceMods->findAll()->sort(array('name' => 1))
        ));
    }
}
