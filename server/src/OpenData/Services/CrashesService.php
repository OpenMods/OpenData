<?php

namespace OpenData\Services;

class CrashesService extends BaseService {

    public function add($packet) {

        // just throw it in for now!
        unset($packet['type']);
        $packet['date'] = new \MongoDate($packet['date']->format('U'));
        $packet['stackhash'] = md5(serialize($packet['stacktrace']));
        $this->db->crashes->insert($packet);
    }

}
