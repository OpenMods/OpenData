<?php

namespace OpenData\Services;

class ModsService extends BaseService {

    public function findIn($signatures = array()) {
        return $this->db->mods->find(
                        array('_id' => array('$in' => $signatures))
        );
    }

    public function add($mod) {

        $document = array(
            '_id' => $mod['signature'],
            'modId' => $mod['modId'],
            'version' => $mod['version'],
            'filesize' => $mod['filesize']
        );

        $this->db->mods->insert($document);

        return (string) $document['_id'];
    }

}
