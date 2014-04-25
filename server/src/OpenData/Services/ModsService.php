<?php

namespace OpenData\Services;

class ModsService extends BaseService {

    public function findById($modId) {
        return $this->db->mods->findOne(array('_id' => $modId));
    }

    public function findByRegex($search, $searchId = true) {
        
        $regex = new \MongoRegex('/'.$search.'/i');
        
        $q = array(array('name' => $regex));
        if ($searchId) {
            $q[] = array('_id' => $regex);
        }
        
        return $this->db->mods->find(array('$or' => $q));
    }
    
    public function updateMod($modId, $data) {
        $this->db->mods->update(
            array('_id' => $modId),
            array(
                '$set' => $data
            )
        );
    }
    
    public function findAll() {
        return $this->db->mods->find(array('hide' => array('$ne' => true)))->sort(array('name' => 1));
    }

    public function findByIds($modIds = array()) {
        return $this->db->mods->find(array('_id' => array('$in' => $modIds)));
    }

    public function upsert($modId, $data) {
        return $this->db->mods->findAndModify(
            array('_id' => $modId),
            array('$setOnInsert' => $data),
            null,
            array('new' => true, 'upsert' => true)
        );
    }
    
    public function getDistinctTags() {
        return $this->db->mods->distinct('tags');
    }
    
    public function findByLetter($letter) {
        $letter = substr($letter, 0, 1);
        return $this->db->mods->find(array(
            'hide' => array('$ne' => true),
            'name' => new \MongoRegex('/^'.$letter.'/i')
        ))->sort(array('name' => 1));
    }
    
    public function findModsWithoutField($field, $skip = 0, $limit = 20) {
        return $this->db->mods->find(array(
           'hide' => array('$ne' => true),
           '$or' => array(
               array( $field =>  array('$exists' => false)),
               array( $field => '')
           )
        ))->skip($skip)->limit($limit);
    }
    
    public function findByTag($tag) {
        $tag = trim($tag);
        return $this->db->mods->find(array(
            'hide' => array('$ne' => true),
            'tags' => new \MongoRegex('/^'.$tag.'/i')
        ))->sort(array('name' => 1));
    }

    public function findOrderedByPastDayLaunches($limit = 50, $filterLibraries = true) {

        $yesterday = strtotime(date("Y-m-d 00:00:00", time() - 86400));
        $searchDate = new \MongoDate($yesterday);
        return $this->db->mods->find(
            array(
                'days.time' => $searchDate,
                'tags' => array('$ne' => 'library'),
                'hide' => array('$ne' => true),
                'image' => array('$exists' => 1)
            ),
            array(
                'name' => 1,
                'authors' => 1,
                'description' => 1,
                'tags' => 1,
                'image' => 1,
                'days' => array('$elemMatch' => array('time' => $searchDate))
            )
        )->sort(array('days.launches' => -1))->limit($limit);
    }
    
    
    public static function sanitizeModId($modId) {
        return strtolower(preg_replace("@[^a-z0-9_ ]+@i", '', $modId));
    }

}
