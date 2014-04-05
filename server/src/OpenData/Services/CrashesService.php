<?php

namespace OpenData\Services;

class CrashesService extends BaseService {

    public function add($packet) {

        $packet['date'] = new \MongoDate($packet['date']->format('U'));
        $packet['stackhash'] = md5(serialize($packet['stacktrace']));
        $this->db->crashes->insert($packet);
    }

}
