<?php

namespace OpenData\Services;

class CrashesService extends BaseService {

    public function add($packet) {

        $packet['date'] = new \MongoDate($packet['date']->format('U'));
        $packet['stackhash'] = md5(serialize($packet['stacktrace']));
        $this->db->crashes->insert($packet);
    }
    
    public function findLatest($skip = 0, $limit = 40) {
        return $this->db->crashes->find()
                                    ->sort(array('timestamp' => -1))
                                    ->skip($skip)
                                    ->limit($limit);
    }

}
