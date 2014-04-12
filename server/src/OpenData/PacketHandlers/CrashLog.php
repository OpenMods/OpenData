<?php

namespace OpenData\PacketHandlers;

class CrashLog implements IPacketHandler {

    private $serviceCrashes;
    
    public function __construct($crashes) {
        $this->serviceCrashes = $crashes;
    }
    
    public function getJsonSchema() {
        return 'crashlog.json';
    }

    public function getPacketType() {
        return 'crashlog';
    }
    
    public function execute($packet) {
        $this->serviceCrashes->add($packet);
    }

}
