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

        $this->serviceMods = $serviceMods;
    }

    public function modinfo($modId) {

        $files = $this->serviceMods->findByModId($modId);
        $lastFile = current(iterator_to_array($files->skip($files->count() - 1)->limit(1)));
        $files->reset();

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

        return $this->twig->render('mod.twig', array(
                    'files' => $files,
                    'modData' => $modData
        ));
    }

    public function home() {

        return $this->twig->render('home.twig', array(
                    'mods' => $this->serviceMods->findUniqueMods()
        ));
    }

}
