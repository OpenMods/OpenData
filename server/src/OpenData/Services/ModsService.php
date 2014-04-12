<?php

namespace OpenData\Services;

class ModsService extends BaseService {

    public function findById($modId) {
        return $this->db->mods->findOne(array('_id' => $modId));
    }
    
    public function findAll() {
        return $this->db->mods->find(array('hide' => array('$ne' => true)))->sort(array('name' => 1));
    }

    public function findByIds($modIds = array()) {
        return $this->db->mods->find(array('_id' => array('$in' => $modIds)));
    }

    public function upsert($modId, $data) {
        return $this->db->mods->findAndModify(
            array('_id' => $modId),
            array('$setOnInsert' => $data),
            null,
            array('new' => true, 'upsert' => true)
        );
    }

}
