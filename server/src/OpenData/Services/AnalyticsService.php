<?php

namespace OpenData\Services;

class AnalyticsService extends BaseService {

    public function add($packet) {
        // just throw it in as-is for now
        unset($packet['type']);
        $this->db->analytics->insert($packet);
    }

}
