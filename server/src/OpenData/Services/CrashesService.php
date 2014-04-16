<?php

namespace OpenData\Services;

class CrashesService extends BaseService {

    public function add($packet) {
        $packet['stackhash'] = md5(serialize($packet['stack']));
        
        $this->db->unique_crashes->update(
            array('stackhash' => $packet['stackhash']),
            array(
                '$set' => array(
                    'exception' => $packet['exception'],
                    'stackhash' => $packet['stackhash'],
                    'message'   => $packet['message'],
                    'stack'     => $packet['stack']
                ),
                '$inc' => array('count' => 1)
            ),
            array('upsert' => true)
        );
        
        $this->db->crashes->insert($packet);
    }
    
    
    public function findByPackage($package, $skip = 0, $limit = 40) {
        return $this->find(array('stack.class' => new \MongoRegex('/^'.preg_quote($package).'\./')))
                ->sort(array('timestamp' => -1))
                ->skip($skip)
                ->limit($limit);
           
    }
    
    public function findLatest($skip = 0, $limit = 40) {
        return $this->find()
                ->sort(array('timestamp' => -1))
                ->skip($skip)
                ->limit($limit);
    }

    private function find($query = array()) {
        return $this->db->crashes->find($query);
    }
    
}
