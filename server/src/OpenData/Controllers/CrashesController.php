<?php

namespace OpenData\Controllers;

use Symfony\Component\HttpFoundation\Request;

class CrashesController {

    private $twig;
    
    private $serviceMods;
    private $serviceFiles;
    private $serviceCrashes;
    private $serviceForms;
    
    public function __construct($twig, $mods, $files, $crashes, $forms) {
        
        $this->twig = $twig;
        $this->serviceMods = $mods;
        $this->serviceFiles = $files;
        $this->serviceCrashes = $crashes;
        $this->serviceForms = $forms;
    }
    
    public function search(Request $request) {
        
        $data = array();
        
        $form = $this->serviceForms->createBuilder('form', $data)
            ->add('mod', 'text', array('required' => false))
            ->add('version', 'text', array('required' => false))
            ->add('signature', 'text', array('required' => false))
            ->add('package', 'text', array('required' => false))
            ->getForm();

        $form->handleRequest($request);

        if ($form->isValid()) {
            $data = $form->getData();

        }

        return $this->twig->render('crashes.twig', array(
            'crashes' => $this->serviceCrashes->findLatestUnique(),
            'form'  => $form->createView()
        ));
    }
    
}
