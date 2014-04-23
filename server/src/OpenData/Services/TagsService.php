<?php

namespace OpenData\Services;

/**
 * Description of TagsService
 *
 * @author Mike Franklin <mike.franklin@naturalmotion.com>
 */
class TagsService extends BaseService {
    
    public function inc($tag, $date, $type = 'hourly') {
        
        return $this->db->tag_stats->findAndModify(array(
                'query' => array(
                    'tag' => $tag,
                    'time' => time(),
                    'type' => $type                    
                ),
                'update' => array(
                    '$inc' => array('launches' => new \MongoInt32(1))                    
                ),
                'upsert' => true
        ));
    }
    
}
