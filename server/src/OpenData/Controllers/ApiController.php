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
    protected $schemas;

    public function __construct($crashes, $analytics, $mods) {
        $this->serviceCrashes = $crashes;
        $this->serviceAnalytics = $analytics;
        $this->serviceMods = $mods;
        $this->schemas = array();

        foreach(array('analytics') as $schema) {
			$retriever = new UriRetriever();
			$this->schemas[$schema] = $retriever->retrieve(__DIR__.'/../Schemas/analytics.json');
		}
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

			$errors = null;

			// handle the packet
            switch ($packet['type']) {

                case 'analytics':
					$errors = $this->validatePacket($packet, 'analytics');
					if (count($errors) == 0) {
                    	$response = $this->analytics($packet);
					}
                    break;
                case 'crashlog':
					$errors = $this->validatePacket($packet, 'crashlog');
					if (count($errors) == 0) {
                    	$response = $this->crashlog($packet);
					}
                    break;
                case 'mod_packages':
					$errors = $this->validatePacket($packet, 'mod_packages');
					if (count($errors) == 0) {
                	    $response = $this->packages($packet);
					}
                    break;
                case 'mod_files':
					$errors = $this->validatePacket($packet, 'mod_files');
					if (count($errors) == 0) {
                    	$response = $this->files($packet);
					}
                    break;
                default:
                    throw new \Exception('Unknown packet type ' . $packet['type']);
            }

            if (is_array($errors) && count($errors) > 0) {
				throw new \Exception(implode("\n", $errors));
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

        $this->serviceAnalytics->add($packet);

        $signatures = array();

        foreach ($packet['files'] as $file) {
            $signatures[] = $file['signature'];
        }

        // find all the mods we already have in the database
        $filesData = $this->serviceMods->findIn($signatures);

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
                    'type' => 'list_packages'
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
                    'type' => 'list_packages',
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


	private function validatePacket($packet, $schemaType) {

		$errors = array();


		// real nasty, but the json validator requires we pass in as an stdClass.
		// so we'll recode it as a class.
		$packet = json_decode(json_encode($packet), false);

		if (!isset($this->schemas[$schemaType])) {
			$errors[] =	'Invalid action type';
		} else {
			$validator = new Validator();
			$validator->check($packet, $this->schemas[$schemaType]);
			if (!$validator->isValid()) {
			    foreach ($validator->getErrors() as $error) {
					$errors[] = sprintf("[%s] %s\n", $error['property'], $error['message']);
				}
			}
		}


		return $errors;
	}
}
