<?php

namespace OpenData\PacketHandlers;

interface IPacketHandler {
    
    /**
     * @return string packet type
     */
    public function getPacketType();
    
    /**
     * @return string schema filename
     */
    public function getJsonSchema();
    
    /**
     * @param array $packet
     * @return array|null Response packets
     */
    public function execute($packet);
    
}
