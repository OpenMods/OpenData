<?php

namespace OpenData\PacketHandlers;

class Ping implements IPacketHandler {

    public function getJsonSchema() {
        return 'ping.json';
    }

    public function getPacketType() {
        return 'ping';
    }
    
    public function execute($packet) {
        
        $packet['type'] = 'pong';
        
        return array(
            $packet
        );
    }
}
