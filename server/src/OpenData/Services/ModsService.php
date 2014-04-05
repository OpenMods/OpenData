<?php

namespace OpenData\Services;

class ModsService extends BaseService {

    public function findIn($signatures = array()) {
        return $this->db->mods->find(
                        array('_id' => array('$in' => $signatures))
        );
    }

    public function add($file) {

        $document = array(
            '_id' => $file['signature'],
            'mods' => $file['mods'],
            'filesize' => $file['filesize'],
            'filename' => $file['filename']
        );

        $this->db->mods->insert($document);

        return (string) $document['_id'];
    }

}
