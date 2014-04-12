<?php

namespace OpenData\Services;

class AnalyticsService extends BaseService {

    public function add($packet) {
        $this->db->analytics->insert($packet);
    }
    
    public function hourlyForFiles($signatures = array(), $from = null, $to = null) {
        if ($to == null) $to = time();
        if ($from == null) $from = $to - 172800;
        return $this->db->files_hourly->find(array(
            'time' => array(
                '$gte' => new \MongoDate($from),
                '$lt'  => new \MongoDate($to)
             ),
            'file' => array('$in' => $signatures)
        ))->sort(array('time' => 1));
    }

}
