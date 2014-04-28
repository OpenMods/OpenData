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
    
    public function view(Request $request, $stackhash) {
        
        $crash = $this->serviceCrashes->findByStackhash($stackhash);
        if ($crash == null) {
            throw new \Exception();
        }
        
        $response = array(
            'crash' => $crash
        );
        
        
        if (count($crash['involvedMods']) > 0) {
            $response['involvedMods'] = $this->serviceMods->findByIds($crash['involvedMods']);
            $response['involvedMods_count'] = $response['involvedMods']->count();
        }
        
        if (count($crash['allMods']) > 0) {
            $response['allMods'] = $this->serviceMods->findByIds($crash['allMods']);
            $response['allMods_count'] = $response['allMods']->count();
        }
        
        if (count($crash['involvedSignatures']) > 0) {
            $response['involvedSignatures'] = $this->serviceFiles->findIn($crash['involvedSignatures']);
            $response['involvedSignatures_count'] = $response['involvedSignatures']->count();
        }
        
        if (count($crash['allSignatures']) > 0) {
            $response['allSignatures'] = $this->serviceFiles->findIn($crash['allSignatures']);
            $response['allSignatures_count'] = $response['allSignatures']->count();
        }
        
        $response['unlisted'] = $this->serviceMods->findUnlistedModIds();
        
        return $this->twig->render('crash.twig', $response);
        
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

        $query = array();
        $invalid = false;
        if ($form->isValid()) {
            
            $data = $form->getData();
            $query = array();
            if (!empty($data['package'])) {
                $query['classes'] = new \MongoRegex('/^'.preg_quote($data['package']).'/i');
            }
            if (!empty($data['mod'])) {
                $mods = $this->serviceMods->findByRegex('^'.$data['mod'].'$');
                if ($mods->count() > 0) {
                    $mod = $mods->getNext();
                    $modId = $mod['_id'];
                    $query['involvedMods'] = $modId;
                } else {
                    $invalid = true;
                }
            }
            if (!empty($data['signature'])) {
                $query['involvedSignatures'] = $data['signature'];
            }
        }
        
        $results = array();
        if (!$invalid) {
            $results = $this->serviceCrashes->findLatest($query);
        }

        return $this->twig->render('crashes.twig', array_merge(
                $this->getPagination($results),
                array(
                    'form'  => $form->createView()
                )
        ));
    }
    
    
    private function getPagination($iterator, $page = 1, $perPage = 20) {
        
        if ($iterator == null) {
            return array('crashes' => array());
        }
        
        $skip = ($page - 1) * $perPage;
        $total = $iterator->count();
        
        $pageCount = ((int) $total / $perPage) + 1;
        
        if ($page > $pageCount || $page < 1) {
            throw new \Exception('nope');
        }
        
        return array(
            'crashes' => $iterator->skip($skip)->limit($perPage),
            'page_count' => $pageCount,
            'current_page' => $page,
            'total' => $total,
            'disablePrev' => $page <= 1,
            'disableNext' => $page + 1 >= $pageCount
        );
    }
}
