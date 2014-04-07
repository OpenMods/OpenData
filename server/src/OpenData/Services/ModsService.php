<?php

namespace OpenData\Services;

class ModsService extends BaseService {

    public function findIn($signatures = array()) {
        return $this->db->mods->find(
                        array('_id' => array('$in' => $signatures))
        );
    }

    public function create($id) {
        try {
            $this->db->mods->insert(array(
                '_id' => $id
            ));
        } catch (\MongoCursorException $e) {
            return false;
        }
        return true;
    }

    public function findByModId($modId) {
        return $this->db->mods->find(
                        array('mods.modId' => $modId)
        );
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

        $this->db->mods->update(
                array('_id' => $signature), array('$set' => $currentEntry)
        );

        return true;
    }

    public function findOne($signature) {
        return $this->db->mods->findOne(array('_id' => $signature));
    }

    public function findUniqueMods() {

        $cmd = $this->db->command(array(
            'mapreduce' => 'mods',
            'map' => "function() {
				for (var i = 0; i < this.mods.length; i++) {
					emit({modId: this.mods[i].modId}, {exists: 1, details:this.mods[i]});
				}
			}",
            'reduce' => "function() {
				return {exists: 1};
			}",
            'out' => 'result'
        ));

        $results = $this->db->selectCollection($cmd['result'])->find();

        $mods = array();
        foreach ($results as $result) {
            $mods[] = $result['value']['details'];
        }

        usort($mods, function($a, $b) {
            $al = strtolower($a['modId']);
            $bl = strtolower($b['modId']);
            if ($al == $bl) {
                return 0;
            }
            return ($al > $bl) ? +1 : -1;
        });

        return $mods;
    }

}
