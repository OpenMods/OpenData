<?php

namespace OpenData\Controllers;

use Symfony\Component\HttpFoundation\JsonResponse;
use Symfony\Component\HttpFoundation\Request;
use JsonSchema\Uri\UriRetriever;
use JsonSchema\Validator;

class ApiController {

    protected $serviceCrashes;
    protected $serviceAnalytics;
    protected $serviceMods;
    protected $memcache;
    
    protected $schemas;
    
    private static $packetTypes = array(
        'analytics',
        'crashlog',
        'mod_info'
    );

    public function __construct($crashes, $analytics, $mods, $memcache) {
        
        $this->serviceCrashes = $crashes;
        $this->serviceAnalytics = $analytics;
        $this->serviceMods = $mods;
        
        $this->memcache = $memcache;
        
        $this->schemas = array();

        foreach (self::$packetTypes as $schema) {
            $retriever = new UriRetriever();
            $this->schemas[$schema] = $retriever->retrieve('file://'.__DIR__ . '/../Schemas/' . $schema . '.json');
        }
    }

    public function main(Request $request) {
        
        if ($this->isUserFlooding($request)) {
            return new JsonResponse(array());
        }

        $data = json_decode($request->getContent(), true);

        if (!is_array($data)) {
            throw new \Exception('Array expected');
        }

        $responses = array();

        foreach ($data as $packet) {

            if (!isset($packet['type']) || !is_string($packet['type'])) {
                throw new \Exception('Packet type not defined');
            }

            $type = $packet['type'];
            
            unset($packet['type']);

            $response = null;

            if (!in_array($type, self::$packetTypes)) {
                throw new \Exception('Invalid packet type');
            }

            $errors = $this->getErrors($packet);

            if ($errors != null) {
                throw new \Exception(implode("\n", $errors));
            }
            

            switch ($type) {
                case 'analytics':
                    $response = $this->analytics($packet);
                    break;
                case 'crashlog':
                    $response = $this->crashlog($packet);
                    break;
                case 'mod_info':
                    $response = $this->modinfo($packet);
                    break;
            }

            if ($response != null) {
                $responses = array_merge($responses, $response);
            }
        }

        return new JsonResponse($responses);
    }

    private function isUserFlooding(Request $request) {
        
        if ($this->memcache == null) return false;
        
        $key = sha1($request->getClientIp().date('Y-m-d-H'));
        $requestCount = $this->memcache->get($key);
 
        if ($requestCount) {
            if ($requestCount > 5) {
                return true;
            }
            $this->memcache->replace($key, $requestCount+1, 0, 3600);
        } else {
            $this->memcache->set($key, 1, 0, 3600);
        }
        
        return false;        
    }
    
    private function modinfo($packet) {
        $this->serviceMods->update($packet);
    }
    
    private function crashlog($packet) {
        // allow this to throw
        $date = new \DateTime($packet['date'], new \DateTimeZone($packet['timezone']));;
        $date->setTimezone(new \DateTimeZone('Europe/London'));
        $packet['date'] = $date;
        unset($packet['timezone']);
        $this->serviceCrashes->add($packet);
    }

    private function analytics($packet) {
        
        $packet['created_at'] = new \MongoDate();
        
        $this->serviceAnalytics->add($packet);

        // find all the mods we already have in the database
        $filesData = $this->serviceMods->findIn($packet['files']);

        $responses = array();

        $fileSignaturesFound = array();

        // loop through them all and check for any additional data needed
        // for example, packages, classes, file, or security/update warnings
        foreach ($filesData as $fileData) {

            $fileSignaturesFound[] = $fileData['_id'];

            $fileNode = array(
                'signature' => $fileData['_id']
            );

            if (!isset($fileData['packages'])) {
                $responses[] = array_merge($fileNode, array(
                    'type' => 'mod_info'
                ));
            }

            if ($this->shouldRequestFiles($fileData)) {
                $responses[] = array_merge($fileNode, array(
                    'type' => 'list_files'
                ));
            }

            if ($this->shouldUploadFile($fileData)) {
                $responses[] = array_merge($fileNode, array(
                    'type' => 'upload_file'
                ));
            }

            if (isset($fileData['security_warning']) && is_string($fileData['security_warning'])) {
                $responses[] = array_merge($fileNode, array(
                    'type' => 'security_warning',
                    'message' => $fileData['security_warning']
                ));
            }

            if (isset($fileData['notes']) && is_array($fileData['notes'])) {
                foreach ($fileData['notes'] as $note) {
                    $responses[] = array_merge($fileNode, array(
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
        foreach ($packet['files'] as $file) {
            if (!in_array($file['signature'], $fileSignaturesFound)) {
                $this->serviceMods->add($file);
                $responses[] = array(
                    'type' => 'mod_info',
                    'signature' => $file['signature']
                );
            }
        }


        return $responses;
    }

    private function shouldRequestFiles($fileData) {
        return isset($fileData['list_files']) &&
                $fileData['list_files'] &&
                !isset($fileData['files']);
    }

    private function shouldUploadFile($fileData) {
        return isset($fileData['upload_file']) &&
                $fileData['upload_file'] &&
                !isset($fileData['file_id']);
    }

    private function getErrors($packet) {

        // real nasty, but the json validator requires we pass in as an stdClass.
        // so we'll recode it as a class.
        $packet = json_decode(json_encode($packet), false);

        $validator = new Validator();
        $validator->check($packet, $this->schemas[$packet->type]);
        if (!$validator->isValid()) {
            $errors = array();
            foreach ($validator->getErrors() as $error) {
                $errors[] = sprintf("[%s] %s\n", $error['property'], $error['message']);
            }
            return $errors;
        }

        return null;
    }

}
