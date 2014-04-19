<?php

namespace OpenData\PacketHandlers;

use OpenData\Services\ModsService;

class CrashLog implements IPacketHandler {

    private $serviceCrashes;
    private $serviceFiles;
    private $serviceMods;
    
    public function __construct($crashes, $files, $mods) {
        $this->serviceCrashes = $crashes;
        $this->serviceFiles = $files;
        $this->serviceMods = $mods;
    }
    
    public function getJsonSchema() {
        return 'crashlog.json';
    }

    public function getPacketType() {
        return 'crashlog';
    }
    
    public function execute($packet) {
        
        $signatures = array();
        $modIds = array();
        foreach ($packet['states'] as $state) {
            $signatures[] = $state['signature'];
            foreach ($state['mods'] as $mod) {
                $modIds[] = ModsService::sanitizeModId($mod['modId']);
            }
        }
        
        $this->serviceCrashes->add($packet, $signatures, $modIds);
    }

}
