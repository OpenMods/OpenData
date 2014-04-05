<?php

namespace OpenData\Services;

class AnalyticsService extends BaseService {

    public function add($packet) {
        $this->db->analytics->insert($packet);
    }

}
