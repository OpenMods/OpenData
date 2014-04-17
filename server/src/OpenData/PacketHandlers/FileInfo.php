<?php

namespace OpenData\PacketHandlers;

use OpenData\Services\ModsService;

class FileInfo implements IPacketHandler {
    
    private $serviceFiles;
    private $serviceMods;
    
    public function __construct($files, $mods) {
        $this->serviceFiles = $files;
        $this->serviceMods = $mods;
    }
    
    public function getJsonSchema() {
        return 'file_info.json';
    }

    public function getPacketType() {
        return 'file_info';
    }
    
    public function execute($packet) {
                
        $this->serviceFiles->append($packet);
        
        if (isset($packet['mods'])) {
            foreach ($packet['mods'] as $mod) {
                if (empty($mod['parent'])) {
                    $this->serviceMods->upsert(ModsService::sanitizeModId($mod['modId']), array(
                        'authors' => $mod['authors'],
                        'credits' => $mod['credits'],
                        'description' => $mod['description'],
                        'name' => $mod['name'],
                        'parent' => $mod['parent'],
                        'url' => $mod['url'],
                        'updateUrl' => $mod['updateUrl']
                    ));
                }
            }
        }
    }

    
}
