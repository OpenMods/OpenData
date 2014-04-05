<?php

namespace OpenData\Services;

class AnalyticsService extends BaseService {

    public function add($packet) {
        unset($packet['type']);
        $packet['created_at'] = new \DateTime();
        $this->db->analytics->insert($packet);
    }

}
