<?php

namespace OpenData\Services;

class AnalyticsService extends BaseService {

    public function add($packet) {
        $this->db->analytics->insert($packet);
    }
    
    public function findBy($query = array()) {
        return $this->db->analytics_aggregated->find($query);
    }
}
