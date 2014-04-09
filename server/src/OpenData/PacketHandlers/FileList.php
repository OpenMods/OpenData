<?php

namespace OpenData\PacketHandlers;

class FileList implements IPacketHandler {

    private $serviceFiles;
    
    public function __construct($files) {
        $this->serviceFiles = $files;
    }
    
    public function getJsonSchema() {
        return 'filelist.json';
    }

    public function getPacketType() {
        return 'filelist';
    }
    
    public function execute($packet) {
        $this->serviceFiles->append($packet);
    }

}
