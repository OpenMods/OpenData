<?php

namespace OpenData\Services;

class FilesService extends BaseService {

    public function findIn($signatures = array()) {
        return $this->db->files->find(
                        array('_id' => array('$in' => $signatures))
        );
    }

    public function create($id) {
        try {
            $this->db->files->insert(array(
                '_id' => $id
            ));
        } catch (\MongoCursorException $e) {
            return false;
        }
        return true;
    }

    public function findByModId($modId) {
        return $this->db->files->find(
            array('mods.modId' => $modId)
        );
    }
    
    public function findByPackage($package) {
        return $this->db->files->find(
            array('packages' => $package)
        );
    }
    
    public function findUniqueModIdsForPackage($package) {
        return array();
        //db.files.aggregate({'$match' : {'packages' : 'thaumcraft.api'}}, {'$project' : {'mods' : 1}}, {'$unwind' : '$mods'}, {'$group' : {'_id' : '$mods.modId'}})
    }

    public function append($file) {

        $signature = $file['signature'];

        $currentEntry = $this->findOne($signature);

        if ($currentEntry == null) {
            return false;
        }
        foreach ($file as $k => $v) {
            if (!isset($currentEntry[$k])) {
                $currentEntry[$k] = $v;
            }
        }

        unset($file['signature']);
        unset($currentEntry['_id']);

        $this->db->files->update(
                array('_id' => $signature), array('$set' => $currentEntry)
        );

        return true;
    }

    public function findOne($signature) {
        return $this->db->files->findOne(array('_id' => $signature));
    }

}
