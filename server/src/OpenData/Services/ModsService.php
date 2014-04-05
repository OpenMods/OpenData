<?php

namespace OpenData\Services;

class ModsService extends BaseService {

    public function findIn($signatures = array()) {
        return $this->db->mods->find(
                        array('_id' => array('$in' => $signatures))
        );
    }

    public function add($file) {
        
        $this->db->mods->insert(array(
            '_id' => $file['signature']
        ));

        return $file['signature'];
    }
    
    public function update($file) {
        
        $signature = $file['signature'];
        unset($file['signature']);
        
        $this->db->mods->update(
            array('_id' => $signature),
            array('$set' => $file)
        );
    
    }
    
    public function findOne($signature) {
        return $this->db->mods->findOne(array('_id' => $signature));
    }

}
