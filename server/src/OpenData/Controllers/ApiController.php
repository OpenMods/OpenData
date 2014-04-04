<?php

namespace OpenData\Controllers;

use Symfony\Component\HttpFoundation\JsonResponse;
use Symfony\Component\HttpFoundation\Request;

class ApiController {

    protected $serviceCrashes;
    protected $serviceAnalytics;
    protected $serviceMods;

    public function __construct($crashes, $analytics, $mods) {
        $this->serviceCrashes = $crashes;
        $this->serviceAnalytics = $analytics;
        $this->serviceMods = $mods;
    }

    public function main(Request $request) {

        $data = json_decode($request->getContent(), true);

        if (!is_array($data)) {
            throw new \Exception('Array expected');
        }

        $responses = array();

        foreach ($data as $packet) {

            if (!isset($packet['type']) || !is_string($packet['type'])) {
                throw new \Exception('Packet type not defined');
            }

            switch ($packet['type']) {

                case 'analytics':
                    $response = $this->analytics($packet);
                    break;
                case 'crashlog':
                    $response = $this->crashlog($packet);
                    break;
                case 'mod_packages':
                    $response = $this->packages($packet);
                    break;
                case 'mod_files':
                    $response = $this->files($packet);
                    break;
                default:
                    throw new \Exception('Unknown packet type ' . $packet['type']);
            }

            if ($response != null) {
                $responses = array_merge($responses, $response);
            }
        }

        return new JsonResponse($responses);
    }

    private function crashlog($packet) {
        $this->serviceCrashes->add($packet);
    }

    private function analytics($packet) {

        if (!isset($packet['mods']) || !is_array($packet['mods'])) {
            throw new \Exception('Expected a mods array in analytics packet');
        }

        $this->serviceAnalytics->add($packet);

        $signatures = array();

        foreach ($packet['mods'] as $mod) {
            if (!is_array($mod) || !is_string($mod['signature'])) {
                throw new \Exception('Expected a signature for each mod in analytics packet');
            }
            $signatures[] = $mod['signature'];
        }

        // find all the mods we already have in the database
        $modsData = $this->serviceMods->findIn($signatures);

        $responses = array();

        $modsSignaturesFound = array();

        // loop through them all and check for any additional data needed
        // for example, packages, classes, file, or security/update warnings
        foreach ($modsData as $modData) {

            $modsSignaturesFound[] = $modData['_id'];

            $modNode = array(
                'modId' => $modData['modId']
            );

            if (!isset($modData['packages'])) {
                $responses[] = array_merge($modNode, array(
                    'type' => 'list_packages'
                ));
            }

            if ($this->shouldRequestFiles($modData)) {
                $responses[] = array_merge($modNode, array(
                    'type' => 'mod_files'
                ));
            }

            if ($this->shouldUploadFile($modData)) {
                $responses[] = array_merge($modNode, array(
                    'type' => 'upload_file'
                ));
            }

            if (isset($modData['security_warning']) && is_string($modData['security_warning'])) {
                $responses[] = array_merge($modNode, array(
                    'type' => 'security_warning',
                    'message' => $modData['security_warning']
                ));
            }

            if (isset($modData['notes']) && is_array($modData['notes'])) {
                foreach ($modData['notes'] as $note) {
                    $responses[] = array_merge($modNode, array(
                        'type' => 'note',
                        'note_type' => $note['type'],
                        'priority' => $note['priority'],
                        'message' => $note['message']
                    ));
                }
            }
        }

        // loop through any mods we didn't find in the database,
        // add them in, then tell the client we need the rest of the packages
        foreach ($packet['mods'] as $mod) {
            if (!in_array($mod['signature'], $modsSignaturesFound)) {
                $this->serviceMods->add($mod);
                $responses[] = array(
                    'type' => 'list_packages',
                    'modId' => $mod['modId']
                );
            }
        }


        return $responses;
    }

    private function shouldRequestFiles($modData) {
        return isset($modData['list_files']) &&
                $modData['list_files'] &&
                !isset($modData['files']);
    }

    private function shouldUploadFile($modData) {
        return isset($modData['upload_file']) &&
                $modData['upload_file'] &&
                !isset($modData['file_id']);
    }

}
