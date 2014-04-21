<?php

namespace OpenData\PacketHandlers;

class FileList implements IPacketHandler {

    private $serviceFiles;
    
    public function __construct($files) {
        $this->serviceFiles = $files;
    }
    
    public function getJsonSchema() {
        return 'files.json';
    }

    public function getPacketType() {
        return 'file_contents';
    }
    
    public function execute($packet) {
        $this->serviceFiles->append($packet);
    }

}
