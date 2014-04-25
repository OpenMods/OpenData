<?php

namespace OpenData\Controllers;
use Symfony\Component\HttpFoundation\Request;

class HomeController {

    private $serviceMods;
    private $twig;
    
    public function __construct($twig, $mods) {
        $this->twig = $twig;
        $this->serviceMods = $mods;
    }
    
    public function home() {
        return $this->twig->render('home.twig', array(
            'mods' => $this->serviceMods->findOrderedByPastDayLaunches(),
            'title' => 'Most popular this week',
            'tags' => $this->serviceMods->getDistinctTags()
        ));
    }
    
    public function all(Request $request) {       
        return $this->twig->render('home.twig', array_merge(
                $this->getPagination(
                    $this->serviceMods->findAll(),
                    $request->get('page', 1),
                    50
                ),
                array(
                    'title' => 'Listing all mods'
                )
        ));
    }
    
    public function letter(Request $request, $letter) {
        return $this->twig->render('home.twig', array_merge(
                $this->getPagination(
                    $this->serviceMods->findByLetter($letter),
                    $request->get('page', 1)
                ),
                array(
                    'title' => 'Mods beginning with "'.strtoupper($letter).'"'
                )
        ));
    }
    
    public function tag(Request $request, $tag) {
        
        $result = $this->serviceMods->findByTag($tag);
        
        if ($result->count() == 0) {
            throw new \Exception();
        }
        
        return $this->twig->render('home.twig', array_merge(
                $this->getPagination(
                    $result,
                    $request->get('page', 1)
                ),
                array(
                    'title' => 'Mods tagged with "'.$tag.'"'
                )
        ));
    }
    
    private function getPagination($iterator, $page = 1, $perPage = 20) {
        
        $skip = ($page - 1) * $perPage;
        $total = $iterator->count();
        
        $pageCount = max(1, ((int) ($total - 1) / $perPage) + 1);
        
        if ($page > $pageCount || $page < 1) {
            throw new \Exception('nope');
        }
        
        return array(
            'mods' => $iterator->skip($skip)->limit($perPage),
            'page_count' => $pageCount,
            'current_page' => $page,
            'total' => $total,
            'disablePrev' => $page <= 1,
            'disableNext' => $page + 1 >= $pageCount,
            'tags' => $this->serviceMods->getDistinctTags()
        );
        
    }
    
}
