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

        if ($commonCrash = $this->serviceCrashes->getCommonCrashDetails($packet)) {

            $crashDetails = $this->serviceCrashes->add($packet, true);
            return array(array(
                    'type' => 'known_crash',
                    'url' => 'http://openeye.openmods.info/commoncrash/' . $commonCrash['url'],
                    'note' => $commonCrash['message']
            ));
        } else {

            $crashDetails = $this->serviceCrashes->add($packet);
            if ($crashDetails != null) {
                return array(array(
                        'type' => 'known_crash',
                        'url' => 'http://openeye.openmods.info/crashes/' . $crashDetails['stackhash'],
                        'note' => $crashDetails['note'] == null ? null : str_replace('%', '', $crashDetails['note']['message'])
                ));
            }
        }
    }

}
