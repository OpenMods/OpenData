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

        $date = new \DateTime($packet['date'], new \DateTimeZone($packet['timezone']));
        $date->setTimezone(new \DateTimeZone('Europe/London'));
        $packet['date'] = $date;
        unset($packet['timezone']);
        $this->serviceCrashes->add($packet);
        
    }

}
