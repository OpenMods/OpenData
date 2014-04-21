<?php

namespace OpenData\Services;

class ModsService extends BaseService {

    public function findById($modId) {
        return $this->db->mods->findOne(array('_id' => $modId));
    }

    public function findByRegex($search) {
        //{ $or: [ { qty: { $lt: 20 } }, { sale: true } ] } 
        $regex = new \MongoRegex('/'.$search.'/i');
        return $this->db->mods->find(array(
            '$or' => array(
                array('_id' => $regex),
                array('name' => $regex)
            )));
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

    public function findOrderedByPastHourLaunches($limit = 50, $filterLibraries = true) {
    	return $this->findAll();
    /*
        $currentHour = strtotime(date("Y-m-d H:00:00"));
        $searchDate = new \MongoDate($currentHour);
        return $this->db->mods->find(
            array(
                //'hours.time' => $searchDate,
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
                'hours' => array('$elemMatch' => array('time' => $searchDate))
            )
        )->sort(array('hours.launches' => -1))->limit($limit);*/
    }
    
    
    public static function sanitizeModId($modId) {
        return strtolower(preg_replace("@[^a-z0-9_ ]+@i", '', $modId));
    }

}
