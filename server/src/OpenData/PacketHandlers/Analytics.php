<?php


namespace OpenData\PacketHandlers;

class Analytics implements IPacketHandler {
    
    private $serviceAnalytics;
    private $serviceFiles;
    
    public function __construct($analytics, $files) {
        $this->serviceAnalytics = $analytics;
        $this->serviceFiles = $files;
    }
    
    public function getPacketType() {
        return 'analytics';
    }
    
    public function getJsonSchema() {
        return 'analytics.json';
    }
    
    public function execute($packet) {

        $packet['created_at'] = new \MongoDate();

        $this->serviceAnalytics->add($packet);
        
        $signatureMap = array();
        
        foreach ($packet['signatures'] as $signature) {
            $signatureMap[$signature['signature']] = $signature['filename'];
        }
        
        // find all the mods we already have in the database
        $filesData = $this->serviceFiles->findIn(array_keys($signatureMap));

        $responses = array();

        $fileSignaturesFound = array();

        // loop through them all and check for any additional data needed
        // for example, packages, classes, file, or security/update warnings
        foreach ($filesData as $fileData) {

            $signature = $fileData['_id'];
            
            $fileSignaturesFound[] = $signature;

            $fileNode = array(
                'signature' => $signature
            );
            
            if (isset($signatureMap[$signature])) {
                $filename = $signatureMap[$signature];
                if (!in_array($filename, $fileData['filenames'])) {
                    $fileData['filenames'][] = $filename;
                    $this->serviceFiles->append(array(
                       'signature' => $fileData['_id'],
                       'filenames' => $fileData['filenames']
                    ), true);
                }
            }

            if (!isset($fileData['packages'])) {
                $responses[] = array_merge($fileNode, array(
                    'type' => 'file_info'
                ));
            }

            if ($this->shouldRequestFiles($fileData)) {
                $responses[] = array_merge($fileNode, array(
                    'type' => 'filelist'
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
        foreach ($packet['signatures'] as $signature) {
            
            if (!in_array($signature['signature'], $fileSignaturesFound)) {
                $this->serviceFiles->create($signature);
                $responses[] = array(
                    'type' => 'file_info',
                    'signature' => $signature['signature']
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

}
