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
    
    public function commoncrash(Request $request, $slug) {
        $commonCrash = $this->serviceCrashes->getCommonCrashBySlug($slug);
        if ($commonCrash == null) {
            throw new \Exception();
        }
        
        return $this->twig->render('commoncrash.twig', $commonCrash);
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
        
        $response['classTransformers'] = array();
        $response['tweakers'] = array();
        
        $response['unlisted'] = $this->serviceMods->findUnlistedModIds();
        
        if (count($crash['allSignatures']) > 0) {
            $response['allSignatures'] = $this->serviceFiles->findIn($crash['allSignatures']);
            foreach ($response['allSignatures'] as $file) {
                
                $linkFile = true;
                foreach ($file['mods'] as $mod) {
                    if (in_array($mod['modId'], $response['unlisted'])) {
                        $linkFile = false;
                    }
                }
                
                if ($file['classTransformers'] != null) {
                    foreach ($file['classTransformers'] as $transformer) {
                        $response['classTransformers'][$transformer] = $linkFile ? $file['_id'] : '';
                    }
                }
                if ($file['tweakers'] != null) {
                    foreach ($file['tweakers'] as $tweaker) {
                        $response['tweakers'][$tweaker['class']] = $linkFile ? $file['_id'] : '';
                    }
                }
            }
            $response['allSignatures_count'] = $response['allSignatures']->count();
        }
        
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
        
        if ($request->getMethod() == 'POST') {
            $form->handleRequest($request);
        } else {
            $params = $request->query->get('form', array());
            if (count($params) > 0) {
                $form->submit($params, false);
            }
        }

        $query = array('hidden' => array('$ne' => true));
        $invalid = false;
        $data = $form->getData();
        if (count($data) > 0) {
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
                $this->getPagination($results, $request->get('page', 1)),
                array(
                    'form'  => $form->createView()
                )
        ));
    }
    
    
    private function getPagination($iterator, $page = 1, $perPage = 50) {
        
        if ($iterator == null) {
            return array('crashes' => array());
        }
        
        $skip = ($page - 1) * $perPage;
        $total = $iterator->count();
        
        $pageCount = max(1, ((int) ($total - 1) / $perPage) + 1);
        
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
