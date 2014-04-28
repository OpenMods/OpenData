<?php


namespace OpenData\PacketHandlers;

class Analytics implements IPacketHandler {
    
    private $serviceAnalytics;
    private $serviceFiles;
    private $serviceTags;
    
    public function __construct($analytics, $files, $tags) {
        $this->serviceAnalytics = $analytics;
        $this->serviceFiles = $files;
        $this->serviceTags = $tags;
    }
    
    public function getPacketType() {
        return 'analytics';
    }
    
    public function getJsonSchema() {
        return 'analytics.json';
    }
    
    public function execute($packet) {

        $packet['created_at'] = time();
        
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
                    'type' => 'file_contents'
                ));
            }

            if (isset($fileData['dangerous_file']) && $fileData['dangerous_file'] == true) {
                $responses[] = array_merge($fileNode, array(
                    'type' => 'dangerous_file'
                ));
            }

            if (isset($fileData['notes']) && is_array($fileData['notes'])) {
                foreach ($fileData['notes'] as $note) {
                    $responses[] = array_merge($fileNode, array(
                        'type' => 'note',
                        'payload' => $note['payload'],
                        'level' => $note['level'],
                        'description' => $note['description']
                    ));
                }
            }
        }

        $newFilenames = array();
        
        // loop through any mods we didn't find in the database,
        // add them in, then tell the client we need the rest of the packages
        foreach ($packet['signatures'] as $signature) {
            
            if (!in_array($signature['signature'], $fileSignaturesFound)) {
                if ($this->serviceFiles->create($signature)) {
                    $newFilenames[] = $signature['filename'];
                }
                $responses[] = array(
                    'type' => 'file_info',
                    'signature' => $signature['signature']
                );
            }
        }
        
        if (count($newFilenames) > 0) {
            $redis = new \Predis\Client();
            $redis->publish('file', 'New files detected: '.implode(", ", $newFilenames));
        }

        return $responses;
    }
    
    private function shouldRequestFiles($fileData) {
        return isset($fileData['request_files']) &&
                $fileData['request_files'] &&
                !isset($fileData['files']);
    }

}
