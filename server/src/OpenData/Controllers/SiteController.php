<?php

namespace OpenData\Controllers;


class SiteController {
    
    private $twig;
    
    public function __construct($twig, $request) {
        $this->twig = $twig;
        $this->twig->addFunction(new \Twig_SimpleFunction('relative', function ($string) use ($request) {
            return $request->getBasePath().'/'.$string;
        }));
    }
    
    public function home() {
        return $this->twig->render('home.twig', array());
    }
}
