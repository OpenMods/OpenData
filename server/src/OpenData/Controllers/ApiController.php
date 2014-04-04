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
                $responses[] = $response;
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
                'signature' => $modData['_id'],
                'modId' => $modData['modId']
            );

            $commands = array();

            if (!isset($modData['packages'])) {
                $commands['list_packages'] = array();
            }

            if ($this->shouldRequestFiles($modData)) {
                $commands['list_files'] = array();
            }

            if ($this->shouldUploadFile($modData)) {
                $commands['upload_file'] = array();
            }

            if (isset($modData['security_warning']) && $modData['security_warning']) {
                $commands['security_warning'] = $modData['security_warning'];
            }

            if (isset($modData['update_critical']) && $modData['update_critical']) {
                $commands['update_critical'] = $modData['update_critical'];
            }

            if (isset($modData['update_normal']) && $modData['update_normal']) {
                $commands['update_normal'] = $modData['update_normal'];
            }

            if (count($commands) > 0) {
                $modNode['commands'] = $commands;
                $responses[] = $modNode;
            }
        }

        // loop through any mods we didn't find in the database,
        // add them in, then tell the client we need the rest of the packages
        foreach ($packet['mods'] as $mod) {
            if (!in_array($mod['signature'], $modsSignaturesFound)) {
                $this->serviceMods->add($mod);
                $responses[] = array(
                    'signature' => $mod['signature'],
                    'modId' => $mod['modId'],
                    'commands' => array(
                        'list_packages' => array()
                    )
                );
            }
        }


        if (count($responses) > 0) {
            return $responses;
        }
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
