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

        //$packages = $this->serviceFiles->findUniquePackages();
        //usort($packages, 'strcasecmp');
        return $this->twig->render('package_list.twig', array(
            'packages' => array()
        ));
    }

    public function package($package) {

        $files = $this->serviceFiles->findByPackage($package)->sort(array('filename' => 1));

        if ($files->count() == 0) {
            throw new \Exception('Unknown package');
        }

        $modIds = $this->serviceFiles->findUniqueModIdsForPackage($package);

        $modList = array();

        foreach ($this->serviceMods->findByIds($modIds) as $mod) {
            $mod['files'] = array();
            $modList[$mod['_id']] = $mod;
        }

        foreach ($files as $file) {
            foreach ($file['mods'] as $mod) {
                if (isset($modList[$mod['modId']])) {
                    $modList[$mod['modId']]['files'][] = $file;
                }
            }
        }

        $parentPackage = null;
        if (substr_count($package, '.') > 0) {
            $parentPackage = substr($package, 0, (strlen ($package)) - (strlen (strrchr($package,'.'))));
            if (!$this->serviceFiles->hasPackage($parentPackage)) {
                $parentPackage = null;
            }
        }

        return $this->twig->render('package.twig', array(
            'packageName' => $package,
            'mods' => $modList,
            'subpackages' => $this->serviceFiles->findSubPackages($package),
            'crashes' => iterator_to_array($this->serviceCrashes->findByPackage($package)),
            'parent' => $parentPackage
        ));
    }

}
