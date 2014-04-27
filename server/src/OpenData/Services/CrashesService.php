<?php

namespace OpenData\Services;

class CrashesService extends BaseService {

    public static function stripSignatures($arr) {
	$signatures = array();
        $classes = array();
	for ($i = 0; $i < count($arr['stack']); $i++) {
		$signatures = array_merge($arr['stack'][$i]['signatures'], $signatures);
                $classes[] = $arr['stack'][$i]['class'];
		unset($arr['stack'][$i]['signatures']);
	}
	if (isset($arr['cause'])) {
		$cause = self::stripSignatures($arr['cause']);
		$signatures = array_merge($signatures, $cause['signatures']);
		$classes = array_merge($classes, $cause['classes']);
		$arr['cause'] = $cause['exception'];
	}
	return array(
            'signatures' => $signatures,
            'classes'   => $classes,
            'exception' => $arr
	);
    }
    
    public function add($packet, $fileIds, $modIds) {

        $note = null;
        
        $involvedSignatures = array();
        $involvedModIds = array();
        
        $allSignatures = array();
        $allModIds = array();
        
        // get all the modids and states from the mod states
        foreach ($packet['states'] as $state) {
            $errored = false;
            foreach ($state['mods'] as $mod) {
                $sanitized = ModsService::sanitizeModId($mod['modId']);
                if ($mod['state'] == 'Errored') {
                    $errored = true;
                    $involvedModIds[] = $sanitized;
                }
                $allModIds[] = $sanitized;
            }
            if ($errored) {
                $involvedSignatures[] = $state['signature'];
            }
            $allSignatures[] = $state['signature'];
        }

        $crashData = self::stripSignatures($packet['exception']);
        $stackSignatures = $crashData['signatures'];
        $stackWithoutSignatures = $crashData['exception'];
        
        // if we've got stack signatures, lets get the modids for those
        // signatures
        if (count($stackSignatures) > 0) {
            $results = $this->db->files->find(
                  array('_id' => array('$in' => $stackSignatures)),
                  array('mods.modId' => 1)
            );
            foreach ($results as $result) {
                foreach ($result['mods'] as $mod) {
                    $involvedModIds[] = $mod['modId'];
                }
            }
            // merge the stack signatures in
            $involvedSignatures = array_merge($involvedSignatures, $stackSignatures);
        }
       
        // get unique lists of both
        $involvedSignatures = array_unique($involvedSignatures);
        $involvedModIds = array_unique($involvedModIds);
        
        // find the hash of the stacktrace
        $packet['stackhash'] = md5(serialize($stackWithoutSignatures));
        
        // look for the stacktrace
        $crash = $this->db->crashes->findOne(array(
            '_id' => $packet['stackhash']
        ));
        
        if ($crash == null) {
            $this->db->crashes->insert(array(
                '_id' => $packet['stackhash'],
                'latest' => time(),
                'exception' => $stackWithoutSignatures,
                'involvedSignatures' => array_values($involvedSignatures),
                'involvedMods' => array_values($involvedModIds),
                'allSignatures' => array_values($allSignatures),
                'allMods' => array_values($allModIds),
                'classes' => $crashData['classes'],
                'count' => 1
            ));
            
            $redis = new \Predis\Client();
            $redis->publish('crash', json_encode(array(
                'modIds' => $involvedModIds,
                'content' => $stackWithoutSignatures['exception'].': '.$stackWithoutSignatures['message'].' - http://openeye.openmods.info/crashes/'.$packet['stackhash']
            )));
 
        } else {
            
            $crash['involvedSignatures'] = array_intersect($crash['involvedSignatures'], $allSignatures);
            $crash['involvedMods'] = array_intersect($crash['involvedMods'], $allModIds);
            
            if (isset($crash['note']) && isset($crash['note']['message'])) {
                $note = $crash['note'];
            }
            
            $this->db->crashes->update(
                array('_id' => $packet['stackhash']),
                array(
                    '$set' => array(
                        'latest' => time(),
                        'allSignatures' => array_values($crash['allSignatures']),
                        'allMods' => array_values($crash['allMods'])
                    ),
                    '$inc' => array('count' => 1),
                    '$addToSet' => array(
                        'involvedSignatures' => array('$each' => $involvedSignatures),
                        'involvedMods' => array('$each' => $involvedModIds)
                    )
                )
            );
        }
        
        return array(
            'stackhash' => $packet['stackhash'],
            'note'      => $note
        );
    }


    public function findByPackage($package, $skip = 0, $limit = 40) {
        return $this->find(array('classes' => new \MongoRegex('/^'.preg_quote($package).'\./')))
                ->sort(array('timestamp' => -1))
                ->skip($skip)
                ->limit($limit);
    }

    public function findBySignatures($signatures = array()) {
        if (count($signatures) == 0) return array();
        return $this->db->crashes->find(array('involvedSignatures' =>
                array('$in' => $signatures)
            ))->sort(array('latest' => -1));
    }
    
    public function findByStackhash($stackhash) {
        return $this->db->crashes->findOne(array('_id' => $stackhash));
    }

    public function findLatest($query = array(), $skip = 0, $limit = 40) {
        return $this->find($query)
                ->sort(array('latest' => -1))
                ->skip($skip)
                ->limit($limit);
    }

    private function find($query = array()) {
        return $this->db->crashes->find($query);
    }

}
