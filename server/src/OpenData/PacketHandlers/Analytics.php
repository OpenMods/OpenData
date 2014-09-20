<?php


namespace OpenData\PacketHandlers;

class Analytics implements IPacketHandler {

    private $serviceAnalytics;
    private $serviceFiles;
    private $serviceTags;
	private static $invalidFilenames = array(
			'minecraft.jar',
			'1.6.4-Forge9.11.1.965.jar',
			'1.7.2-Forge10.12.1.1060.jar',
			'scala-compiler-2.10.2.jar',
			'scala-library-2.10.2.jar',
			'1.7.2-Forge10.12.0.1047.jar'
		);

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

        $client = new \Predis\Client();

        $client->pipeline(function ($pipe) use ($packet) {
            $singleKeys = array('language', 'locale', 'timezone', 'minecraft', 'javaVersion');
            $multipleKeys = array('branding', 'tags');
            foreach ($packet['signatures'] as $signature) {
                $sig = $signature['signature'];
                $pipe->sadd('signatures', $sig);
                $pipe->incrby($sig, 1);
                foreach ($singleKeys as $key) {
                    if (isset($packet[$key])) {
                        $pipe->hincrby($sig.':'.$key, $packet[$key], 1);
                    }
                }

                foreach ($multipleKeys as $key) {
                    if (isset($packet[$key])) {
                        foreach ($packet[$key] as $item) {
                                $pipe->hincrby($sig.':'.$key, $item, 1);
                        }
                    }
                }

				if (isset($packet['runtime'])) {
                    foreach ($packet['runtime'] as $ware => $version) {
                        $pipe->hincrby($sig.':'.$ware, $version, 1);
                    }
                }
            }
		});


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

            if ($this->shouldUploadFile($fileData)) {
                $responses[] = array_merge($fileNode, array(
                    'type' => 'upload_file'
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
                        'payload' => isset($note['payload']) ? $note['payload'] : "",
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
			if (!in_array($signature['signature'], $fileSignaturesFound) && !in_array($signature['filename'], self::$invalidFilenames)) {
				if ($this->serviceFiles->create($signature)) {
					$newFilenames[] = $signature['filename'];
					$responses[] = array(
						'type' => 'file_info',
						'signature' => $signature['signature']
					);
				}
			}
        }

        if (count($newFilenames) > 0) {
            $redis = new \Predis\Client();
            $redis->publish('file', implode(", ", $newFilenames));
        }

        return $responses;
    }

    private function shouldRequestFiles($fileData) {
        return isset($fileData['request_files']) &&
                $fileData['request_files'] &&
                !isset($fileData['files']);
    }

    private function shouldUploadFile($fileData) {
        return isset($fileData['upload_file']) &&
                $fileData['upload_file'] &&
                !isset($fileData['file_id']);
    }

}
